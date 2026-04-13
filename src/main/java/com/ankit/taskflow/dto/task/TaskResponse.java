package com.ankit.taskflow.dto.task;

import com.ankit.taskflow.model.enums.TaskPriority;
import com.ankit.taskflow.model.enums.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        UUID projectId,
        UUID assigneeId,
        String assigneeEmail,
        UUID creatorId,
        LocalDate dueDate,
        Integer version,
        Instant createdAt,
        Instant updatedAt) {
}
