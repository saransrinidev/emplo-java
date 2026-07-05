package com.emplo.repository;

import com.emplo.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findAllByAssignedToOrderByCreatedAtDesc(UUID empId);

    List<Task> findAllByAssignedByOrderByCreatedAtDesc(UUID empId);
}
