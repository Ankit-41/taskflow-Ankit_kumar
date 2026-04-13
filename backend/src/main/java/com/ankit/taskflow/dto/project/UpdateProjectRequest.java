package com.ankit.taskflow.dto.project;

import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @Size(max = 160, message = "name must be at most 160 characters")
        String name,
        String description) {
}

