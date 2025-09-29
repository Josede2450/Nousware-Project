package com.nousware.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        return Map.of(
                "principal", auth == null ? null : auth.getName(),
                "class", auth == null ? null : auth.getClass().getName(),
                "authorities", auth == null ? null :
                        auth.getAuthorities().stream().map(a -> a.getAuthority()).toList()
        );
    }
}
