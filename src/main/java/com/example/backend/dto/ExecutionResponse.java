package com.example.backend.dto;

import java.util.UUID;

import com.example.backend.entity.Execution.ExecutionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExecutionResponse {
    private UUID executionId;
    private ExecutionStatus status;
    private String stdout;
    private String stderr;
    private Long executionTimeMs;
}
