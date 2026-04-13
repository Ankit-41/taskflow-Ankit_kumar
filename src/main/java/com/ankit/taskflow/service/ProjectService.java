package com.ankit.taskflow.service;

import com.ankit.taskflow.config.TaskFlowProperties;
import com.ankit.taskflow.dto.common.PageResponse;
import com.ankit.taskflow.dto.project.CreateProjectRequest;
import com.ankit.taskflow.dto.project.ProjectMemberRequest;
import com.ankit.taskflow.dto.project.ProjectResponse;
import com.ankit.taskflow.dto.project.ProjectStatsResponse;
import com.ankit.taskflow.dto.project.UpdateProjectRequest;
import com.ankit.taskflow.exception.NotFoundException;
import com.ankit.taskflow.model.Project;
import com.ankit.taskflow.model.ProjectMember;
import com.ankit.taskflow.model.ProjectMemberId;
import com.ankit.taskflow.model.User;
import com.ankit.taskflow.repository.ProjectMemberRepository;
import com.ankit.taskflow.repository.ProjectRepository;
import com.ankit.taskflow.repository.TaskRepository;
import com.ankit.taskflow.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final RedisCacheService redisCacheService;
    private final TaskFlowProperties properties;

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> listProjects(UUID userId, int page, int limit) {
        Page<ProjectResponse> projects = projectRepository.findAccessibleProjects(userId, PageRequest.of(page, limit))
                .map(this::toProjectResponse);
        return new PageResponse<>(
                projects.getContent(),
                projects.getNumber(),
                projects.getSize(),
                projects.getTotalElements(),
                projects.getTotalPages());
    }

    @Transactional
    public ProjectResponse createProject(UUID userId, CreateProjectRequest request) {
        User owner = findUser(userId);
        Project project = Project.builder()
                .id(UUID.randomUUID())
                .name(request.name().trim())
                .description(request.description().trim())
                .owner(owner)
                .build();
        return toProjectResponse(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID projectId) {
        String cacheKey = projectCacheKey(projectId);
        return redisCacheService.get(cacheKey, ProjectResponse.class)
                .orElseGet(() -> {
                    ProjectResponse response = toProjectResponse(findProject(projectId));
                    redisCacheService.put(cacheKey, response, properties.getCache().getProjectTtl());
                    return response;
                });
    }

    @Transactional(readOnly = true)
    public ProjectStatsResponse getStats(UUID projectId) {
        findProject(projectId);
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        taskRepository.countByStatus(projectId)
                .forEach(view -> statusCounts.put(view.getStatus().name(), view.getTotal()));

        Map<String, Long> assigneeCounts = new LinkedHashMap<>();
        taskRepository.countByAssignee(projectId)
                .forEach(view -> assigneeCounts.put(view.getAssigneeEmail(), view.getTotal()));

        return new ProjectStatsResponse(statusCounts, assigneeCounts);
    }

    @Transactional
    public ProjectResponse updateProject(UUID projectId, UpdateProjectRequest request) {
        Project project = findProject(projectId);
        if (request.name() != null && !request.name().isBlank()) {
            project.setName(request.name().trim());
        }
        if (request.description() != null && !request.description().isBlank()) {
            project.setDescription(request.description().trim());
        }

        Project saved = projectRepository.save(project);
        evictProjectCaches(projectId);
        return toProjectResponse(saved);
    }

    @Transactional
    public void deleteProject(UUID projectId) {
        Project project = findProject(projectId);
        projectRepository.delete(project);
        evictProjectCaches(projectId);
    }

    @Transactional
    public ProjectResponse addMember(UUID projectId, ProjectMemberRequest request) {
        Project project = findProject(projectId);
        User user = findUser(request.userId());

        if (project.getOwner().getId().equals(user.getId())) {
            return toProjectResponse(project);
        }

        ProjectMember member = projectMemberRepository.findByProject_IdAndUser_Id(projectId, user.getId())
                .orElse(ProjectMember.builder()
                        .id(new ProjectMemberId(projectId, user.getId()))
                        .project(project)
                        .user(user)
                        .build());
        member.setRole(request.role());
        projectMemberRepository.save(member);
        evictProjectCaches(projectId);
        return toProjectResponse(project);
    }

    public void evictProjectCaches(UUID projectId) {
        redisCacheService.delete(projectCacheKey(projectId));
        redisCacheService.deleteByPattern(taskListCachePrefix(projectId) + "*");
    }

    public String taskListCachePrefix(UUID projectId) {
        return "project_tasks:" + projectId + ":";
    }

    private String projectCacheKey(UUID projectId) {
        return "project:" + projectId;
    }

    private Project findProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("project not found"));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("user not found"));
    }

    private ProjectResponse toProjectResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getOwner().getId(),
                project.getOwner().getEmail(),
                project.getCreatedAt());
    }
}
