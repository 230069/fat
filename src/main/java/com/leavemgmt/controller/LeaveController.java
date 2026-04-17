// src/main/java/com/leavemgmt/controller/LeaveController.java
package com.leavemgmt.controller;

import com.leavemgmt.dto.*;
import com.leavemgmt.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    /**
     * POST /api/leaves/apply
     * Submit a new leave request
     */
    @PostMapping("/apply")
    public ResponseEntity<LeaveResponseDTO> applyLeave(
            @Valid @RequestBody LeaveRequestDTO requestDTO) {

        LeaveResponseDTO response = leaveService.submitLeaveRequest(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/leaves/process
     * Approve or reject a leave request
     */
    @PutMapping("/process")
    public ResponseEntity<LeaveResponseDTO> processLeave(
            @Valid @RequestBody LeaveApprovalDTO approvalDTO) {

        LeaveResponseDTO response = leaveService.processLeaveRequest(approvalDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/leaves/{requestId}/cancel?employeeId={id}
     * Cancel a leave request
     */
    @PutMapping("/{requestId}/cancel")
    public ResponseEntity<LeaveResponseDTO> cancelLeave(
            @PathVariable Long requestId,
            @RequestParam Long employeeId) {

        LeaveResponseDTO response = leaveService.cancelLeaveRequest(
            requestId, employeeId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/leaves/employee/{employeeId}
     * Get all leave requests for an employee
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<LeaveResponseDTO>> getEmployeeLeaves(
            @PathVariable Long employeeId) {

        List<LeaveResponseDTO> leaves = leaveService
            .getEmployeeLeaveRequests(employeeId);
        return ResponseEntity.ok(leaves);
    }

    /**
     * GET /api/leaves/pending
     * Get all pending leave requests (manager view)
     */
    @GetMapping("/pending")
    public ResponseEntity<List<LeaveResponseDTO>> getPendingLeaves() {

        List<LeaveResponseDTO> pendingLeaves = leaveService.getAllPendingRequests();
        return ResponseEntity.ok(pendingLeaves);
    }

    /**
     * GET /api/leaves/{requestId}
     * Get specific leave request
     */
    @GetMapping("/{requestId}")
    public ResponseEntity<LeaveResponseDTO> getLeaveById(
            @PathVariable Long requestId) {

        LeaveResponseDTO response = leaveService.getLeaveRequestById(requestId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/leaves/balance/{employeeId}
     * Get leave balance for employee
     */
    @GetMapping("/balance/{employeeId}")
    public ResponseEntity<List<LeaveBalanceDTO>> getLeaveBalance(
            @PathVariable Long employeeId) {

        List<LeaveBalanceDTO> balance = leaveService.getLeaveBalance(employeeId);
        return ResponseEntity.ok(balance);
    }
}