package com.ankit.taskflow.dto.task;

import com.ankit.taskflow.model.enums.TaskPriority;
import com.ankit.taskflow.model.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateTaskRequest(
        @NotBlank(message = "title is required")
        @Size(max = 180, message = "title must be at most 180 characters")
        String title,
        @NotBlank(message = "description is required")
        String description,
        @NotNull(message = "status is required")
        TaskStatus status,
        @NotNull(message = "priority is required")
        TaskPriority priority,
        UUID assigneeId,
        LocalDate dueDate) {
}

