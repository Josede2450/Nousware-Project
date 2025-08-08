package com.nousware.service;

public interface EmailService {
    void sendVerificationEmail(String to, String token);
}
