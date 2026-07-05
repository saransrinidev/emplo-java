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

        if (!request.getProfilePhoto().startsWith("data:image")) {
            throw new BadRequestException("Invalid image format. Must be a data URL.");
        }
        if (request.getProfilePhoto().length() > 2_800_000) {
            throw new BadRequestException("Image too large. Maximum 2MB.");
        }

        emp.setProfilePhoto(request.getProfilePhoto());
        employeeRepository.save(emp);

        auditService.logAction(user.getId(), "update_photo", "employee",
                emp.getId().toString(), Map.of("field", "profile_photo"));

        return buildProfile(emp);
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
