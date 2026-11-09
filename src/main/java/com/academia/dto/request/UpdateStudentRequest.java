package com.academia.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateStudentRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 30) String contactNumber,
        @Size(max = 1000) String notes
) {
}
