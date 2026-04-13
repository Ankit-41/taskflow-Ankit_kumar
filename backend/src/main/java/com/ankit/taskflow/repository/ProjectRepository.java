package com.ankit.taskflow.repository;

import com.ankit.taskflow.model.Project;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @Query("""
        select p from Project p
        where p.owner.id = :userId
           or exists (
                select 1 from ProjectMember pm
                where pm.project = p and pm.user.id = :userId
           )
           or exists (
                select 1 from Task t
                where t.project = p and t.assignee.id = :userId
           )
        """)
    Page<Project> findAccessibleProjects(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
        select p from Project p
        where p.id = :projectId
          and (
                p.owner.id = :userId
                or exists (
                    select 1 from ProjectMember pm
                    where pm.project = p and pm.user.id = :userId
                )
                or exists (
                    select 1 from Task t
                    where t.project = p and t.assignee.id = :userId
                )
          )
        """)
    Optional<Project> findAccessibleProject(@Param("projectId") UUID projectId, @Param("userId") UUID userId);
}

