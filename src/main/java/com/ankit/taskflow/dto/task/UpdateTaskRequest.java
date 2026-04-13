package com.ankit.taskflow.dto.task;

import com.ankit.taskflow.model.enums.TaskPriority;
import com.ankit.taskflow.model.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateTaskRequest(
        @NotNull(message = "version is required")
        Integer version,
        @Size(max = 180, message = "title must be at most 180 characters")
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        UUID assigneeId,
        Boolean clearAssignee,
        LocalDate dueDate) {
}

