package com.example.backend.dto;

import lombok.Data;

@Data
public class CodeSessionUpdateRequest {
    private String language;
    private String sourceCode;
}
