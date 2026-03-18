package com.example.backend.service.impl;

import com.example.backend.dto.ExecutionResponse;
import com.example.backend.entity.Execution;
import com.example.backend.repository.ExecutionRepository;
import com.example.backend.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExecutionServiceImpl implements ExecutionService {

    private final ExecutionRepository executionRepository;

    @Override
    public ExecutionResponse getExecutionStatus(UUID executionId) {
        Execution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("Execution not found"));

        return ExecutionResponse.builder()
                .executionId(execution.getExecutionId())
                .status(execution.getStatus())
                .stdout(execution.getStdout())
                .stderr(execution.getStderr())
                .executionTimeMs(execution.getExecutionTimeMs())
                .build();
    }
}
