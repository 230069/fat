// src/test/java/com/leavemgmt/LeaveManagementIntegrationTest.java
package com.leavemgmt;

import com.leavemgmt.dto.*;
import com.leavemgmt.model.*;
import com.leavemgmt.repository.*;
import com.leavemgmt.service.LeaveService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Leave Management Integration Tests")
class LeaveManagementIntegrationTest {

    @Autowired private LeaveService leaveService;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private LeaveBalanceRepository leaveBalanceRepository;
    @Autowired private LeaveRequestRepository leaveRequestRepository;

    private static Long employeeId;

    @BeforeAll
    static void setUpClass() {
        System.out.println("Starting Leave Management Integration Tests...");
    }

    @Test
    @Order(1)
    @DisplayName("Should seed data correctly")
    void shouldSeedDataCorrectly() {
        List<Employee> employees = employeeRepository.findAll();
        assertThat(employees).isNotEmpty();
        employeeId = employees.get(0).getId();
        System.out.println("Using employee ID: " + employeeId);
    }

    @Test
    @Order(2)
    @DisplayName("Should have leave balances initialized")
    void shouldHaveLeaveBalancesInitialized() {
        List<LeaveBalance> balances = leaveBalanceRepository.findByEmployeeId(employeeId);
        assertThat(balances).isNotEmpty();
        assertThat(balances).hasSize(LeaveType.values().length);
    }

    @Test
    @Order(3)
    @DisplayName("Full leave lifecycle - submit, approve, verify balance")
    @Transactional
    void fullLeaveLifecycle() {
        // Step 1: Submit leave
        LeaveRequestDTO requestDTO = new LeaveRequestDTO();
        requestDTO.setEmployeeId(employeeId);
        requestDTO.setLeaveType(LeaveType.ANNUAL);
        requestDTO.setStartDate(LocalDate.now().plusDays(30));
        requestDTO.setEndDate(LocalDate.now().plusDays(34));
        requestDTO.setReason("Integration Test Vacation");

        LeaveResponseDTO submitted = leaveService.submitLeaveRequest(requestDTO);
        assertThat(submitted.getStatus()).isEqualTo(LeaveStatus.PENDING);

        // Step 2: Approve leave
        LeaveApprovalDTO approvalDTO = new LeaveApprovalDTO();
        approvalDTO.setLeaveRequestId(submitted.getId());
        approvalDTO.setStatus(LeaveStatus.APPROVED);
        approvalDTO.setApprovedBy("Integration Test Manager");
        approvalDTO.setManagerComments("Approved for integration test");

        LeaveResponseDTO approved = leaveService.processLeaveRequest(approvalDTO);
        assertThat(approved.getStatus()).isEqualTo(LeaveStatus.APPROVED);

        // Step 3: Verify balance was deducted
        List<LeaveBalanceDTO> balances = leaveService.getLeaveBalance(employeeId);
        LeaveBalanceDTO annualBalance = balances.stream()
            .filter(b -> b.getLeaveType() == LeaveType.ANNUAL)
            .findFirst()
            .orElseThrow();

        assertThat(annualBalance.getUsedDays()).isEqualTo(5);
        assertThat(annualBalance.getAvailableDays()).isEqualTo(16);
    }
}