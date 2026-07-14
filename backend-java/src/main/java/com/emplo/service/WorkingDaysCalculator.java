package com.emplo.service;

import com.emplo.entity.Holiday;
import com.emplo.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Calculates the number of working days between two dates (inclusive),
 * excluding weekends (Saturday, Sunday) and configured non-optional holidays.
 */
@Service
@RequiredArgsConstructor
public class WorkingDaysCalculator {

    private final HolidayRepository holidayRepository;

    /**
     * Count working days from startDate to endDate inclusive.
     * Excludes Saturdays, Sundays, and mandatory (non-optional) holidays.
     */
    public double calculateWorkingDays(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return 0;
        }

        // Load mandatory holidays within the range into a fast lookup set
        Set<LocalDate> holidayDates = new HashSet<>();
        for (Holiday h : holidayRepository.findAllByHolidayDateBetween(startDate, endDate)) {
            // Optional holidays don't reduce the leave count (employee may still work)
            if (h.getIsOptional() == null || !h.getIsOptional()) {
                holidayDates.add(h.getHolidayDate());
            }
        }

        int workingDays = 0;
        LocalDate d = startDate;
        while (!d.isAfter(endDate)) {
            DayOfWeek dow = d.getDayOfWeek();
            boolean isWeekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
            boolean isHoliday = holidayDates.contains(d);
            if (!isWeekend && !isHoliday) {
                workingDays++;
            }
            d = d.plusDays(1);
        }
        return workingDays;
    }
}
