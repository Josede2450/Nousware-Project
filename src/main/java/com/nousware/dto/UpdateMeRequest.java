// src/main/java/com/nousware/dto/UpdateMeRequest.java
package com.nousware.dto;

import jakarta.validation.constraints.Size;

public record UpdateMeRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 30)  String phone
) {}
