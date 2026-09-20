package com.researchmate.exception;

public class PaperNotFoundException extends RuntimeException {

    public PaperNotFoundException(Long id) {
        super("Paper not found with id: " + id);
    }

    public PaperNotFoundException(String message) {
        super(message);
    }
}