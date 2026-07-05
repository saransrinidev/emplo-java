package com.emplo.service;

import com.emplo.entity.Employee;
import com.emplo.entity.SalaryRevision;
import com.emplo.entity.User;
import com.emplo.entity.enums.ApprovalStatus;
import com.emplo.exception.BadRequestException;
import com.emplo.exception.NotFoundException;
import com.emplo.repository.EmployeeRepository;
import com.emplo.repository.SalaryRevisionRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PayslipService {

    private final EmployeeRepository employeeRepository;
    private final SalaryRevisionRepository salaryRevisionRepository;
    private final AuthorizationService authorizationService;

    public byte[] generatePayslip(User user, UUID employeeId, int month, int year) {
        UUID targetId = employeeId != null ? employeeId : user.getEmployeeId();
        if (targetId == null) {
            throw new BadRequestException("No employee record linked");
        }
        authorizationService.requireViewEmployee(user, targetId);

        Employee emp = employeeRepository.findById(targetId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        SalaryRevision revision = salaryRevisionRepository
                .findFirstByEmployeeIdAndApprovalStatusOrderByEffectiveDateDesc(targetId, ApprovalStatus.approved)
                .orElseThrow(() -> new BadRequestException("No approved salary found for this employee"));

        double annualCtc = revision.getRevisedSalary().doubleValue();
        return buildPdf(emp, month, year, annualCtc);
    }

    private byte[] buildPdf(Employee emp, int month, int year, double annualCtc) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);

            double monthlyGross = Math.round(annualCtc / 12 * 100.0) / 100.0;
            double basic = Math.round(monthlyGross * 0.4 * 100.0) / 100.0;
            double hra = Math.round(monthlyGross * 0.2 * 100.0) / 100.0;
            double specialAllowance = Math.round(monthlyGross * 0.2 * 100.0) / 100.0;
            double otherAllowance = Math.round(monthlyGross * 0.1 * 100.0) / 100.0;
            double pfDeduction = Math.round(basic * 0.12 * 100.0) / 100.0;
            double professionalTax = 200.0;
            double totalDeductions = Math.round((pfDeduction + professionalTax) * 100.0) / 100.0;
            double netPay = Math.round((monthlyGross - totalDeductions) * 100.0) / 100.0;

            String monthName = LocalDate.of(year, month, 1).format(DateTimeFormatter.ofPattern("MMMM yyyy"));

            // Title
            doc.add(new Paragraph("EMPLO")
                    .setFontSize(20).setBold()
                    .setFontColor(new DeviceRgb(30, 64, 175)));
            doc.add(new Paragraph("Payslip for " + monthName)
                    .setFontSize(10).setFontColor(ColorConstants.GRAY));
            doc.add(new Paragraph("\n"));

            // Employee info
            doc.add(new Paragraph("Employee Information").setFontSize(12).setBold());
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{25, 35, 25, 35}))
                    .useAllAvailableWidth();
            infoTable.addCell(infoCell("Employee Name"));
            infoTable.addCell(valueCell(emp.getFullName()));
            infoTable.addCell(infoCell("Employee Code"));
            infoTable.addCell(valueCell(emp.getEmployeeCode()));
            infoTable.addCell(infoCell("Department"));
            infoTable.addCell(valueCell(emp.getDepartment() != null ? emp.getDepartment() : "—"));
            infoTable.addCell(infoCell("Designation"));
            infoTable.addCell(valueCell(emp.getDesignation() != null ? emp.getDesignation() : "—"));
            infoTable.addCell(infoCell("Date of Joining"));
            infoTable.addCell(valueCell(emp.getDateOfJoining() != null ? emp.getDateOfJoining().toString() : "—"));
            infoTable.addCell(infoCell("Pay Period"));
            infoTable.addCell(valueCell(monthName));
            doc.add(infoTable);
            doc.add(new Paragraph("\n"));

            // Earnings and Deductions
            doc.add(new Paragraph("Earnings & Deductions").setFontSize(12).setBold());
            Table salaryTable = new Table(UnitValue.createPercentArray(new float[]{30, 20, 30, 20}))
                    .useAllAvailableWidth();
            salaryTable.addHeaderCell(headerCell("Earnings"));
            salaryTable.addHeaderCell(headerCell("Amount (₹)"));
            salaryTable.addHeaderCell(headerCell("Deductions"));
            salaryTable.addHeaderCell(headerCell("Amount (₹)"));

            salaryTable.addCell(dataCell("Basic Pay"));
            salaryTable.addCell(amountCell(basic));
            salaryTable.addCell(dataCell("Provident Fund (12%)"));
            salaryTable.addCell(amountCell(pfDeduction));

            salaryTable.addCell(dataCell("HRA"));
            salaryTable.addCell(amountCell(hra));
            salaryTable.addCell(dataCell("Professional Tax"));
            salaryTable.addCell(amountCell(professionalTax));

            salaryTable.addCell(dataCell("Special Allowance"));
            salaryTable.addCell(amountCell(specialAllowance));
            salaryTable.addCell(dataCell(""));
            salaryTable.addCell(dataCell(""));

            salaryTable.addCell(dataCell("Other Allowance"));
            salaryTable.addCell(amountCell(otherAllowance));
            salaryTable.addCell(dataCell(""));
            salaryTable.addCell(dataCell(""));

            salaryTable.addCell(new Cell().add(new Paragraph("Gross Earnings").setBold()).setFontSize(9));
            salaryTable.addCell(new Cell().add(new Paragraph(String.format("%.2f", monthlyGross)).setBold()).setFontSize(9).setTextAlignment(TextAlignment.RIGHT));
            salaryTable.addCell(new Cell().add(new Paragraph("Total Deductions").setBold()).setFontSize(9));
            salaryTable.addCell(new Cell().add(new Paragraph(String.format("%.2f", totalDeductions)).setBold()).setFontSize(9).setTextAlignment(TextAlignment.RIGHT));

            doc.add(salaryTable);
            doc.add(new Paragraph("\n"));

            // Net pay
            Table netTable = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                    .useAllAvailableWidth();
            netTable.addCell(new Cell().add(new Paragraph("Net Pay (Take Home)").setBold().setFontSize(12))
                    .setBackgroundColor(new DeviceRgb(30, 64, 175)).setFontColor(ColorConstants.WHITE).setPadding(8));
            netTable.addCell(new Cell().add(new Paragraph(String.format("₹ %.2f", netPay)).setBold().setFontSize(12))
                    .setBackgroundColor(new DeviceRgb(30, 64, 175)).setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(8));
            doc.add(netTable);
            doc.add(new Paragraph("\n"));

            // Footer
            doc.add(new Paragraph("This is a system-generated payslip. No signature required.")
                    .setFontSize(8).setFontColor(ColorConstants.GRAY));
            doc.add(new Paragraph("Generated on " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) + " by Emplo HRMS")
                    .setFontSize(8).setFontColor(ColorConstants.GRAY));

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate payslip PDF", e);
        }
    }

    private Cell infoCell(String text) {
        return new Cell().add(new Paragraph(text).setFontSize(8).setFontColor(ColorConstants.GRAY)).setBorder(null);
    }

    private Cell valueCell(String text) {
        return new Cell().add(new Paragraph(text).setFontSize(9)).setBorder(null);
    }

    private Cell headerCell(String text) {
        return new Cell().add(new Paragraph(text).setBold().setFontSize(9))
                .setBackgroundColor(new DeviceRgb(241, 245, 249));
    }

    private Cell dataCell(String text) {
        return new Cell().add(new Paragraph(text).setFontSize(9));
    }

    private Cell amountCell(double amount) {
        return new Cell().add(new Paragraph(String.format("%.2f", amount)).setFontSize(9))
                .setTextAlignment(TextAlignment.RIGHT);
    }
}
