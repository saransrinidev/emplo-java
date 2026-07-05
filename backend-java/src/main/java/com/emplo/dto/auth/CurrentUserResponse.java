package com.emplo.dto.auth;

import com.emplo.entity.enums.RoleName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CurrentUserResponse {
    private String id;
    private String email;
    private RoleName role;
    private String name;
    private String profilePhoto;
}
