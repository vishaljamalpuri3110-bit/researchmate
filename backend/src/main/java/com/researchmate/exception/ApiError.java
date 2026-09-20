package com.researchmate.exception;

import java.time.Instant;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
    public ApiError(Instant timestamp, int status, String error, String message) {
        this(timestamp, status, error, message, null);
    }
}