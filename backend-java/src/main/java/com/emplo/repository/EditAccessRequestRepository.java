package com.emplo.repository;

import com.emplo.entity.EditAccessRequest;
import com.emplo.entity.enums.EditRequestStatus;
import com.emplo.entity.enums.EditableSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EditAccessRequestRepository extends JpaRepository<EditAccessRequest, UUID> {

    List<EditAccessRequest> findAllByEmployeeIdOrderByCreatedAtDesc(UUID empId);

    List<EditAccessRequest> findAllByStatusInOrderByCreatedAtAsc(List<EditRequestStatus> statuses);

    List<EditAccessRequest> findAllByStatusIn(List<EditRequestStatus> statuses);

    List<EditAccessRequest> findAllByEmployeeIdAndStatusIn(UUID empId, List<EditRequestStatus> statuses);

    Optional<EditAccessRequest> findByEmployeeIdAndSectionAndStatusIn(
            UUID empId, EditableSection section, List<EditRequestStatus> statuses);
}
