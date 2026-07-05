package com.emplo.dto.profile;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProfileResponse {
    private UUID id;
    private String employeeCode;
    private String fullName;
    private String email;
    private String mobileNumber;
    private LocalDate dateOfBirth;
    private String gender;
    private String maritalStatus;
    private LocalDate dateOfJoining;
    private String department;
    private String designation;
    private String managerName;
    private String employmentStatus;
    private String workLocation;
    private String profilePhoto;
    private List<AddressDto> addresses;
    private List<EmergencyContactDto> emergencyContacts;

    @Data
    @Builder
    public static class AddressDto {
        private UUID id;
        private String addressType;
        private String addressLine;
        private String city;
        private String state;
        private String postalCode;
        private String country;
    }

    @Data
    @Builder
    public static class EmergencyContactDto {
        private UUID id;
        private String name;
        private String relationship;
        private String phone;
    }
}
