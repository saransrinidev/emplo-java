package com.emplo.service;

import com.emplo.entity.Department;
import com.emplo.entity.Designation;
import com.emplo.exception.BadRequestException;
import com.emplo.exception.NotFoundException;
import com.emplo.repository.DepartmentRepository;
import com.emplo.repository.DesignationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;

    @Cacheable(value = "departments", key = "#activeOnly")
    public List<Department> listDepartments(boolean activeOnly) {
        if (activeOnly) {
            return departmentRepository.findAllByIsActiveTrueOrderByName();
        }
        return departmentRepository.findAll();
    }

    @Transactional
    @CacheEvict(value = "departments", allEntries = true)
    public Department createDepartment(String name, String code, UUID headEmployeeId) {
        if (departmentRepository.findByName(name).isPresent()) {
            throw new BadRequestException("Department name already exists");
        }
        if (departmentRepository.findByCode(code).isPresent()) {
            throw new BadRequestException("Department code already exists");
        }
        Department dept = Department.builder()
                .name(name)
                .code(code)
                .headEmployeeId(headEmployeeId)
                .isActive(true)
                .build();
        return departmentRepository.save(dept);
    }

    @Transactional
    @CacheEvict(value = "departments", allEntries = true)
    public Department updateDepartment(UUID id, String name, String code, UUID headEmployeeId, Boolean isActive) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Department not found"));
        if (name != null && !name.equals(dept.getName())) {
            if (departmentRepository.findByName(name).isPresent()) {
                throw new BadRequestException("Department name already exists");
            }
            dept.setName(name);
        }
        if (code != null && !code.equals(dept.getCode())) {
            if (departmentRepository.findByCode(code).isPresent()) {
                throw new BadRequestException("Department code already exists");
            }
            dept.setCode(code);
        }
        if (headEmployeeId != null) dept.setHeadEmployeeId(headEmployeeId);
        if (isActive != null) dept.setIsActive(isActive);
        return departmentRepository.save(dept);
    }

    @Transactional
    @CacheEvict(value = "departments", allEntries = true)
    public void deleteDepartment(UUID id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Department not found"));
        departmentRepository.delete(dept);
    }

    @Cacheable(value = "designations", key = "#departmentId + '_' + #activeOnly")
    public List<Designation> listDesignations(UUID departmentId, boolean activeOnly) {
        if (departmentId != null && activeOnly) {
            return designationRepository.findAllByDepartmentIdAndIsActiveTrue(departmentId);
        }
        if (departmentId != null) {
            return designationRepository.findAllByDepartmentId(departmentId);
        }
        if (activeOnly) {
            return designationRepository.findAllByIsActiveTrueOrderByTitle();
        }
        return designationRepository.findAll();
    }

    @Transactional
    public Designation createDesignation(String title, UUID departmentId, Integer level) {
        if (departmentId != null && departmentRepository.findById(departmentId).isEmpty()) {
            throw new BadRequestException("Department not found");
        }
        if (designationRepository.findByTitleAndDepartmentId(title, departmentId).isPresent()) {
            throw new BadRequestException("Designation already exists for this department");
        }
        Designation desig = Designation.builder()
                .title(title)
                .departmentId(departmentId)
                .level(level)
                .isActive(true)
                .build();
        return designationRepository.save(desig);
    }

    @Transactional
    public Designation updateDesignation(UUID id, String title, UUID departmentId, Integer level, Boolean isActive) {
        Designation desig = designationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Designation not found"));
        if (title != null) desig.setTitle(title);
        if (departmentId != null) desig.setDepartmentId(departmentId);
        if (level != null) desig.setLevel(level);
        if (isActive != null) desig.setIsActive(isActive);
        return designationRepository.save(desig);
    }

    @Transactional
    public void deleteDesignation(UUID id) {
        Designation desig = designationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Designation not found"));
        designationRepository.delete(desig);
    }
}
