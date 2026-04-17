// src/main/java/com/leavemgmt/dto/LeaveBalanceDTO.java
package com.leavemgmt.dto;

import com.leavemgmt.model.LeaveType;
import lombok.Data;

@Data
public class LeaveBalanceDTO {

    private Long employeeId;
    private String employeeName;
    private LeaveType leaveType;
    private String leaveTypeDescription;
    private int totalDays;
    private int usedDays;
    private int availableDays;
}