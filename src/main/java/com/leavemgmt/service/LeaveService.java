// src/main/java/com/leavemgmt/service/LeaveService.java
package com.leavemgmt.service;

import com.leavemgmt.dto.*;
import com.leavemgmt.exception.*;
import com.leavemgmt.model.*;
import com.leavemgmt.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;

    // =============================================
    // LEAVE REQUEST SUBMISSION
    // =============================================

    /**
     * Submit a new leave request with full validation
     */
    public LeaveResponseDTO submitLeaveRequest(LeaveRequestDTO dto) {
        log.info("Processing leave request for employee ID: {}", dto.getEmployeeId());

        // Step 1: Validate employee exists
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Employee", dto.getEmployeeId()));

        // Step 2: Validate date logic
        validateDates(dto);

        // Step 3: Check for conflicting leaves
        checkForConflicts(dto.getEmployeeId(), dto.getStartDate(),
            dto.getEndDate(), -1L);

        // Step 4: Calculate requested days
        int requestedDays = (int) (dto.getEndDate()
            .toEpochDay() - dto.getStartDate().toEpochDay()) + 1;

        // Step 5: Check leave balance (skip for UNPAID leave)
        if (dto.getLeaveType() != LeaveType.UNPAID) {
            validateLeaveBalance(dto.getEmployeeId(),
                dto.getLeaveType(), requestedDays);
        }

        // Step 6: Create and save leave request
        LeaveRequest request = new LeaveRequest();
        request.setEmployee(employee);
        request.setLeaveType(dto.getLeaveType());
        request.setStartDate(dto.getStartDate());
        request.setEndDate(dto.getEndDate());
        request.setReason(dto.getReason());
        request.setNumberOfDays(requestedDays);

        LeaveRequest savedRequest = leaveRequestRepository.save(request);
        log.info("Leave request submitted successfully with ID: {}", savedRequest.getId());

        return mapToResponseDTO(savedRequest);
    }

    // =============================================
    // LEAVE APPROVAL / REJECTION
    // =============================================

    /**
     * Approve or reject a leave request
     */
    public LeaveResponseDTO processLeaveRequest(LeaveApprovalDTO approvalDTO) {
        log.info("Processing approval for leave request ID: {}",
            approvalDTO.getLeaveRequestId());

        // Fetch the leave request
        LeaveRequest request = leaveRequestRepository
            .findById(approvalDTO.getLeaveRequestId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "LeaveRequest", approvalDTO.getLeaveRequestId()));

        // Validate current status is PENDING
        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new InvalidLeaveRequestException(
                "Only PENDING requests can be processed. Current status: "
                + request.getStatus());
        }

        // Validate approval status
        if (approvalDTO.getStatus() != LeaveStatus.APPROVED
                && approvalDTO.getStatus() != LeaveStatus.REJECTED) {
            throw new InvalidLeaveRequestException(
                "Status must be either APPROVED or REJECTED");
        }

        // Update request
        request.setStatus(approvalDTO.getStatus());
        request.setManagerComments(approvalDTO.getManagerComments());
        request.setApprovedBy(approvalDTO.getApprovedBy());
        request.setProcessedOn(LocalDateTime.now());

        // If APPROVED → deduct from leave balance
        if (approvalDTO.getStatus() == LeaveStatus.APPROVED) {
            deductLeaveBalance(request.getEmployee().getId(),
                request.getLeaveType(), request.getNumberOfDays());
            log.info("Leave approved. Deducted {} days from {} balance",
                request.getNumberOfDays(), request.getLeaveType());
        } else {
            log.info("Leave request ID: {} rejected by {}",
                request.getId(), approvalDTO.getApprovedBy());
        }

        LeaveRequest updatedRequest = leaveRequestRepository.save(request);
        return mapToResponseDTO(updatedRequest);
    }

    // =============================================
    // LEAVE CANCELLATION
    // =============================================

    /**
     * Cancel an existing leave request
     */
    public LeaveResponseDTO cancelLeaveRequest(Long requestId, Long employeeId) {
        log.info("Cancelling leave request ID: {}", requestId);

        LeaveRequest request = leaveRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", requestId));

        // Ensure the employee owns this request
        if (!request.getEmployee().getId().equals(employeeId)) {
            throw new InvalidLeaveRequestException(
                "You are not authorized to cancel this leave request");
        }

        // Can only cancel PENDING or APPROVED requests
        if (request.getStatus() == LeaveStatus.CANCELLED) {
            throw new InvalidLeaveRequestException("Request is already cancelled");
        }

        if (request.getStatus() == LeaveStatus.REJECTED) {
            throw new InvalidLeaveRequestException(
                "Cannot cancel a rejected leave request");
        }

        // Restore balance if it was already approved
        if (request.getStatus() == LeaveStatus.APPROVED) {
            restoreLeaveBalance(request.getEmployee().getId(),
                request.getLeaveType(), request.getNumberOfDays());
            log.info("Restored {} days to {} balance",
                request.getNumberOfDays(), request.getLeaveType());
        }

        request.setStatus(LeaveStatus.CANCELLED);
        request.setProcessedOn(LocalDateTime.now());

        LeaveRequest cancelledRequest = leaveRequestRepository.save(request);
        return mapToResponseDTO(cancelledRequest);
    }

    // =============================================
    // QUERY METHODS
    // =============================================

    /**
     * Get all leave requests for an employee
     */
    @Transactional(readOnly = true)
    public List<LeaveResponseDTO> getEmployeeLeaveRequests(Long employeeId) {
        employeeRepository.findById(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));

        return leaveRequestRepository.findByEmployeeId(employeeId)
            .stream()
            .map(this::mapToResponseDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get all pending leave requests (for manager view)
     */
    @Transactional(readOnly = true)
    public List<LeaveResponseDTO> getAllPendingRequests() {
        return leaveRequestRepository.findByStatus(LeaveStatus.PENDING)
            .stream()
            .map(this::mapToResponseDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get leave balance for an employee
     */
    @Transactional(readOnly = true)
    public List<LeaveBalanceDTO> getLeaveBalance(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));

        return leaveBalanceRepository.findByEmployeeId(employeeId)
            .stream()
            .map(balance -> mapToBalanceDTO(balance, employee))
            .collect(Collectors.toList());
    }

    /**
     * Get specific leave request by ID
     */
    @Transactional(readOnly = true)
    public LeaveResponseDTO getLeaveRequestById(Long requestId) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "LeaveRequest", requestId));
        return mapToResponseDTO(request);
    }

    // =============================================
    // PRIVATE HELPER METHODS
    // =============================================

    private void validateDates(LeaveRequestDTO dto) {
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new InvalidLeaveRequestException(
                "End date cannot be before start date");
        }

        if (dto.getStartDate().equals(dto.getEndDate())
                && dto.getStartDate().isBefore(
                    java.time.LocalDate.now())) {
            throw new InvalidLeaveRequestException(
                "Cannot apply for leave in the past");
        }
    }

    private void checkForConflicts(Long employeeId,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            Long excludeId) {

        List<LeaveRequest> conflicts = leaveRequestRepository
            .findConflictingLeaves(employeeId, startDate, endDate, excludeId);

        if (!conflicts.isEmpty()) {
            LeaveRequest conflict = conflicts.get(0);
            throw new LeaveConflictException(String.format(
                "Leave request conflicts with existing request ID: %d "
                + "(from %s to %s, Status: %s)",
                conflict.getId(),
                conflict.getStartDate(),
                conflict.getEndDate(),
                conflict.getStatus()
            ));
        }
    }

    private void validateLeaveBalance(Long employeeId,
            LeaveType leaveType, int requestedDays) {

        LeaveBalance balance = leaveBalanceRepository
            .findByEmployeeIdAndLeaveType(employeeId, leaveType)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Leave balance not found for type: " + leaveType));

        if (balance.getAvailableDays() < requestedDays) {
            throw new InsufficientLeaveException(
                requestedDays, balance.getAvailableDays());
        }
    }

    private void deductLeaveBalance(Long employeeId,
            LeaveType leaveType, int days) {

        if (leaveType == LeaveType.UNPAID) return;

        LeaveBalance balance = leaveBalanceRepository
            .findByEmployeeIdAndLeaveType(employeeId, leaveType)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Leave balance not found for type: " + leaveType));

        balance.deductDays(days);
        leaveBalanceRepository.save(balance);
    }

    private void restoreLeaveBalance(Long employeeId,
            LeaveType leaveType, int days) {

        if (leaveType == LeaveType.UNPAID) return;

        LeaveBalance balance = leaveBalanceRepository
            .findByEmployeeIdAndLeaveType(employeeId, leaveType)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Leave balance not found for type: " + leaveType));

        balance.restoreDays(days);
        leaveBalanceRepository.save(balance);
    }

    // =============================================
    // MAPPING METHODS
    // =============================================

    private LeaveResponseDTO mapToResponseDTO(LeaveRequest request) {
        LeaveResponseDTO dto = new LeaveResponseDTO();
        dto.setId(request.getId());
        dto.setEmployeeId(request.getEmployee().getId());
        dto.setEmployeeName(request.getEmployee().getName());
        dto.setDepartment(request.getEmployee().getDepartment());
        dto.setLeaveType(request.getLeaveType());
        dto.setStartDate(request.getStartDate());
        dto.setEndDate(request.getEndDate());
        dto.setNumberOfDays(request.getNumberOfDays());
        dto.setReason(request.getReason());
        dto.setStatus(request.getStatus());
        dto.setManagerComments(request.getManagerComments());
        dto.setApprovedBy(request.getApprovedBy());
        dto.setAppliedOn(request.getAppliedOn());
        dto.setProcessedOn(request.getProcessedOn());
        return dto;
    }

    private LeaveBalanceDTO mapToBalanceDTO(LeaveBalance balance, Employee employee) {
        LeaveBalanceDTO dto = new LeaveBalanceDTO();
        dto.setEmployeeId(employee.getId());
        dto.setEmployeeName(employee.getName());
        dto.setLeaveType(balance.getLeaveType());
        dto.setLeaveTypeDescription(balance.getLeaveType().getDescription());
        dto.setTotalDays(balance.getTotalDays());
        dto.setUsedDays(balance.getUsedDays());
        dto.setAvailableDays(balance.getAvailableDays());
        return dto;
    }
}