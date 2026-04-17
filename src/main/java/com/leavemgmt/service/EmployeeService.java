// src/main/java/com/leavemgmt/service/EmployeeService.java
package com.leavemgmt.service;

import com.leavemgmt.exception.ResourceNotFoundException;
import com.leavemgmt.model.Employee;
import com.leavemgmt.model.LeaveBalance;
import com.leavemgmt.model.LeaveType;
import com.leavemgmt.repository.EmployeeRepository;
import com.leavemgmt.repository.LeaveBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;

    public Employee createEmployee(Employee employee) {
        if (employeeRepository.existsByEmail(employee.getEmail())) {
            throw new RuntimeException(
                "Employee with email already exists: " + employee.getEmail());
        }

        Employee saved = employeeRepository.save(employee);

        // Initialize leave balances for all leave types
        for (LeaveType type : LeaveType.values()) {
            LeaveBalance balance = new LeaveBalance();
            balance.setEmployee(saved);
            balance.setLeaveType(type);
            balance.setTotalDays(type.getDefaultDays());
            balance.setUsedDays(0);
            balance.setAvailableDays(type.getDefaultDays());
            leaveBalanceRepository.save(balance);
        }

        log.info("Created employee: {} with ID: {}", saved.getName(), saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
    }

    public Employee updateEmployee(Long id, Employee updatedEmployee) {
        Employee existing = getEmployeeById(id);
        existing.setName(updatedEmployee.getName());
        existing.setDepartment(updatedEmployee.getDepartment());
        existing.setRole(updatedEmployee.getRole());
        return employeeRepository.save(existing);
    }

    public void deleteEmployee(Long id) {
        Employee employee = getEmployeeById(id);
        employeeRepository.delete(employee);
        log.info("Deleted employee with ID: {}", id);
    }
}