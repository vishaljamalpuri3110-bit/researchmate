package com.researchmate.exception;

import java.time.Instant;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PaperNotFoundException.class)
    public ResponseEntity<ApiError> handlePaperNotFound(
            PaperNotFoundException ex, HttpServletRequest request) {
        log.warn("Paper not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiError> handleDuplicateEmail(
            DuplicateEmailException ex, HttpServletRequest request) {
        log.warn("Registration conflict: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", ex.getMessage(), request);
    }

    @ExceptionHandler({InvalidCredentialsException.class, BadCredentialsException.class})
    public ResponseEntity<ApiError> handleBadCredentials(
            Exception ex, HttpServletRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid email or password", request);
    }

    @ExceptionHandler({UnauthorizedAccessException.class, AccessDeniedException.class})
    public ResponseEntity<ApiError> handleAccessDenied(
            Exception ex, HttpServletRequest request) {
        log.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, "FORBIDDEN", "You are not authorized to access this resource", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("Validation error on {}: {}", request.getRequestURI(), details);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", details, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Bad request on {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxSize(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("File upload exceeded maximum size limit: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "PDF file exceeds maximum allowed size of 10 MB", request);
    }

    @ExceptionHandler(PdfProcessingException.class)
    public ResponseEntity<ApiError> handlePdfProcessing(
            PdfProcessingException ex, HttpServletRequest request) {
        log.error("PDF processing failed: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, "PDF_PROCESSING_FAILED", ex.getMessage(), request);
    }

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<ApiError> handleAiService(
            AiServiceException ex, HttpServletRequest request) {
        log.error("AI service error: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE", ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "An unexpected error occurred. Please try again later.", request);
    }

    private ResponseEntity<ApiError> buildErrorResponse(
            HttpStatus status, String errorCode, String message, HttpServletRequest request) {
        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                errorCode,
                message,
                request != null ? request.getRequestURI() : null
        );
        return ResponseEntity.status(status).body(error);
    }
}