package com.emplo.service;

import com.emplo.entity.Employee;
import com.emplo.entity.PerformanceReview;
import com.emplo.entity.User;
import com.emplo.exception.BadRequestException;
import com.emplo.exception.NotFoundException;
import com.emplo.repository.EmployeeRepository;
import com.emplo.repository.PerformanceReviewRepository;
import com.emplo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final PerformanceReviewRepository performanceReviewRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final AuthorizationService authorizationService;

    public List<PerformanceReview> listReviews(User user, UUID employeeId) {
        UUID target = employeeId != null ? employeeId : user.getEmployeeId();
        if (target == null) return List.of();
        authorizationService.requireViewEmployee(user, target);
        return performanceReviewRepository.findAllByEmployeeIdOrderByReviewDateDesc(target);
    }

    @Transactional
    public PerformanceReview addReview(User user, UUID employeeId, String reviewPeriod,
                                        LocalDate reviewDate, UUID reviewerId,
                                        BigDecimal rating, String strengths,
                                        String areasForImprovement, String comments) {
        if (employeeRepository.findById(employeeId).isEmpty()) {
            throw new NotFoundException("Employee not found");
        }
        if (reviewerId != null && employeeRepository.findById(reviewerId).isEmpty()) {
            throw new BadRequestException("Reviewer not found");
        }
        UUID finalReviewerId = reviewerId;
        if (finalReviewerId == null && user.getEmployeeId() != null) {
            if (employeeRepository.findById(user.getEmployeeId()).isPresent()) {
                finalReviewerId = user.getEmployeeId();
            }
        }

        PerformanceReview review = PerformanceReview.builder()
                .employeeId(employeeId)
                .reviewPeriod(reviewPeriod)
                .reviewDate(reviewDate)
                .reviewerId(finalReviewerId)
                .rating(rating)
                .strengths(strengths)
                .areasForImprovement(areasForImprovement)
                .comments(comments)
                .build();
        review = performanceReviewRepository.save(review);

        auditService.logAction(user.getId(), "add_performance_review", "performance",
                review.getId().toString(), Map.of("employee_id", employeeId.toString(),
                        "rating", rating != null ? rating.toString() : "N/A"));

        userRepository.findByEmployeeId(employeeId).ifPresent(empUser ->
                notificationService.createNotification(empUser.getId(), "Performance Review Added",
                        "A new performance review for " + (reviewPeriod != null ? reviewPeriod : "this period") + " has been submitted. Rating: " + (rating != null ? rating + "/5" : "N/A")));

        return review;
    }
}
