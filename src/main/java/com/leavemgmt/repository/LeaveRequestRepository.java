// src/main/java/com/leavemgmt/repository/LeaveRequestRepository.java
package com.leavemgmt.repository;

import com.leavemgmt.model.LeaveRequest;
import com.leavemgmt.model.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployeeId(Long employeeId);

    List<LeaveRequest> findByEmployeeIdAndStatus(Long employeeId, LeaveStatus status);

    List<LeaveRequest> findByStatus(LeaveStatus status);

    /**
     * Check for conflicting leave requests (overlapping dates)
     */
    @Query("""
        SELECT lr FROM LeaveRequest lr
        WHERE lr.employee.id = :employeeId
        AND lr.status IN ('PENDING', 'APPROVED')
        AND lr.id != :excludeId
        AND (lr.startDate <= :endDate AND lr.endDate >= :startDate)
        """)
    List<LeaveRequest> findConflictingLeaves(
        @Param("employeeId") Long employeeId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("excludeId") Long excludeId
    );

    /**
     * Get all leave requests for an employee within date range
     */
    @Query("""
        SELECT lr FROM LeaveRequest lr
        WHERE lr.employee.id = :employeeId
        AND lr.startDate >= :startDate
        AND lr.endDate <= :endDate
        ORDER BY lr.startDate ASC
        """)
    List<LeaveRequest> findByEmployeeAndDateRange(
        @Param("employeeId") Long employeeId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}