package com.emplo.service;

import com.emplo.entity.Holiday;
import com.emplo.entity.HolidayCalendar;
import com.emplo.exception.BadRequestException;
import com.emplo.exception.NotFoundException;
import com.emplo.repository.HolidayCalendarRepository;
import com.emplo.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HolidayService {

    private final HolidayCalendarRepository calendarRepository;
    private final HolidayRepository holidayRepository;

    public List<HolidayCalendar> listCalendars(Integer year) {
        if (year != null) {
            return calendarRepository.findAllByYear(year);
        }
        return calendarRepository.findAllByOrderByYearDesc();
    }

    @Transactional
    public HolidayCalendar createCalendar(String name, String region, Integer year) {
        if (calendarRepository.findByName(name).isPresent()) {
            throw new BadRequestException("Calendar name already exists");
        }
        HolidayCalendar cal = HolidayCalendar.builder()
                .name(name).region(region).year(year).build();
        return calendarRepository.save(cal);
    }

    @Transactional
    public void deleteCalendar(UUID id) {
        HolidayCalendar cal = calendarRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Calendar not found"));
        calendarRepository.delete(cal);
    }

    public List<Holiday> listHolidays(UUID calendarId, Integer year) {
        if (calendarId != null) {
            return holidayRepository.findAllByCalendarIdOrderByHolidayDate(calendarId);
        }
        return holidayRepository.findAll();
    }

    @Transactional
    public Holiday addHoliday(UUID calendarId, LocalDate holidayDate, String name, Boolean isOptional) {
        if (calendarRepository.findById(calendarId).isEmpty()) {
            throw new BadRequestException("Calendar not found");
        }
        Holiday holiday = Holiday.builder()
                .calendarId(calendarId)
                .holidayDate(holidayDate)
                .name(name)
                .isOptional(isOptional != null ? isOptional : false)
                .build();
        return holidayRepository.save(holiday);
    }

    @Transactional
    public void removeHoliday(UUID id) {
        Holiday h = holidayRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Holiday not found"));
        holidayRepository.delete(h);
    }
}
