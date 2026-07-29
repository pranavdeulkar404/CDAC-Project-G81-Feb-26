package com.sprintflow.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileRequest(
        @Pattern(regexp = "^$|^[+0-9() -]{7,25}$", message = "Enter a valid phone number") String phone,
        @Size(max = 100) String designation,
        @Size(max = 1000) String bio
) {
}
