// src/test/java/com/leavemgmt/service/LeaveServiceTest.java
package com.leavemgmt.service;

import com.leavemgmt.dto.LeaveApprovalDTO;
import com.leavemgmt.dto.LeaveRequestDTO;
import com.leavemgmt.dto.LeaveResponseDTO;
import com.leavemgmt.exception.*;
import com.leavemgmt.model.*;
import com.leavemgmt.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Leave Service Unit Tests")
class LeaveServiceTest {

    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private LeaveBalanceRepository leaveBalanceRepository;
    @Mock private EmployeeRepository employeeRepository;

    @InjectMocks private LeaveService leaveService;

    // Test data
    private Employee testEmployee;
    private LeaveBalance testBalance;
    private LeaveRequest testLeaveRequest;
    private LeaveRequestDTO testRequestDTO;

    @BeforeEach
    void setUp() {
        // Create test employee
        testEmployee = Employee.builder()
            .id(1L)
            .name("Alice Johnson")
            .email("alice@company.com")
            .department("Engineering")
            .role("Developer")
            .build();

        // Create leave balance (21 days available)
        testBalance = LeaveBalance.builder()
            .id(1L)
            .employee(testEmployee)
            .leaveType(LeaveType.ANNUAL)
            .totalDays(21)
            .usedDays(0)
            .availableDays(21)
            .build();

        // Create base leave request
        testLeaveRequest = new LeaveRequest();
        testLeaveRequest.setId(1L);
        testLeaveRequest.setEmployee(testEmployee);
        testLeaveRequest.setLeaveType(LeaveType.ANNUAL);
        testLeaveRequest.setStartDate(LocalDate.now().plusDays(5));
        testLeaveRequest.setEndDate(LocalDate.now().plusDays(9));
        testLeaveRequest.setNumberOfDays(5);
        testLeaveRequest.setStatus(LeaveStatus.PENDING);
        testLeaveRequest.setReason("Family vacation");

        // Create base DTO
        testRequestDTO = new LeaveRequestDTO();
        testRequestDTO.setEmployeeId(1L);
        testRequestDTO.setLeaveType(LeaveType.ANNUAL);
        testRequestDTO.setStartDate(LocalDate.now().plusDays(5));
        testRequestDTO.setEndDate(LocalDate.now().plusDays(9));
        testRequestDTO.setReason("Family vacation");
    }

    // ==========================================================
    // TEST GROUP 1: LEAVE REQUEST SUBMISSION
    // ==========================================================

    @Nested
    @DisplayName("Leave Request Submission Tests")
    class LeaveSubmissionTests {

        @Test
        @DisplayName("Should submit leave request successfully")
        void shouldSubmitLeaveRequestSuccessfully() {
            // Arrange
            when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(testEmployee));
            when(leaveRequestRepository.findConflictingLeaves(
                anyLong(), any(), any(), anyLong()))
                .thenReturn(Collections.emptyList());
            when(leaveBalanceRepository.findByEmployeeIdAndLeaveType(1L, LeaveType.ANNUAL))
                .thenReturn(Optional.of(testBalance));
            when(leaveRequestRepository.save(any(LeaveRequest.class)))
                .thenReturn(testLeaveRequest);

            // Act
            LeaveResponseDTO result = leaveService.submitLeaveRequest(testRequestDTO);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getEmployeeId()).isEqualTo(1L);
            assertThat(result.getLeaveType()).isEqualTo(LeaveType.ANNUAL);
            assertThat(result.getStatus()).isEqualTo(LeaveStatus.PENDING);
            verify(leaveRequestRepository, times(1)).save(any(LeaveRequest.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when employee not found")
        void shouldThrowExceptionWhenEmployeeNotFound() {
            // Arrange
            when(employeeRepository.findById(999L))
                .thenReturn(Optional.empty());
            testRequestDTO.setEmployeeId(999L);

            // Act & Assert
            assertThatThrownBy(() -> leaveService.submitLeaveRequest(testRequestDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee not found with id: 999");

            verify(leaveRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw InvalidLeaveRequestException when end date before start")
        void shouldThrowExceptionWhenEndDateBeforeStartDate() {
            // Arrange
            when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(testEmployee));

            testRequestDTO.setStartDate(LocalDate.now().plusDays(10));
            testRequestDTO.setEndDate(LocalDate.now().plusDays(5));

            // Act & Assert
            assertThatThrownBy(() -> leaveService.submitLeaveRequest(testRequestDTO))
                .isInstanceOf(InvalidLeaveRequestException.class)
                .hasMessageContaining("End date cannot be before start date");
        }

        @Test
        @DisplayName("Should throw LeaveConflictException when dates overlap")
        void shouldThrowExceptionWhenDatesOverlap() {
            // Arrange
            when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(testEmployee));
            when(leaveRequestRepository.findConflictingLeaves(
                anyLong(), any(), any(), anyLong()))
                .thenReturn(List.of(testLeaveRequest));

            // Act & Assert
            assertThatThrownBy(() -> leaveService.submitLeaveRequest(testRequestDTO))
                .isInstanceOf(LeaveConflictException.class)
                .hasMessageContaining("conflicts with existing request");
        }

        @Test
        @DisplayName("Should throw InsufficientLeaveException when balance is low")
        void shouldThrowExceptionWhenInsufficientBalance() {
            // Arrange - Only 2 days available but requesting 5
            testBalance.setAvailableDays(2);
            testBalance.setUsedDays(19);

            when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(testEmployee));
            when(leaveRequestRepository.findConflictingLeaves(
                anyLong(), any(), any(), anyLong()))
                .thenReturn(Collections.emptyList());
            when(leaveBalanceRepository.findByEmployeeIdAndLeaveType(1L, LeaveType.ANNUAL))
                .thenReturn(Optional.of(testBalance));

            // Act & Assert
            assertThatThrownBy(() -> leaveService.submitLeaveRequest(testRequestDTO))
                .isInstanceOf(InsufficientLeaveException.class)
                .hasMessageContaining("Insufficient leave balance")
                .hasMessageContaining("Requested: 5 days")
                .hasMessageContaining("Available: 2 days");
        }

        @Test
        @DisplayName("Should allow UNPAID leave without balance check")
        void shouldAllowUnpaidLeaveWithoutBalanceCheck() {
            // Arrange
            testRequestDTO.setLeaveType(LeaveType.UNPAID);
            testLeaveRequest.setLeaveType(LeaveType.UNPAID);

            when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(testEmployee));
            when(leaveRequestRepository.findConflictingLeaves(
                anyLong(), any(), any(), anyLong()))
                .thenReturn(Collections.emptyList());
            when(leaveRequestRepository.save(any()))
                .thenReturn(testLeaveRequest);

            // Act
            LeaveResponseDTO result = leaveService.submitLeaveRequest(testRequestDTO);

            // Assert - Balance should never be checked for UNPAID
            assertThat(result).isNotNull();
            verify(leaveBalanceRepository, never())
                .findByEmployeeIdAndLeaveType(any(), any());
        }

        @Test
        @DisplayName("Should calculate number of days correctly")
        void shouldCalculateNumberOfDaysCorrectly() {
            // Arrange - 3 day leave
            testRequestDTO.setStartDate(LocalDate.now().plusDays(5));
            testRequestDTO.setEndDate(LocalDate.now().plusDays(7));

            LeaveRequest savedRequest = new LeaveRequest();
            savedRequest.setId(2L);
            savedRequest.setEmployee(testEmployee);
            savedRequest.setLeaveType(LeaveType.ANNUAL);
            savedRequest.setStartDate(testRequestDTO.getStartDate());
            savedRequest.setEndDate(testRequestDTO.getEndDate());
            savedRequest.setNumberOfDays(3);
            savedRequest.setStatus(LeaveStatus.PENDING);

            when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(testEmployee));
            when(leaveRequestRepository.findConflictingLeaves(
                anyLong(), any(), any(), anyLong()))
                .thenReturn(Collections.emptyList());
            when(leaveBalanceRepository.findByEmployeeIdAndLeaveType(
                1L, LeaveType.ANNUAL))
                .thenReturn(Optional.of(testBalance));
            when(leaveRequestRepository.save(any()))
                .thenReturn(savedRequest);

            // Act
            LeaveResponseDTO result = leaveService.submitLeaveRequest(testRequestDTO);

            // Assert
            assertThat(result.getNumberOfDays()).isEqualTo(3);
        }
    }

    // ==========================================================
    // TEST GROUP 2: LEAVE APPROVAL / REJECTION
    // ==========================================================

    @Nested
    @DisplayName("Leave Approval and Rejection Tests")
    class LeaveApprovalTests {

        private LeaveApprovalDTO approvalDTO;

        @BeforeEach
        void setUp() {
            approvalDTO = new LeaveApprovalDTO();
            approvalDTO.setLeaveRequestId(1L);
            approvalDTO.setApprovedBy("Manager Bob");
        }

        @Test
        @DisplayName("Should approve pending leave request")
        void shouldApprovePendingLeaveRequest() {
            // Arrange
            approvalDTO.setStatus(LeaveStatus.APPROVED);
            approvalDTO.setManagerComments("Approved. Enjoy your vacation!");

            LeaveRequest approvedRequest = new LeaveRequest();
            approvedRequest.setId(1L);
            approvedRequest.setEmployee(testEmployee);
            approvedRequest.setLeaveType(LeaveType.ANNUAL);
            approvedRequest.setNumberOfDays(5);
            approvedRequest.setStatus(LeaveStatus.APPROVED);
            approvedRequest.setApprovedBy("Manager Bob");

            when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(testLeaveRequest));
            when(leaveBalanceRepository.findByEmployeeIdAndLeaveType(
                1L, LeaveType.ANNUAL))
                .thenReturn(Optional.of(testBalance));
            when(leaveRequestRepository.save(any()))
                .thenReturn(approvedRequest);

            // Act
            LeaveResponseDTO result = leaveService.processLeaveRequest(approvalDTO);

            // Assert
            assertThat(result.getStatus()).isEqualTo(LeaveStatus.APPROVED);
            assertThat(result.getApprovedBy()).isEqualTo("Manager Bob");

            // Verify balance was deducted
            verify(leaveBalanceRepository, times(1)).save(any(LeaveBalance.class));
        }

        @Test
        @DisplayName("Should reject pending leave request")
        void shouldRejectPendingLeaveRequest() {
            // Arrange
            approvalDTO.setStatus(LeaveStatus.REJECTED);
            approvalDTO.setManagerComments("Team is short staffed during this period");

            LeaveRequest rejectedRequest = new LeaveRequest();
            rejectedRequest.setId(1L);
            rejectedRequest.setEmployee(testEmployee);
            rejectedRequest.setLeaveType(LeaveType.ANNUAL);
            rejectedRequest.setNumberOfDays(5);
            rejectedRequest.setStatus(LeaveStatus.REJECTED);

            when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(testLeaveRequest));
            when(leaveRequestRepository.save(any()))
                .thenReturn(rejectedRequest);

            // Act
            LeaveResponseDTO result = leaveService.processLeaveRequest(approvalDTO);

            // Assert
            assertThat(result.getStatus()).isEqualTo(LeaveStatus.REJECTED);

            // Verify balance was NOT deducted on rejection
            verify(leaveBalanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when processing already approved request")
        void shouldThrowExceptionForAlreadyApprovedRequest() {
            // Arrange
            testLeaveRequest.setStatus(LeaveStatus.APPROVED);
            approvalDTO.setStatus(LeaveStatus.APPROVED);

            when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(testLeaveRequest));

            // Act & Assert
            assertThatThrownBy(() -> leaveService.processLeaveRequest(approvalDTO))
                .isInstanceOf(InvalidLeaveRequestException.class)
                .hasMessageContaining("Only PENDING requests can be processed");
        }

        @Test
        @DisplayName("Should throw exception when processing already rejected request")
        void shouldThrowExceptionForAlreadyRejectedRequest() {
            // Arrange
            testLeaveRequest.setStatus(LeaveStatus.REJECTED);
            approvalDTO.setStatus(LeaveStatus.APPROVED);

            when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(testLeaveRequest));

            // Act & Assert
            assertThatThrownBy(() -> leaveService.processLeaveRequest(approvalDTO))
                .isInstanceOf(InvalidLeaveRequestException.class)
                .hasMessageContaining("Only PENDING requests can be processed");
        }

        @Test
        @DisplayName("Should throw exception for non-existent leave request")
        void shouldThrowExceptionForNonExistentLeaveRequest() {
            // Arrange
            approvalDTO.setLeaveRequestId(999L);
            approvalDTO.setStatus(LeaveStatus.APPROVED);

            when(leaveRequestRepository.findById(999L))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> leaveService.processLeaveRequest(approvalDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("LeaveRequest not found with id: 999");
        }

        @Test
        @DisplayName("Should deduct correct days from balance on approval")
        void shouldDeductCorrectDaysOnApproval() {
            // Arrange - 5 day leave request
            approvalDTO.setStatus(LeaveStatus.APPROVED);
            testLeaveRequest.setNumberOfDays(5);

            LeaveRequest approvedRequest = new LeaveRequest();
            approvedRequest.setId(1L);
            approvedRequest.setEmployee(testEmployee);
            approvedRequest.setLeaveType(LeaveType.ANNUAL);
            approvedRequest.setNumberOfDays(5);
            approvedRequest.setStatus(LeaveStatus.APPROVED);

            when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(testLeaveRequest));
            when(leaveBalanceRepository.findByEmployeeIdAndLeaveType(
                1L, LeaveType.ANNUAL))
                .thenReturn(Optional.of(testBalance));
            when(leaveRequestRepository.save(any()))
                .thenReturn(approvedRequest);

            // Act
            leaveService.processLeaveRequest(approvalDTO);

            // Assert - Verify balance was updated
            assertThat(testBalance.getUsedDays()).isEqualTo(5);
            assertThat(testBalance.getAvailableDays()).isEqualTo(16);
        }
    }

    // ==========================================================
    // TEST GROUP 3: LEAVE CANCELLATION
    // ==========================================================

    @Nested
    @DisplayName("Leave Cancellation Tests")
    class LeaveCancellationTests {

        @Test
        @DisplayName("Should cancel pending leave request successfully")
        void shouldCancelPendingLeaveRequest() {
            // Arrange
            LeaveRequest cancelledRequest = new LeaveRequest();
            cancelledRequest.setId(1L);
            cancelledRequest.setEmployee(testEmployee);
            cancelledRequest.setStatus(LeaveStatus.CANCELLED);
            cancelledRequest.setLeaveType(LeaveType.ANNUAL);
            cancelledRequest.setNumberOfDays(5);

            when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(testLeaveRequest));
            when(leaveRequestRepository.save(any()))
                .thenReturn(cancelledRequest);

            // Act
            LeaveResponseDTO result = leaveService.cancelLeaveRequest(1L, 1L);

            // Assert
            assertThat(result.getStatus()).isEqualTo(LeaveStatus.CANCELLED);
            // Balance should NOT be restored for PENDING requests
            verify(leaveBalanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should cancel approved leave and restore balance")
        void shouldCancelApprovedLeaveAndRestoreBalance() {
            // Arrange - already approved request
            testLeaveRequest.setStatus(LeaveStatus.APPROVED);
            testBalance.setUsedDays(5);
            testBalance.setAvailableDays(16);

            LeaveRequest cancelledRequest = new LeaveRequest();
            cancelledRequest.setId(1L);
            cancelledRequest.setEmployee(testEmployee);
            cancelledRequest.setStatus(LeaveStatus.CANCELLED);
            cancelledRequest.setLeaveType(LeaveType.ANNUAL);
            cancelledRequest.setNumberOfDays(5);

            when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(testLeaveRequest));
            when(leaveBalanceRepository.findByEmployeeIdAndLeaveType(
                1L, LeaveType.ANNUAL))
                .thenReturn(Optional.of(testBalance));
            when(leaveRequestRepository.save(any()))
                .thenReturn(cancelledRequest);

            // Act
            leaveService.cancelLeaveRequest(1L, 1L);

            // Assert - Balance should be restored
            assertThat(testBalance.getUsedDays()).isEqualTo(0);
            assertThat(testBalance.getAvailableDays()).isEqualTo(21);
            verify(leaveBalanceRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("Should throw exception when cancelling another employee's request")
        void shouldThrowExceptionForUnauthorizedCancellation() {
            // Arrange
            when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(testLeaveRequest));

            // Act & Assert - Employee ID 2 trying to cancel employee ID 1's request
            assertThatThrownBy(() -> leaveService.cancelLeaveRequest(1L, 2L))
                .isInstanceOf(InvalidLeaveRequestException.class)
                .hasMessageContaining("not authorized to cancel");
        }

        @Test
        @DisplayName("Should throw exception when cancelling rejected request")
        void shouldThrowExceptionWhenCancellingRejectedRequest() {
            // Arrange
            testLeaveRequest.setStatus(LeaveStatus.REJECTED);

            when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(testLeaveRequest));

            // Act & Assert
            assertThatThrownBy(() -> leaveService.cancelLeaveRequest(1L, 1L))
                .isInstanceOf(InvalidLeaveRequestException.class)
                .hasMessageContaining("Cannot cancel a rejected leave request");
        }

        @Test
        @DisplayName("Should throw exception when cancelling already cancelled request")
        void shouldThrowExceptionWhenCancellingAlreadyCancelledRequest() {
            // Arrange
            testLeaveRequest.setStatus(LeaveStatus.CANCELLED);

            when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(testLeaveRequest));

            // Act & Assert
            assertThatThrownBy(() -> leaveService.cancelLeaveRequest(1L, 1L))
                .isInstanceOf(InvalidLeaveRequestException.class)
                .hasMessageContaining("already cancelled");
        }
    }

    // ==========================================================
    // TEST GROUP 4: LEAVE BALANCE
    // ==========================================================

    @Nested
    @DisplayName("Leave Balance Tests")
    class LeaveBalanceTests {

        @Test
        @DisplayName("Should return leave balance for employee")
        void shouldReturnLeaveBalance() {
            // Arrange
            when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(testEmployee));
            when(leaveBalanceRepository.findByEmployeeId(1L))
                .thenReturn(List.of(testBalance));

            // Act
            var result = leaveService.getLeaveBalance(1L);

            // Assert
            assertThat(result).isNotEmpty();
            assertThat(result.get(0).getAvailableDays()).isEqualTo(21);
            assertThat(result.get(0).getLeaveType()).isEqualTo(LeaveType.ANNUAL);
        }

        @Test
        @DisplayName("Should restore balance correctly on cancellation")
        void shouldRestoreBalanceCorrectly() {
            // Arrange
            testBalance.setUsedDays(10);
            testBalance.setAvailableDays(11);

            // Act
            testBalance.restoreDays(5);

            // Assert
            assertThat(testBalance.getUsedDays()).isEqualTo(5);
            assertThat(testBalance.getAvailableDays()).isEqualTo(16);
        }

        @Test
        @DisplayName("Should not go below zero on balance deduction")
        void shouldNotGoBelowZeroOnDeduction() {
            // Arrange
            testBalance.setUsedDays(19);
            testBalance.setAvailableDays(2);

            // Act
            testBalance.restoreDays(100); // Trying to restore more than used

            // Assert - Should be capped at total days
            assertThat(testBalance.getAvailableDays()).isLessThanOrEqualTo(21);
            assertThat(testBalance.getUsedDays()).isGreaterThanOrEqualTo(0);
        }
    }

    // ==========================================================
    // TEST GROUP 5: QUERY TESTS
    // ==========================================================

    @Nested
    @DisplayName("Query Tests")
    class QueryTests {

        @Test
        @DisplayName("Should return all pending requests")
        void shouldReturnAllPendingRequests() {
            // Arrange
            when(leaveRequestRepository.findByStatus(LeaveStatus.PENDING))
                .thenReturn(List.of(testLeaveRequest));

            // Act
            var result = leaveService.getAllPendingRequests();

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(LeaveStatus.PENDING);
        }

        @Test
        @DisplayName("Should return empty list when no pending requests")
        void shouldReturnEmptyListWhenNoPendingRequests() {
            // Arrange
            when(leaveRequestRepository.findByStatus(LeaveStatus.PENDING))
                .thenReturn(Collections.emptyList());

            // Act
            var result = leaveService.getAllPendingRequests();

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should get leave request by ID")
        void shouldGetLeaveRequestById() {
            // Arrange
            when(leaveRequestRepository.findById(1L))
                .thenReturn(Optional.of(testLeaveRequest));

            // Act
            LeaveResponseDTO result = leaveService.getLeaveRequestById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw exception when leave request not found")
        void shouldThrowExceptionWhenLeaveRequestNotFound() {
            // Arrange
            when(leaveRequestRepository.findById(999L))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> leaveService.getLeaveRequestById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}