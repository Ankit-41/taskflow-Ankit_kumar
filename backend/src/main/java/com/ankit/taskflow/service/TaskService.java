package com.ankit.taskflow.service;

import com.ankit.taskflow.config.TaskFlowProperties;
import com.ankit.taskflow.dto.common.PageResponse;
import com.ankit.taskflow.dto.task.CreateTaskRequest;
import com.ankit.taskflow.dto.task.TaskResponse;
import com.ankit.taskflow.dto.task.UpdateTaskRequest;
import com.ankit.taskflow.exception.ConflictException;
import com.ankit.taskflow.exception.NotFoundException;
import com.ankit.taskflow.model.Project;
import com.ankit.taskflow.model.Task;
import com.ankit.taskflow.model.User;
import com.ankit.taskflow.model.enums.TaskStatus;
import com.ankit.taskflow.repository.ProjectRepository;
import com.ankit.taskflow.repository.TaskRepository;
import com.ankit.taskflow.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final RedisCacheService redisCacheService;
    private final TaskFlowProperties properties;

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> listTasks(UUID projectId, TaskStatus status, UUID assigneeId, int page, int limit) {
        String cacheKey = cacheKey(projectId, status, assigneeId, page, limit);
        return redisCacheService.get(cacheKey, new TypeReference<PageResponse<TaskResponse>>() { })
                .orElseGet(() -> {
                    Page<TaskResponse> tasks = taskRepository.findByFilters(projectId, status, assigneeId, PageRequest.of(page, limit))
                            .map(this::toTaskResponse);
                    PageResponse<TaskResponse> response = new PageResponse<>(
                            tasks.getContent(),
                            tasks.getNumber(),
                            tasks.getSize(),
                            tasks.getTotalElements(),
                            tasks.getTotalPages());
                    redisCacheService.put(cacheKey, response, properties.getCache().getTaskListTtl());
                    return response;
                });
    }

    @Transactional
    public TaskResponse createTask(UUID projectId, UUID creatorId, CreateTaskRequest request) {
        Project project = findProject(projectId);
        User creator = findUser(creatorId);
        User assignee = request.assigneeId() == null ? null : findUser(request.assigneeId());

        Task task = Task.builder()
                .id(UUID.randomUUID())
                .title(request.title().trim())
                .description(request.description().trim())
                .status(request.status())
                .priority(request.priority())
                .project(project)
                .assignee(assignee)
                .creator(creator)
                .dueDate(request.dueDate())
                .build();

        Task saved = taskRepository.save(task);
        projectService.evictProjectCaches(projectId);
        return toTaskResponse(saved);
    }

    @Transactional
    public TaskResponse updateTask(UUID taskId, UpdateTaskRequest request) {
        Task task = findTask(taskId);
        if (!task.getVersion().equals(request.version())) {
            throw new ConflictException("stale version");
        }

        if (request.title() != null && !request.title().isBlank()) {
            task.setTitle(request.title().trim());
        }
        if (request.description() != null && !request.description().isBlank()) {
            task.setDescription(request.description().trim());
        }
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        if (Boolean.TRUE.equals(request.clearAssignee())) {
            task.setAssignee(null);
        } else if (request.assigneeId() != null) {
            task.setAssignee(findUser(request.assigneeId()));
        }
        if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }

        Task saved = taskRepository.save(task);
        projectService.evictProjectCaches(task.getProject().getId());
        return toTaskResponse(saved);
    }

    @Transactional
    public void deleteTask(UUID taskId) {
        Task task = findTask(taskId);
        UUID projectId = task.getProject().getId();
        taskRepository.delete(task);
        projectService.evictProjectCaches(projectId);
    }

    private Task findTask(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("task not found"));
    }

    private Project findProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("project not found"));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("user not found"));
    }

    private String cacheKey(UUID projectId, TaskStatus status, UUID assigneeId, int page, int limit) {
        return projectService.taskListCachePrefix(projectId)
                + (status == null ? "all" : status.name()) + ":"
                + (assigneeId == null ? "all" : assigneeId) + ":"
                + page + ":" + limit;
    }

    private TaskResponse toTaskResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getProject().getId(),
                task.getAssignee() == null ? null : task.getAssignee().getId(),
                task.getAssignee() == null ? null : task.getAssignee().getEmail(),
                task.getCreator().getId(),
                task.getDueDate(),
                task.getVersion(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
