package com.emplo.dto.profile;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdatePhotoRequest {
    @NotBlank
    private String profilePhoto;
}
