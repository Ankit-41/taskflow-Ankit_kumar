package com.ankit.taskflow.service;

import com.ankit.taskflow.model.ProjectMember;
import com.ankit.taskflow.model.enums.ProjectRole;
import com.ankit.taskflow.repository.ProjectMemberRepository;
import com.ankit.taskflow.repository.ProjectRepository;
import com.ankit.taskflow.repository.TaskRepository;
import com.ankit.taskflow.security.UserPrincipal;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("permissionService")
@RequiredArgsConstructor
public class PermissionService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;

    public boolean canViewProject(Authentication authentication, UUID projectId) {
        UUID userId = extractUserId(authentication);
        return userId != null && projectRepository.findAccessibleProject(projectId, userId).isPresent();
    }

    public boolean canEditProject(Authentication authentication, UUID projectId) {
        UUID userId = extractUserId(authentication);
        if (userId == null) {
            return false;
        }
        if (isProjectOwner(projectId, userId)) {
            return true;
        }
        return findMemberRole(projectId, userId)
                .map(role -> role == ProjectRole.ADMIN || role == ProjectRole.EDITOR)
                .orElse(false);
    }

    public boolean canManageProject(Authentication authentication, UUID projectId) {
        UUID userId = extractUserId(authentication);
        if (userId == null) {
            return false;
        }
        if (isProjectOwner(projectId, userId)) {
            return true;
        }
        return findMemberRole(projectId, userId).map(role -> role == ProjectRole.ADMIN).orElse(false);
    }

    public boolean canUpdateTask(Authentication authentication, UUID taskId) {
        UUID userId = extractUserId(authentication);
        if (userId == null) {
            return false;
        }
        return taskRepository.findById(taskId)
                .map(task -> isProjectOwner(task.getProject().getId(), userId)
                        || task.getCreator().getId().equals(userId)
                        || findMemberRole(task.getProject().getId(), userId)
                                .map(role -> role == ProjectRole.ADMIN || role == ProjectRole.EDITOR)
                                .orElse(false))
                .orElse(false);
    }

    public boolean canDeleteTask(Authentication authentication, UUID taskId) {
        UUID userId = extractUserId(authentication);
        if (userId == null) {
            return false;
        }
        return taskRepository.findById(taskId)
                .map(task -> isProjectOwner(task.getProject().getId(), userId)
                        || task.getCreator().getId().equals(userId)
                        || findMemberRole(task.getProject().getId(), userId)
                                .map(role -> role == ProjectRole.ADMIN)
                                .orElse(false))
                .orElse(false);
    }

    private Optional<ProjectRole> findMemberRole(UUID projectId, UUID userId) {
        return projectMemberRepository.findByProject_IdAndUser_Id(projectId, userId)
                .map(ProjectMember::getRole);
    }

    private boolean isProjectOwner(UUID projectId, UUID userId) {
        return projectRepository.findById(projectId)
                .map(project -> project.getOwner().getId().equals(userId))
                .orElse(false);
    }

    private UUID extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal.getUserId();
    }
}

