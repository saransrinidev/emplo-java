package com.emplo.controller;

import com.emplo.dto.profile.*;
import com.emplo.entity.User;
import com.emplo.security.CurrentUserProvider;
import com.emplo.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PutMapping("/photo")
    public ResponseEntity<ProfileResponse> updatePhoto(@Valid @RequestBody UpdatePhotoRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(profileService.updatePhoto(user, request));
    }

    @DeleteMapping("/photo")
    public ResponseEntity<ProfileResponse> removePhoto() {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(profileService.removePhoto(user));
    }
}
