package com.emplo.controller;

import com.emplo.entity.PerformanceReview;
import com.emplo.entity.User;
import com.emplo.entity.enums.RoleName;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.AuthorizationService;
import com.emplo.service.PerformanceService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/performance")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;

    @GetMapping
    public ResponseEntity<List<PerformanceReview>> list(
            @RequestParam(value = "employee_id", required = false) UUID employeeId) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(performanceService.listReviews(user, employeeId));
    }

    @PostMapping
    public ResponseEntity<PerformanceReview> add(@RequestBody AddReviewRequest request) {
        User user = currentUserProvider.getCurrentUser();
        authorizationService.requireRole(user, RoleName.hr_admin);
        PerformanceReview review = performanceService.addReview(user, request.getEmployeeId(),
                request.getReviewPeriod(), request.getReviewDate(), request.getReviewerId(),
                request.getRating(), request.getStrengths(), request.getAreasForImprovement(), request.getComments());
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    @Data
    public static class AddReviewRequest {
        private UUID employeeId;
        private String reviewPeriod;
        private LocalDate reviewDate;
        private UUID reviewerId;
        private BigDecimal rating;
        private String strengths;
        private String areasForImprovement;
        private String comments;
    }
}
