package com.example.backend.worker;

import com.example.backend.config.RedisConfig;
import com.example.backend.dto.ExecutionTask;
import com.example.backend.entity.Execution;
import com.example.backend.repository.ExecutionRepository;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExecutionWorker {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ExecutionRepository executionRepository;
    private final DockerClient dockerClient;

    @Scheduled(fixedDelay = 1000)
    public void processQueue() {
        Object taskObj = redisTemplate.opsForList().leftPop(RedisConfig.EXECUTION_QUEUE);
        if (taskObj instanceof ExecutionTask task) {
            log.info("Processing execution task: {}", task.getExecutionId());
            executeInDocker(task);
        }
    }

    private void executeInDocker(ExecutionTask task) {
        String containerId = null;
        try {
            // 1. Update status to RUNNING
            updateStatus(task.getExecutionId(), Execution.ExecutionStatus.RUNNING, null, null, null);

            // 2. Map language to Docker Image and Command
            String image;
            String command;
            
            if ("javascript".equalsIgnoreCase(task.getLanguage())) {
                image = "node:18-slim";
                command = "node -e \"" + task.getSourceCode().replace("\"", "\\\"") + "\"";
            } else if ("python".equalsIgnoreCase(task.getLanguage())) {
                image = "python:3.10-slim";
                command = "python3 -c \"" + task.getSourceCode().replace("\"", "\\\"") + "\"";
            } else {
                // Return descriptive error for unsupported languages
                String errorMsg = "Unsupported language: " + task.getLanguage() + ". Currently only 'python' and 'javascript' are supported.";
                log.warn(errorMsg);
                updateStatus(task.getExecutionId(), Execution.ExecutionStatus.FAILED, null, errorMsg, null);
                return;
            }

            // 3. Create Container with limits (Security & Safety)
            CreateContainerResponse container = dockerClient.createContainerCmd(image)
                    .withCmd("sh", "-c", command)
                    .withHostConfig(new HostConfig()
                            .withMemory(128 * 1024 * 1024L) // 128MB
                            .withCpuQuota(50000L) // 0.5 CPU
                            .withNetworkMode("none")) // No internet
                    .exec();

            containerId = container.getId();
            long startTime = System.currentTimeMillis();

            // 4. Start and Wait with Timeout
            dockerClient.startContainerCmd(containerId).exec();
            
            WaitContainerResultCallback waitCallback = new WaitContainerResultCallback();
            boolean finished = dockerClient.waitContainerCmd(containerId)
                    .exec(waitCallback)
                    .awaitCompletion(10, TimeUnit.SECONDS);

            long executionTime = System.currentTimeMillis() - startTime;

            if (!finished) {
                log.warn("Execution timeout for {}", task.getExecutionId());
                updateStatus(task.getExecutionId(), Execution.ExecutionStatus.TIMEOUT, null, "Execution timeout after 10s", executionTime);
                return;
            }

            // Check exit code to determine SUCCESS or FAILED
            Integer exitCode = waitCallback.awaitStatusCode();
            Execution.ExecutionStatus statusFromExitCode = (exitCode != null && exitCode == 0) 
                ? Execution.ExecutionStatus.COMPLETED 
                : Execution.ExecutionStatus.FAILED;

            // 5. Collect Logs (stdout/stderr)
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            String msg = new String(frame.getPayload(), StandardCharsets.UTF_8);
                            if (frame.getStreamType().name().contains("STDOUT")) {
                                stdout.append(msg);
                            } else {
                                stderr.append(msg);
                            }
                        }
                    }).awaitCompletion(5, TimeUnit.SECONDS);

            // 6. Final Update
            Execution.ExecutionStatus finalStatus = (stderr.length() > 0 || statusFromExitCode == Execution.ExecutionStatus.FAILED) 
                ? Execution.ExecutionStatus.FAILED 
                : Execution.ExecutionStatus.COMPLETED;
                
            updateStatus(task.getExecutionId(), finalStatus, 
                         stdout.toString(), stderr.toString(), executionTime);

        } catch (Exception e) {
            log.error("Failed to execute code", e);
            updateStatus(task.getExecutionId(), Execution.ExecutionStatus.FAILED, null, e.getMessage(), null);
        } finally {
            if (containerId != null) {
                try {
                    dockerClient.removeContainerCmd(containerId).withForce(true).exec();
                } catch (Exception ignored) {}
            }
        }
    }

    private void updateStatus(UUID id, Execution.ExecutionStatus status, String stdout, String stderr, Long time) {
        executionRepository.findById(id).ifPresent(e -> {
            e.setStatus(status);
            e.setStdout(stdout);
            e.setStderr(stderr);
            e.setExecutionTimeMs(time);
            executionRepository.save(e);
        });
    }
}
