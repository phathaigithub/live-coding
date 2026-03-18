package com.example.backend.dto;

import java.util.UUID;

import com.example.backend.entity.CodeSession.SessionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CodeSessionResponse {
    private UUID sessionId;
    private SessionStatus status;
}
