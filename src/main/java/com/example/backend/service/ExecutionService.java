package com.example.backend.service;

import java.util.UUID;

import com.example.backend.dto.ExecutionResponse;

public interface ExecutionService {
    ExecutionResponse getExecutionStatus(UUID executionId);
}
