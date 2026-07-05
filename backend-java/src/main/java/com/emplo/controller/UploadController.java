package com.emplo.controller;

import com.emplo.exception.BadRequestException;
import com.emplo.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadController {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        currentUserProvider.getCurrentUser(); // ensure authenticated

        if (file.isEmpty() || file.getOriginalFilename() == null) {
            throw new BadRequestException("No file provided");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File too large. Maximum 5MB.");
        }

        try {
            byte[] content = file.getBytes();
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            String b64 = Base64.getEncoder().encodeToString(content);
            String dataUrl = "data:" + contentType + ";base64," + b64;

            return ResponseEntity.ok(Map.of(
                    "url", dataUrl,
                    "filename", file.getOriginalFilename()
            ));
        } catch (Exception e) {
            throw new BadRequestException("Failed to process file: " + e.getMessage());
        }
    }
}
