package com.emplo.repository;

import com.emplo.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findAllByEmployeeIdOrderByCreatedAtDesc(UUID empId);

    List<Ticket> findAllByEmployeeIdInOrderByCreatedAtDesc(List<UUID> empIds);

    @Query("SELECT COUNT(t) FROM Ticket t")
    long countAll();
}
