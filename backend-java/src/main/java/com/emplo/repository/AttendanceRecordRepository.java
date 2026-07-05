package com.emplo.repository;

import com.emplo.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {

    Optional<AttendanceRecord> findByEmployeeIdAndWorkDate(UUID employeeId, LocalDate date);

    List<AttendanceRecord> findAllByEmployeeIdOrderByWorkDateDesc(UUID employeeId);

    List<AttendanceRecord> findAllByWorkDate(LocalDate date);

    List<AttendanceRecord> findAllByEmployeeIdInAndWorkDate(List<UUID> empIds, LocalDate date);

    List<AttendanceRecord> findAllByWorkDateOrderByCheckIn(LocalDate date);

    @Query("SELECT r FROM AttendanceRecord r WHERE r.employeeId = :empId " +
            "AND EXTRACT(MONTH FROM r.workDate) = :month AND EXTRACT(YEAR FROM r.workDate) = :year " +
            "ORDER BY r.workDate DESC")
    List<AttendanceRecord> findByEmployeeIdAndMonthAndYear(@Param("empId") UUID empId,
                                                           @Param("month") int month,
                                                           @Param("year") int year);
}
