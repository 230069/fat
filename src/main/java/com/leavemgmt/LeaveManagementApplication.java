// src/main/java/com/leavemgmt/LeaveManagementApplication.java
package com.leavemgmt;

import com.leavemgmt.model.Employee;
import com.leavemgmt.model.LeaveBalance;
import com.leavemgmt.model.LeaveType;
import com.leavemgmt.repository.EmployeeRepository;
import com.leavemgmt.repository.LeaveBalanceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class LeaveManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeaveManagementApplication.class, args);
    }

    /**
     * Seed initial data on application startup
     */
    @Bean
    public CommandLineRunner seedData(
            EmployeeRepository employeeRepo,
            LeaveBalanceRepository leaveBalanceRepo) {

        return args -> {
            // Create sample employees
            Employee emp1 = new Employee();
            emp1.setName("Alice Johnson");
            emp1.setEmail("alice@company.com");
            emp1.setDepartment("Engineering");
            emp1.setRole("Developer");
            employeeRepo.save(emp1);

            Employee emp2 = new Employee();
            emp2.setName("Bob Smith");
            emp2.setEmail("bob@company.com");
            emp2.setDepartment("HR");
            emp2.setRole("Manager");
            employeeRepo.save(emp2);

            Employee emp3 = new Employee();
            emp3.setName("Carol White");
            emp3.setEmail("carol@company.com");
            emp3.setDepartment("Finance");
            emp3.setRole("Analyst");
            employeeRepo.save(emp3);

            // Assign leave balances to employees
            createLeaveBalances(leaveBalanceRepo, emp1);
            createLeaveBalances(leaveBalanceRepo, emp2);
            createLeaveBalances(leaveBalanceRepo, emp3);

            System.out.println("====================================");
            System.out.println(" Leave Management System Started!  ");
            System.out.println("====================================");
        };
    }

    private void createLeaveBalances(
            LeaveBalanceRepository repo, Employee employee) {

        for (LeaveType type : LeaveType.values()) {
            LeaveBalance balance = new LeaveBalance();
            balance.setEmployee(employee);
            balance.setLeaveType(type);
            balance.setTotalDays(type.getDefaultDays());
            balance.setUsedDays(0);
            balance.setAvailableDays(type.getDefaultDays());
            repo.save(balance);
        }
    }
}