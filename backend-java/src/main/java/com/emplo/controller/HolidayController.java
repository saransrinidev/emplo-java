package com.emplo.controller;

import com.emplo.entity.Holiday;
import com.emplo.entity.HolidayCalendar;
import com.emplo.entity.User;
import com.emplo.entity.enums.RoleName;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.AuthorizationService;
import com.emplo.service.HolidayService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;

    @GetMapping("/calendars")
    public ResponseEntity<List<HolidayCalendar>> listCalendars(
            @RequestParam(value = "year", required = false) Integer year) {
        currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(holidayService.listCalendars(year));
    }

    @PostMapping("/calendars")
    public ResponseEntity<HolidayCalendar> createCalendar(@RequestBody CalendarRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(holidayService.createCalendar(request.getName(), request.getRegion(), request.getYear()));
    }

    @DeleteMapping("/calendars/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCalendar(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        holidayService.deleteCalendar(id);
    }

    @GetMapping
    public ResponseEntity<List<Holiday>> listHolidays(
            @RequestParam(value = "calendar_id", required = false) UUID calendarId,
            @RequestParam(value = "year", required = false) Integer year) {
        currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(holidayService.listHolidays(calendarId, year));
    }

    @PostMapping
    public ResponseEntity<Holiday> addHoliday(@RequestBody HolidayRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(holidayService.addHoliday(request.getCalendarId(), request.getHolidayDate(), request.getName(), request.getIsOptional()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeHoliday(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        holidayService.removeHoliday(id);
    }

    @Data
    public static class CalendarRequest {
        private String name;
        private String region;
        private Integer year;
    }

    @Data
    public static class HolidayRequest {
        private UUID calendarId;
        private LocalDate holidayDate;
        private String name;
        private Boolean isOptional;
    }
}
