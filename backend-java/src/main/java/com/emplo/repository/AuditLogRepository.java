package com.emplo.repository;

import com.emplo.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:entityType IS NULL OR a.entityType = :entityType) AND " +
            "(:action IS NULL OR a.action = :action) AND " +
            "(:actorId IS NULL OR a.actorId = :actorId) " +
            "ORDER BY a.createdAt DESC")
    Page<AuditLog> findAllWithFilters(
            @Param("entityType") String entityType,
            @Param("action") String action,
            @Param("actorId") UUID actorId,
            Pageable pageable);

    @Query("SELECT DISTINCT a.action FROM AuditLog a")
    List<String> findDistinctActions();

    @Query("SELECT DISTINCT a.entityType FROM AuditLog a")
    List<String> findDistinctEntityTypes();
}
