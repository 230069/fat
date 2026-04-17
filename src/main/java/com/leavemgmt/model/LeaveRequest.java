// src/main/java/com/leavemgmt/model/LeaveRequest.java
package com.leavemgmt.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "leave_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false)
    @NotNull(message = "Leave type is required")
    private LeaveType leaveType;

    @Column(name = "start_date", nullable = false)
    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @Column(name = "number_of_days")
    private int numberOfDays;

    @Column(name = "reason", length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private LeaveStatus status;

    @Column(name = "manager_comments", length = 500)
    private String managerComments;

    @Column(name = "applied_on")
    private LocalDateTime appliedOn;

    @Column(name = "processed_on")
    private LocalDateTime processedOn;

    @Column(name = "approved_by")
    private String approvedBy;

    /**
     * Calculate number of days before persisting
     */
    @PrePersist
    protected void onCreate() {
        this.status = LeaveStatus.PENDING;
        this.appliedOn = LocalDateTime.now();
        calculateDays();
    }

    @PreUpdate
    protected void onUpdate() {
        calculateDays();
    }

    private void calculateDays() {
        if (startDate != null && endDate != null) {
            this.numberOfDays = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        }
    }
}