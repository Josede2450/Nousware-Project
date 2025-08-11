package com.nousware.service;

import com.nousware.entities.ContactForm;

public interface EmailService {
    void sendVerificationEmail(String to, String token);

    // NEW
    void sendPasswordResetEmail(String to, String token);

    void sendContactNotification(ContactForm form);

}
