package com.nousware.service;

public interface EmailService {
    void sendVerificationEmail(String to, String token);

    // NEW
    void sendPasswordResetEmail(String to, String token);
}
