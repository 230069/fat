// src/main/java/com/leavemgmt/dto/LeaveResponseDTO.java
package com.leavemgmt.dto;

import com.leavemgmt.model.LeaveStatus;
import com.leavemgmt.model.LeaveType;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class LeaveResponseDTO {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private String department;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private int numberOfDays;
    private String reason;
    private LeaveStatus status;
    private String managerComments;
    private String approvedBy;
    private LocalDateTime appliedOn;
    private LocalDateTime processedOn;
}