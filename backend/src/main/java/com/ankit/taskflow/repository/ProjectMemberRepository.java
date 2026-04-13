package com.ankit.taskflow.repository;

import com.ankit.taskflow.model.ProjectMember;
import com.ankit.taskflow.model.ProjectMemberId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {

    Optional<ProjectMember> findByProject_IdAndUser_Id(UUID projectId, UUID userId);

    boolean existsByProject_IdAndUser_Id(UUID projectId, UUID userId);
}

