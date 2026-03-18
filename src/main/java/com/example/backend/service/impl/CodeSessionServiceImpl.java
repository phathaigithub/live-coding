package com.example.backend.service.impl;

import com.example.backend.config.RedisConfig;
import com.example.backend.dto.CodeSessionResponse;
import com.example.backend.dto.CodeSessionUpdateRequest;
import com.example.backend.dto.RunCodeResponse;
import com.example.backend.dto.ExecutionTask;
import com.example.backend.entity.CodeSession;
import com.example.backend.entity.Execution;
import com.example.backend.repository.CodeSessionRepository;
import com.example.backend.repository.ExecutionRepository;
import com.example.backend.service.CodeSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CodeSessionServiceImpl implements CodeSessionService {

    private final CodeSessionRepository sessionRepository;
    private final ExecutionRepository executionRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public CodeSessionResponse createSession() {
        CodeSession session = CodeSession.builder()
                .status(CodeSession.SessionStatus.ACTIVE)
                .build();
        CodeSession savedSession = sessionRepository.save(session);
        return mapToResponse(savedSession);
    }

    @Override
    @Transactional
    public CodeSessionResponse updateSession(UUID sessionId, CodeSessionUpdateRequest request) {
        CodeSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        
        session.setLanguage(request.getLanguage());
        session.setSourceCode(request.getSourceCode());
        CodeSession updatedSession = sessionRepository.save(session);
        return mapToResponse(updatedSession);
    }

    @Override
    @Transactional
    public RunCodeResponse runCode(UUID sessionId) {
        CodeSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        Execution execution = Execution.builder()
                .session(session)
                .status(Execution.ExecutionStatus.QUEUED)
                .build();
        Execution savedExecution = executionRepository.save(execution);

        // Push to Redis Queue
        ExecutionTask task = ExecutionTask.builder()
                .executionId(savedExecution.getExecutionId())
                .sessionId(session.getSessionId())
                .language(session.getLanguage())
                .sourceCode(session.getSourceCode())
                .build();
        
        redisTemplate.opsForList().rightPush(RedisConfig.EXECUTION_QUEUE, task);
        
        return RunCodeResponse.builder()
                .executionId(savedExecution.getExecutionId())
                .status(savedExecution.getStatus())
                .build();
    }

    private CodeSessionResponse mapToResponse(CodeSession session) {
        return CodeSessionResponse.builder()
                .sessionId(session.getSessionId())
                .status(session.getStatus())
                .build();
    }
}
