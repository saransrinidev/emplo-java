package com.emplo.controller;

import com.emplo.exception.BadRequestException;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.OcrService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final OcrService ocrService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/parse-certificate")
    public ResponseEntity<OcrService.ParsedCertificate> parseCertificate(@RequestBody OcrRequest request) {
        currentUserProvider.getCurrentUser(); // ensure authenticated

        String text = request.getText();

        if (text == null && request.getImageBase64() != null) {
            throw new BadRequestException("Server-side OCR not available. Please use client-side text extraction.");
        }
        if (text == null) {
            throw new BadRequestException("Provide either text or image_base64");
        }

        return ResponseEntity.ok(ocrService.parseCertificateText(text));
    }

    @Data
    public static class OcrRequest {
        private String text;
        private String imageBase64;
    }
}
