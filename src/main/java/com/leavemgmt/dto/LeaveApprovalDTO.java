// src/main/java/com/leavemgmt/dto/LeaveApprovalDTO.java
package com.leavemgmt.dto;

import com.leavemgmt.model.LeaveStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LeaveApprovalDTO {

    @NotNull(message = "Leave request ID is required")
    private Long leaveRequestId;

    @NotNull(message = "Status is required")
    private LeaveStatus status; // APPROVED or REJECTED

    private String managerComments;

    @NotNull(message = "Approver name is required")
    private String approvedBy;
}