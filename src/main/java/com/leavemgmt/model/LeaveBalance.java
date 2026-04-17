// src/main/java/com/leavemgmt/model/LeaveBalance.java
package com.leavemgmt.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "leave_balances",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"employee_id", "leave_type"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false)
    private LeaveType leaveType;

    @Column(name = "total_days")
    private int totalDays;

    @Column(name = "used_days")
    private int usedDays;

    @Column(name = "available_days")
    private int availableDays;

    /**
     * Deduct days from available balance
     */
    public void deductDays(int days) {
        this.usedDays += days;
        this.availableDays -= days;
    }

    /**
     * Restore days to available balance (for cancellation/rejection)
     */
    public void restoreDays(int days) {
        this.usedDays = Math.max(0, this.usedDays - days);
        this.availableDays = Math.min(this.totalDays, this.availableDays + days);
    }
}