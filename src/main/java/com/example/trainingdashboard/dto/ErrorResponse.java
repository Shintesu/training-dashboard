package com.example.trainingdashboard.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ErrorResponse(String message, Map<String, String> fieldErrors) {

    public ErrorResponse {
        fieldErrors = fieldErrors == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }
}
