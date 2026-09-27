package com.researchmate.entity;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(
    name = "password_reset_tokens",
    indexes = {
        @Index(name = "idx_reset_token_token", columnList = "token"),
        @Index(name = "idx_reset_token_user", columnList = "user_id")
    }
)
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        unique = true
    )
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    public PasswordResetToken() {
    }

    public PasswordResetToken(
            String token,
            User user,
            Instant expiresAt) {

        this.token = token;
        this.user = user;
        this.expiresAt = expiresAt;
        this.used = false;
    }

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }
}
