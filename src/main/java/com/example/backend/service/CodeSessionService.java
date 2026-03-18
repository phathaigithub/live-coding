package com.example.backend.service;

import java.util.UUID;

import com.example.backend.dto.CodeSessionResponse;
import com.example.backend.dto.CodeSessionUpdateRequest;
import com.example.backend.dto.RunCodeResponse;

public interface CodeSessionService {
    CodeSessionResponse createSession();
    CodeSessionResponse updateSession(UUID sessionId, CodeSessionUpdateRequest request);
    RunCodeResponse runCode(UUID sessionId);
}
