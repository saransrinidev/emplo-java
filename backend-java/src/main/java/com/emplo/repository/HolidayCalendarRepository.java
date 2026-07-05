package com.emplo.repository;

import com.emplo.entity.HolidayCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HolidayCalendarRepository extends JpaRepository<HolidayCalendar, UUID> {

    Optional<HolidayCalendar> findByName(String name);

    List<HolidayCalendar> findAllByYear(Integer year);

    List<HolidayCalendar> findAllByOrderByYearDesc();
}
