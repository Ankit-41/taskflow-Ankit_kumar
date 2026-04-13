package com.ankit.taskflow.repository;

import com.ankit.taskflow.model.Task;
import com.ankit.taskflow.model.enums.TaskStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    @Query("""
        select t from Task t
        where t.project.id = :projectId
          and (:status is null or t.status = :status)
          and (:assigneeId is null or t.assignee.id = :assigneeId)
        order by t.createdAt desc
        """)
    Page<Task> findByFilters(
            @Param("projectId") UUID projectId,
            @Param("status") TaskStatus status,
            @Param("assigneeId") UUID assigneeId,
            Pageable pageable);

    @Query("""
        select t.status as status, count(t) as total
        from Task t
        where t.project.id = :projectId
        group by t.status
        """)
    List<StatusCountView> countByStatus(@Param("projectId") UUID projectId);

    @Query("""
        select assignee.id as assigneeId,
               coalesce(assignee.email, 'unassigned') as assigneeEmail,
               count(t) as total
        from Task t
        left join t.assignee assignee
        where t.project.id = :projectId
        group by assignee.id, assignee.email
        """)
    List<AssigneeCountView> countByAssignee(@Param("projectId") UUID projectId);

    interface StatusCountView {
        TaskStatus getStatus();

        Long getTotal();
    }

    interface AssigneeCountView {
        UUID getAssigneeId();

        String getAssigneeEmail();

        Long getTotal();
    }
}
