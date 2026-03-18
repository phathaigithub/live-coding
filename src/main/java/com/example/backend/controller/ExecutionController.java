package com.example.backend.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.ExecutionResponse;
import com.example.backend.service.ExecutionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/executions")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionService executionService;

    @GetMapping("/{execution_id}")
    public ResponseEntity<ExecutionResponse> getExecutionStatus(@PathVariable("execution_id") UUID executionId) {
        return ResponseEntity.ok(executionService.getExecutionStatus(executionId));
    }
}
