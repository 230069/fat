// src/test/java/com/leavemgmt/controller/LeaveControllerTest.java
package com.leavemgmt.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.leavemgmt.dto.*;
import com.leavemgmt.exception.*;
import com.leavemgmt.model.*;
import com.leavemgmt.service.LeaveService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LeaveController.class)
@DisplayName("Leave Controller Integration Tests")
class LeaveControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private LeaveService leaveService;

    private ObjectMapper objectMapper;
    private LeaveResponseDTO sampleResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleResponse = new LeaveResponseDTO();
        sampleResponse.setId(1L);
        sampleResponse.setEmployeeId(1L);
        sampleResponse.setEmployeeName("Alice Johnson");
        sampleResponse.setLeaveType(LeaveType.ANNUAL);
        sampleResponse.setStartDate(LocalDate.now().plusDays(5));
        sampleResponse.setEndDate(LocalDate.now().plusDays(9));
        sampleResponse.setNumberOfDays(5);
        sampleResponse.setStatus(LeaveStatus.PENDING);
        sampleResponse.setAppliedOn(LocalDateTime.now());
    }

    @Test
    @DisplayName("POST /api/leaves/apply - Should create leave request")
    void shouldCreateLeaveRequest() throws Exception {
        // Arrange
        LeaveRequestDTO requestDTO = new LeaveRequestDTO();
        requestDTO.setEmployeeId(1L);
        requestDTO.setLeaveType(LeaveType.ANNUAL);
        requestDTO.setStartDate(LocalDate.now().plusDays(5));
        requestDTO.setEndDate(LocalDate.now().plusDays(9));
        requestDTO.setReason("Vacation");

        when(leaveService.submitLeaveRequest(any())).thenReturn(sampleResponse);

        // Act & Assert
        mockMvc.perform(post("/api/leaves/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.employeeName").value("Alice Johnson"));
    }

    @Test
    @DisplayName("PUT /api/leaves/process - Should approve leave request")
    void shouldApproveLeaveRequest() throws Exception {
        // Arrange
        sampleResponse.setStatus(LeaveStatus.APPROVED);
        sampleResponse.setApprovedBy("Manager Bob");

        LeaveApprovalDTO approvalDTO = new LeaveApprovalDTO();
        approvalDTO.setLeaveRequestId(1L);
        approvalDTO.setStatus(LeaveStatus.APPROVED);
        approvalDTO.setApprovedBy("Manager Bob");

        when(leaveService.processLeaveRequest(any())).thenReturn(sampleResponse);

        // Act & Assert
        mockMvc.perform(put("/api/leaves/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approvalDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.approvedBy").value("Manager Bob"));
    }

    @Test
    @DisplayName("GET /api/leaves/pending - Should return pending requests")
    void shouldReturnPendingRequests() throws Exception {
        // Arrange
        when(leaveService.getAllPendingRequests()).thenReturn(List.of(sampleResponse));

        // Act & Assert
        mockMvc.perform(get("/api/leaves/pending"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/leaves/{id} - Should return leave request by ID")
    void shouldReturnLeaveById() throws Exception {
        // Arrange
        when(leaveService.getLeaveRequestById(1L)).thenReturn(sampleResponse);

        // Act & Assert
        mockMvc.perform(get("/api/leaves/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.leaveType").value("ANNUAL"));
    }

    @Test
    @DisplayName("GET /api/leaves/{id} - Should return 404 when not found")
    void shouldReturn404WhenNotFound() throws Exception {
        // Arrange
        when(leaveService.getLeaveRequestById(999L))
            .thenThrow(new ResourceNotFoundException("LeaveRequest", 999L));

        // Act & Assert
        mockMvc.perform(get("/api/leaves/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/leaves/apply - Should return 409 on conflict")
    void shouldReturn409OnConflict() throws Exception {
        // Arrange
        LeaveRequestDTO requestDTO = new LeaveRequestDTO();
        requestDTO.setEmployeeId(1L);
        requestDTO.setLeaveType(LeaveType.ANNUAL);
        requestDTO.setStartDate(LocalDate.now().plusDays(5));
        requestDTO.setEndDate(LocalDate.now().plusDays(9));

        when(leaveService.submitLeaveRequest(any()))
            .thenThrow(new LeaveConflictException("Conflict with existing leave"));

        // Act & Assert
        mockMvc.perform(post("/api/leaves/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("LEAVE_CONFLICT"));
    }

    @Test
    @DisplayName("POST /api/leaves/apply - Should return 400 on insufficient balance")
    void shouldReturn400OnInsufficientBalance() throws Exception {
        // Arrange
        LeaveRequestDTO requestDTO = new LeaveRequestDTO();
        requestDTO.setEmployeeId(1L);
        requestDTO.setLeaveType(LeaveType.ANNUAL);
        requestDTO.setStartDate(LocalDate.now().plusDays(5));
        requestDTO.setEndDate(LocalDate.now().plusDays(30));

        when(leaveService.submitLeaveRequest(any()))
            .thenThrow(new InsufficientLeaveException(25, 2));

        // Act & Assert
        mockMvc.perform(post("/api/leaves/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_LEAVE"));
    }

    @Test
    @DisplayName("GET /api/leaves/balance/{id} - Should return leave balance")
    void shouldReturnLeaveBalance() throws Exception {
        // Arrange
        LeaveBalanceDTO balanceDTO = new LeaveBalanceDTO();
        balanceDTO.setEmployeeId(1L);
        balanceDTO.setLeaveType(LeaveType.ANNUAL);
        balanceDTO.setTotalDays(21);
        balanceDTO.setUsedDays(5);
        balanceDTO.setAvailableDays(16);

        when(leaveService.getLeaveBalance(1L)).thenReturn(List.of(balanceDTO));

        // Act & Assert
        mockMvc.perform(get("/api/leaves/balance/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].availableDays").value(16))
            .andExpect(jsonPath("$[0].usedDays").value(5));
    }
}