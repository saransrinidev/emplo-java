package com.emplo.dto.profile;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EditableSectionsResponse {
    private boolean phone;
    private boolean address;
    private boolean certifications;
}
