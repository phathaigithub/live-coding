package com.example.backend.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.CodeSessionResponse;
import com.example.backend.dto.CodeSessionUpdateRequest;
import com.example.backend.dto.RunCodeResponse;
import com.example.backend.service.CodeSessionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/code-sessions")
@RequiredArgsConstructor
public class CodeSessionController {


    private final CodeSessionService sessionService;

    @PostMapping
    public ResponseEntity<CodeSessionResponse> createSession() {
        return ResponseEntity.ok(sessionService.createSession());
    }

    @PatchMapping("/{session_id}")
    public ResponseEntity<CodeSessionResponse> updateSession(
            @PathVariable("session_id") UUID sessionId,
            @RequestBody CodeSessionUpdateRequest request) {
        return ResponseEntity.ok(sessionService.updateSession(sessionId, request));
    }

    @PostMapping("/{session_id}/run")
    public ResponseEntity<RunCodeResponse> runCode(@PathVariable("session_id") UUID sessionId) {
        return ResponseEntity.ok(sessionService.runCode(sessionId));
    }
}
