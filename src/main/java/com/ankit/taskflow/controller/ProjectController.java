package com.ankit.taskflow.controller;

import com.ankit.taskflow.dto.common.PageResponse;
import com.ankit.taskflow.dto.project.CreateProjectRequest;
import com.ankit.taskflow.dto.project.ProjectMemberRequest;
import com.ankit.taskflow.dto.project.ProjectResponse;
import com.ankit.taskflow.dto.project.ProjectStatsResponse;
import com.ankit.taskflow.dto.project.UpdateProjectRequest;
import com.ankit.taskflow.dto.task.CreateTaskRequest;
import com.ankit.taskflow.dto.task.TaskResponse;
import com.ankit.taskflow.model.enums.TaskStatus;
import com.ankit.taskflow.security.UserPrincipal;
import com.ankit.taskflow.service.ProjectService;
import com.ankit.taskflow.service.TaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<PageResponse<ProjectResponse>> listProjects(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be at least 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "limit must be at least 1") @Max(value = 100, message = "limit must be at most 100") int limit) {
        return ResponseEntity.ok(projectService.listProjects(principal.getUserId(), page, limit));
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.createProject(principal.getUserId(), request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.canViewProject(authentication, #id)")
    public ResponseEntity<ProjectResponse> getProject(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.getProject(id));
    }

    @GetMapping("/{id}/stats")
    @PreAuthorize("@permissionService.canViewProject(authentication, #id)")
    public ResponseEntity<ProjectStatsResponse> getStats(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.getStats(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@permissionService.canManageProject(authentication, #id)")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequest request) {
        return ResponseEntity.ok(projectService.updateProject(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.canManageProject(authentication, #id)")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("@permissionService.canManageProject(authentication, #id)")
    public ResponseEntity<ProjectResponse> addMember(
            @PathVariable UUID id,
            @Valid @RequestBody ProjectMemberRequest request) {
        return ResponseEntity.ok(projectService.addMember(id, request));
    }

    @GetMapping("/{id}/tasks")
    @PreAuthorize("@permissionService.canViewProject(authentication, #id)")
    public ResponseEntity<PageResponse<TaskResponse>> listTasks(
            @PathVariable UUID id,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) UUID assignee,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be at least 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "limit must be at least 1") @Max(value = 100, message = "limit must be at most 100") int limit) {
        return ResponseEntity.ok(taskService.listTasks(id, status, assignee, page, limit));
    }

    @PostMapping("/{id}/tasks")
    @PreAuthorize("@permissionService.canEditProject(authentication, #id)")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.createTask(id, principal.getUserId(), request));
    }
}

