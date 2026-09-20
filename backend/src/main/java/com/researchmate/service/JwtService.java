package com.researchmate.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.researchmate.config.JwtProperties;
import com.researchmate.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service 
public class JwtService {
    
    private  final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties){
        this.jwtProperties=jwtProperties;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
                jwtProperties.secret().getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateToken(User user){

        Date now=new Date();

        Date expiration=new Date(
            now.getTime()+jwtProperties.expiration()
        );

        return Jwts.builder()
                   .subject(user.getEmail())
                   .claim("role", user.getRole())
                   .issuedAt(now)
                   .expiration(expiration)
                   .signWith(getSigningKey())
                   .compact();
    }

    public String extractEmail(String token){
        return parseToken(token)
               .getPayload()
               .getSubject();
              
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } 
        catch (Exception ex) {
            return false;
        }
    }
    private Jws<Claims> parseToken(String token){
        return Jwts.parser()
                   .verifyWith(getSigningKey())
                   .build()
                   .parseSignedClaims(token);
    }
}
