package com.ankit.taskflow.dto.project;

import com.ankit.taskflow.model.enums.ProjectRole;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ProjectMemberRequest(
        @NotNull(message = "userId is required")
        UUID userId,
        @NotNull(message = "role is required")
        ProjectRole role) {
}

