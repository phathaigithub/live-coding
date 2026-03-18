package com.example.backend.dto;

import java.util.UUID;
import com.example.backend.entity.Execution.ExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RunCodeResponse {
    private UUID executionId;
    private ExecutionStatus status;
}
