package com.ankit.taskflow.dto.project;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String description,
        UUID ownerId,
        String ownerEmail,
        Instant createdAt) {
}

