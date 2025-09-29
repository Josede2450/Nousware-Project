package com.nousware.service;

import com.nousware.entities.ContactForm;

public interface EmailService {
    void sendVerificationEmail(String to, String token);
    void sendPasswordResetEmail(String to, String token);
    void sendContactNotification(ContactForm form);
    void sendContactConfirmation(ContactForm form);
}
