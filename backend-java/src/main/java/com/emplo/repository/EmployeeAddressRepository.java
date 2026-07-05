package com.emplo.repository;

import com.emplo.entity.EmployeeAddress;
import com.emplo.entity.enums.AddressType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeAddressRepository extends JpaRepository<EmployeeAddress, UUID> {

    List<EmployeeAddress> findAllByEmployeeId(UUID employeeId);

    Optional<EmployeeAddress> findByEmployeeIdAndAddressType(UUID employeeId, AddressType type);
}
