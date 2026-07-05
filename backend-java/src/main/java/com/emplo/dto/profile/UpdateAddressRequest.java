package com.emplo.dto.profile;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateAddressRequest {
    @NotNull
    private String addressType;
    private String addressLine;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}
