package com.emplo.repository;

import com.emplo.entity.ProfileChangeRequest;
import com.emplo.entity.enums.ChangeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProfileChangeRequestRepository extends JpaRepository<ProfileChangeRequest, UUID> {

    List<ProfileChangeRequest> findAllByEmployeeIdOrderByCreatedAtDesc(UUID empId);

    List<ProfileChangeRequest> findAllByStatusOrderByCreatedAtAsc(ChangeStatus status);
}
