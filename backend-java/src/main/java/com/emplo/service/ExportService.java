package com.emplo.service;

import com.emplo.entity.AttendanceRecord;
import com.emplo.entity.Employee;
import com.emplo.entity.LeaveRequest;
import com.emplo.entity.SalaryRevision;
import com.emplo.repository.AttendanceRecordRepository;
import com.emplo.repository.EmployeeRepository;
import com.emplo.repository.LeaveRequestRepository;
import com.emplo.repository.SalaryRevisionRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final EmployeeRepository employeeRepository;
    private final SalaryRevisionRepository salaryRevisionRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;

    public String exportEmployees() throws IOException {
        List<Employee> employees = employeeRepository.findAll();
        StringWriter writer = new StringWriter();
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                .setHeader("employee_code", "full_name", "email", "mobile_number", "date_of_birth",
                        "gender", "marital_status", "date_of_joining", "department", "designation",
                        "employment_status", "work_location")
                .build());

        for (Employee e : employees) {
            printer.printRecord(
                    e.getEmployeeCode(), e.getFullName(), e.getEmail(),
                    e.getMobileNumber() != null ? e.getMobileNumber() : "",
                    e.getDateOfBirth() != null ? e.getDateOfBirth().toString() : "",
                    e.getGender() != null ? e.getGender() : "",
                    e.getMaritalStatus() != null ? e.getMaritalStatus() : "",
                    e.getDateOfJoining() != null ? e.getDateOfJoining().toString() : "",
                    e.getDepartment() != null ? e.getDepartment() : "",
                    e.getDesignation() != null ? e.getDesignation() : "",
                    e.getEmploymentStatus() != null ? e.getEmploymentStatus() : "",
                    e.getWorkLocation() != null ? e.getWorkLocation() : ""
            );
        }
        printer.flush();
        return writer.toString();
    }

    public String exportSalaryRevisions() throws IOException {
        List<SalaryRevision> revisions = salaryRevisionRepository.findAll();
        StringWriter writer = new StringWriter();
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                .setHeader("employee_code", "employee_name", "effective_date", "previous_salary",
                        "revised_salary", "revision_percentage", "approval_status", "comments")
                .build());

        for (SalaryRevision r : revisions) {
            Employee emp = employeeRepository.findById(r.getEmployeeId()).orElse(null);
            printer.printRecord(
                    emp != null ? emp.getEmployeeCode() : "",
                    emp != null ? emp.getFullName() : "",
                    r.getEffectiveDate().toString(),
                    r.getPreviousSalary() != null ? r.getPreviousSalary().toString() : "",
                    r.getRevisedSalary().toString(),
                    r.getRevisionPercentage() != null ? r.getRevisionPercentage().toString() : "",
                    r.getApprovalStatus().name(),
                    r.getComments() != null ? r.getComments() : ""
            );
        }
        printer.flush();
        return writer.toString();
    }

    public String exportLeaveRequests() throws IOException {
        List<LeaveRequest> requests = leaveRequestRepository.findAll();
        StringWriter writer = new StringWriter();
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                .setHeader("employee_code", "employee_name", "leave_type", "start_date", "end_date",
                        "status", "reason", "manager_remarks", "hr_remarks")
                .build());

        for (LeaveRequest lr : requests) {
            Employee emp = employeeRepository.findById(lr.getEmployeeId()).orElse(null);
            printer.printRecord(
                    emp != null ? emp.getEmployeeCode() : "",
                    emp != null ? emp.getFullName() : "",
                    lr.getLeaveType().name(),
                    lr.getStartDate().toString(),
                    lr.getEndDate().toString(),
                    lr.getStatus().name(),
                    lr.getReason() != null ? lr.getReason() : "",
                    lr.getManagerRemarks() != null ? lr.getManagerRemarks() : "",
                    lr.getHrRemarks() != null ? lr.getHrRemarks() : ""
            );
        }
        printer.flush();
        return writer.toString();
    }

    public String exportAttendance(Integer month, Integer year) throws IOException {
        List<AttendanceRecord> records;
        if (month != null && year != null) {
            records = attendanceRecordRepository.findByEmployeeIdAndMonthAndYear(null, month, year);
            // Fallback to getting all and filtering
            records = attendanceRecordRepository.findAll().stream()
                    .filter(r -> r.getWorkDate().getMonthValue() == month && r.getWorkDate().getYear() == year)
                    .toList();
        } else {
            records = attendanceRecordRepository.findAll();
        }

        StringWriter writer = new StringWriter();
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                .setHeader("employee_code", "employee_name", "work_date", "check_in", "check_out",
                        "work_hours", "status", "source")
                .build());

        for (AttendanceRecord r : records) {
            Employee emp = employeeRepository.findById(r.getEmployeeId()).orElse(null);
            printer.printRecord(
                    emp != null ? emp.getEmployeeCode() : "",
                    emp != null ? emp.getFullName() : "",
                    r.getWorkDate().toString(),
                    r.getCheckIn() != null ? r.getCheckIn().toString() : "",
                    r.getCheckOut() != null ? r.getCheckOut().toString() : "",
                    r.getWorkHours() != null ? r.getWorkHours().toString() : "",
                    r.getStatus().name(),
                    r.getSource() != null ? r.getSource() : ""
            );
        }
        printer.flush();
        return writer.toString();
    }
}
