package com.emplo.service;

import com.emplo.dto.profile.*;
import com.emplo.entity.*;
import com.emplo.entity.enums.AddressType;
import com.emplo.entity.enums.EditableSection;
import com.emplo.entity.enums.RoleName;
import com.emplo.exception.BadRequestException;
import com.emplo.exception.ForbiddenException;
import com.emplo.exception.NotFoundException;
import com.emplo.repository.EmployeeAddressRepository;
import com.emplo.repository.EmployeeEditPermissionRepository;
import com.emplo.repository.EmployeeRepository;
import com.emplo.repository.EmergencyContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeAddressRepository addressRepository;
    private final EmergencyContactRepository emergencyContactRepository;
    private final EmployeeEditPermissionRepository editPermissionRepository;
    private final AuditService auditService;
    private final StorageService storageService;

    public ProfileResponse getProfile(User user) {
        if (user.getEmployeeId() == null) {
            throw new NotFoundException("No employee record is linked to this account.");
        }
        Employee emp = employeeRepository.findById(user.getEmployeeId())
                .orElseThrow(() -> new NotFoundException("Employee record not found"));
        return buildProfile(emp);
    }

    public EditableSectionsResponse getEditableSections(User user) {
        if (user.getRole().getName() == RoleName.hr_admin) {
            return EditableSectionsResponse.builder().phone(true).address(true).certifications(true).build();
        }
        if (user.getEmployeeId() == null) {
            return EditableSectionsResponse.builder().phone(false).address(false).certifications(false).build();
        }
        return EditableSectionsResponse.builder()
                .phone(hasActivePermission(user.getEmployeeId(), EditableSection.phone))
                .address(hasActivePermission(user.getEmployeeId(), EditableSection.address))
                .certifications(hasActivePermission(user.getEmployeeId(), EditableSection.certifications))
                .build();
    }

    @Transactional
    public ProfileResponse updatePhone(User user, UpdatePhoneRequest request) {
        if (user.getEmployeeId() == null) {
            throw new NotFoundException("No employee record linked");
        }
        if (!canEdit(user, EditableSection.phone)) {
            throw new ForbiddenException("You don't have active permission to edit your phone number.");
        }
        Employee emp = employeeRepository.findById(user.getEmployeeId())
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        emp.setMobileNumber(request.getMobileNumber());
        employeeRepository.save(emp);

        auditService.logAction(user.getId(), "update_phone", "employee",
                emp.getId().toString(), Map.of("mobile_number", request.getMobileNumber()));

        return buildProfile(emp);
    }

    @Transactional
    public ProfileResponse updateAddress(User user, UpdateAddressRequest request) {
        if (user.getEmployeeId() == null) {
            throw new NotFoundException("No employee record linked");
        }
        if (!canEdit(user, EditableSection.address)) {
            throw new ForbiddenException("You don't have active permission to edit your address.");
        }
        Employee emp = employeeRepository.findById(user.getEmployeeId())
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        AddressType addrType = AddressType.valueOf(request.getAddressType());
        EmployeeAddress address = addressRepository
                .findByEmployeeIdAndAddressType(emp.getId(), addrType)
                .orElse(null);

        if (address == null) {
            address = new EmployeeAddress();
            address.setEmployeeId(emp.getId());
            address.setAddressType(addrType);
        }
        address.setAddressLine(request.getAddressLine());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());
        addressRepository.save(address);

        auditService.logAction(user.getId(), "update_address", "employee",
                emp.getId().toString(), Map.of("address_type", request.getAddressType(), "city", request.getCity() != null ? request.getCity() : ""));

        return buildProfile(emp);
    }

    @Transactional
    public ProfileResponse updatePhoto(User user, UpdatePhotoRequest request) {
        if (user.getEmployeeId() == null) {
            throw new NotFoundException("No employee record linked");
        }
        Employee emp = employeeRepository.findById(user.getEmployeeId())
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        String photo = request.getProfilePhoto();

        // Accept either a base64 data URL or a cloud URL
        if (photo.startsWith("data:image")) {
            // Legacy base64: validate size
            if (photo.length() > 2_800_000) {
                throw new BadRequestException("Image too large. Maximum 2MB.");
            }
        } else if (!photo.startsWith("http")) {
            throw new BadRequestException("Invalid image format. Must be a URL or data URL.");
        }

        emp.setProfilePhoto(photo);
        employeeRepository.save(emp);

        auditService.logAction(user.getId(), "update_photo", "employee",
                emp.getId().toString(), Map.of("field", "profile_photo"));

        return buildProfile(emp);
    }

    /**
     * Upload a profile photo as a multipart file.
     * Uploads original + generates 200x200 thumbnail to Supabase Storage.
     */
    @Transactional
    public Map<String, String> uploadPhotoFile(User user, org.springframework.web.multipart.MultipartFile file) {
        if (user.getEmployeeId() == null) {
            throw new NotFoundException("No employee record linked");
        }
        if (file.isEmpty()) {
            throw new BadRequestException("No file provided");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Only image files are allowed");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BadRequestException("Image too large. Maximum 10MB.");
        }

        Employee emp = employeeRepository.findById(user.getEmployeeId())
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        String folder = "profile-photos/" + emp.getId().toString().substring(0, 8);

        // Upload original
        String originalUrl = storageService.upload(file, folder);

        // Generate and upload thumbnail (200x200)
        String thumbnailUrl = null;
        try {
            byte[] thumbBytes = generateThumbnail(file.getBytes(), 200, 200, contentType);
            // Create a simple wrapper to upload raw bytes
            String thumbName = "thumb_" + (file.getOriginalFilename() != null ? file.getOriginalFilename() : "photo.jpg");
            thumbnailUrl = storageService.uploadBytes(thumbBytes, contentType, thumbName, folder + "/thumbnails");
        } catch (Exception e) {
            // Thumbnail generation is non-critical; use original as fallback
            thumbnailUrl = originalUrl;
        }

        // Store the thumbnail URL as the profile photo (smaller, faster to load)
        emp.setProfilePhoto(thumbnailUrl);
        employeeRepository.save(emp);

        auditService.logAction(user.getId(), "update_photo", "employee",
                emp.getId().toString(), Map.of("field", "profile_photo", "source", "upload"));

        return Map.of(
                "url", originalUrl,
                "thumbnail_url", thumbnailUrl,
                "profile_photo", thumbnailUrl
        );
    }

    private byte[] generateThumbnail(byte[] imageBytes, int width, int height, String contentType) throws Exception {
        java.awt.image.BufferedImage original = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(imageBytes));
        if (original == null) throw new BadRequestException("Cannot read image");

        // Calculate aspect-ratio-preserving dimensions
        int origW = original.getWidth();
        int origH = original.getHeight();
        double scale = Math.min((double) width / origW, (double) height / origH);
        int newW = (int) (origW * scale);
        int newH = (int) (origH * scale);

        java.awt.image.BufferedImage thumbnail = new java.awt.image.BufferedImage(newW, newH, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = thumbnail.createGraphics();
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.drawImage(original, 0, 0, newW, newH, null);
        g2d.dispose();

        String format = contentType.contains("png") ? "png" : "jpg";
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(thumbnail, format, baos);
        return baos.toByteArray();
    }

    @Transactional
    public ProfileResponse removePhoto(User user) {
        if (user.getEmployeeId() == null) {
            throw new NotFoundException("No employee record linked");
        }
        Employee emp = employeeRepository.findById(user.getEmployeeId())
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        emp.setProfilePhoto(null);
        employeeRepository.save(emp);
        return buildProfile(emp);
    }

    private boolean hasActivePermission(UUID employeeId, EditableSection section) {
        Instant now = Instant.now();
        return editPermissionRepository
                .findByEmployeeIdAndSectionAndIsRevokedFalseAndStartAtBeforeAndExpiryAtAfter(
                        employeeId, section, now, now)
                .isPresent();
    }

    private boolean canEdit(User user, EditableSection section) {
        if (user.getRole().getName() == RoleName.hr_admin) return true;
        if (user.getEmployeeId() == null) return false;
        return hasActivePermission(user.getEmployeeId(), section);
    }

    private ProfileResponse buildProfile(Employee emp) {
        String managerName = null;
        if (emp.getManagerId() != null) {
            managerName = employeeRepository.findById(emp.getManagerId())
                    .map(Employee::getFullName).orElse(null);
        }

        List<EmployeeAddress> addresses = addressRepository.findAllByEmployeeId(emp.getId());
        List<EmergencyContact> contacts = emergencyContactRepository.findAllByEmployeeId(emp.getId());

        return ProfileResponse.builder()
                .id(emp.getId())
                .employeeCode(emp.getEmployeeCode())
                .fullName(emp.getFullName())
                .email(emp.getEmail())
                .mobileNumber(emp.getMobileNumber())
                .dateOfBirth(emp.getDateOfBirth())
                .gender(emp.getGender())
                .maritalStatus(emp.getMaritalStatus())
                .dateOfJoining(emp.getDateOfJoining())
                .department(emp.getDepartment())
                .designation(emp.getDesignation())
                .managerName(managerName)
                .employmentStatus(emp.getEmploymentStatus())
                .workLocation(emp.getWorkLocation())
                .profilePhoto(emp.getProfilePhoto())
                .addresses(addresses.stream().map(a -> ProfileResponse.AddressDto.builder()
                        .id(a.getId())
                        .addressType(a.getAddressType().name())
                        .addressLine(a.getAddressLine())
                        .city(a.getCity())
                        .state(a.getState())
                        .postalCode(a.getPostalCode())
                        .country(a.getCountry())
                        .build()).toList())
                .emergencyContacts(contacts.stream().map(c -> ProfileResponse.EmergencyContactDto.builder()
                        .id(c.getId())
                        .name(c.getContactName())
                        .relationship(c.getRelationshipTo())
                        .phone(c.getContactNumber())
                        .build()).toList())
                .build();
    }
}
