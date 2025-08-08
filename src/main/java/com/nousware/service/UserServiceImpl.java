package com.nousware.service.impl;

import com.nousware.dto.RegistrationRequest;
import com.nousware.entities.User;
import com.nousware.entities.VerificationToken;
import com.nousware.repository.UserRepository;
import com.nousware.repository.VerificationTokenRepository;
import com.nousware.service.EmailService;
import com.nousware.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserServiceImpl(UserRepository userRepository,
                           VerificationTokenRepository tokenRepository,
                           PasswordEncoder passwordEncoder,
                           EmailService emailService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    @Override
    public void registerUser(RegistrationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            // 409 Conflict is clearer for clients
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setGender(request.getGender());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnable(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        // create verification token
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));

        tokenRepository.save(verificationToken);

        // build link (point to your frontend if you have one)
        emailService.sendVerificationEmail(user.getEmail(), token);
    }

    @Transactional
    @Override
    public boolean verifyAccount(String token) {
        return tokenRepository.findByToken(token)
                .filter(t -> t.getExpiryDate().isAfter(LocalDateTime.now()))
                .map(t -> {
                    User user = t.getUser();
                    if (!user.isEnable()) {
                        user.setEnable(true);
                        user.setUpdatedAt(LocalDateTime.now());
                        userRepository.save(user);
                    }
                    tokenRepository.delete(t); // burn token
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    @Override
    public void resendVerification(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.isEnable()) return; // already verified

            // remove old tokens
            List<VerificationToken> old = tokenRepository.findByUser_UserId(user.getUserId());
            old.forEach(tokenRepository::delete);

            // create new token
            String token = UUID.randomUUID().toString();
            VerificationToken vt = new VerificationToken();
            vt.setToken(token);
            vt.setUser(user);
            vt.setExpiryDate(LocalDateTime.now().plusMinutes(15));
            tokenRepository.save(vt);

            emailService.sendVerificationEmail(user.getEmail(), token);
        });
    }
}
