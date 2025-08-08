package com.nousware.service;

import com.nousware.dto.RegistrationRequest;
import org.springframework.transaction.annotation.Transactional;

public interface UserService {
    void registerUser(RegistrationRequest request);
    boolean verifyAccount(String token);

    @Transactional
    void resendVerification(String email);
}
