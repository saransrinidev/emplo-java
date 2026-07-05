package com.emplo.controller;

import com.emplo.entity.User;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.PayslipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/payslip")
@RequiredArgsConstructor
public class PayslipController {

    private final PayslipService payslipService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(
            @RequestParam(value = "employee_id", required = false) UUID employeeId,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year) {
        User user = currentUserProvider.getCurrentUser();

        LocalDate today = LocalDate.now();
        int m = month != null ? month : today.getMonthValue();
        int y = year != null ? year : today.getYear();

        byte[] pdf = payslipService.generatePayslip(user, employeeId, m, y);
        String filename = "payslip_" + y + "_" + String.format("%02d", m) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
