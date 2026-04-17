// src/main/java/com/leavemgmt/repository/LeaveBalanceRepository.java
package com.leavemgmt.repository;

import com.leavemgmt.model.LeaveBalance;
import com.leavemgmt.model.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    Optional<LeaveBalance> findByEmployeeIdAndLeaveType(
        Long employeeId, LeaveType leaveType);

    List<LeaveBalance> findByEmployeeId(Long employeeId);
}