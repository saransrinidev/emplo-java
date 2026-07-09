package com.emplo.controller;

import com.emplo.dto.profile.*;
import com.emplo.entity.User;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public ResponseEntity<ProfileResponse> getMyProfile() {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(profileService.getProfile(user));
    }

    @GetMapping("/editable-sections")
    public ResponseEntity<EditableSectionsResponse> getEditableSections() {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(profileService.getEditableSections(user));
    }

    @PutMapping("/phone")
    public ResponseEntity<ProfileResponse> updatePhone(@Valid @RequestBody UpdatePhoneRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(profileService.updatePhone(user, request));
    }

    @PutMapping("/address")
    public ResponseEntity<ProfileResponse> updateAddress(@Valid @RequestBody UpdateAddressRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(profileService.updateAddress(user, request));
    }

    /**
     * Upload profile photo as multipart file → stored in Supabase Storage.
     * Also accepts legacy JSON body with base64 data URL for backward compat.
     */
    @PutMapping("/photo")
    public ResponseEntity<ProfileResponse> updatePhoto(@Valid @RequestBody UpdatePhotoRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(profileService.updatePhoto(user, request));
    }

    /**
     * New: Upload profile photo as multipart/form-data.
     * Generates a thumbnail and stores both in cloud storage.
     * Returns profile with the new photo URL.
     */
    @PostMapping("/photo/upload")
    public ResponseEntity<Map<String, String>> uploadPhoto(@RequestParam("file") MultipartFile file) {
        User user = currentUserProvider.getCurrentUser();
        Map<String, String> result = profileService.uploadPhotoFile(user, file);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/photo")
    public ResponseEntity<ProfileResponse> removePhoto() {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(profileService.removePhoto(user));
    }
}
