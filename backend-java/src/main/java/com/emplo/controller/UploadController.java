package com.emplo.controller;

import com.emplo.entity.User;
import com.emplo.exception.BadRequestException;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadController {

    private final CurrentUserProvider currentUserProvider;
    private final StorageService storageService;

    @PostMapping
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        User user = currentUserProvider.getCurrentUser();

        if (file.isEmpty() || file.getOriginalFilename() == null) {
            throw new BadRequestException("No file provided");
        }

        // Determine folder based on content type
        String folder = "documents";
        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("image/")) {
            folder = "images";
        }

        // Include employee context in path if available
        if (user.getEmployeeId() != null) {
            folder = folder + "/" + user.getEmployeeId().toString().substring(0, 8);
        }

        String url = storageService.upload(file, folder);

        return ResponseEntity.ok(Map.of(
                "url", url,
                "filename", file.getOriginalFilename()
        ));
    }
}
