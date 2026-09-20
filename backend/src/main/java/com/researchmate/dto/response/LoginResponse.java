package com.researchmate.dto.response;

public record LoginResponse(
    String token,
    String tokenType,
    long userId,
    String email,
    String role

) {
    
}
