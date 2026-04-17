// src/main/java/com/leavemgmt/model/LeaveType.java
package com.leavemgmt.model;

public enum LeaveType {
    ANNUAL(21, "Annual Leave"),
    SICK(10, "Sick Leave"),
    MATERNITY(90, "Maternity Leave"),
    PATERNITY(14, "Paternity Leave"),
    EMERGENCY(5, "Emergency Leave"),
    UNPAID(0, "Unpaid Leave");

    private final int defaultDays;
    private final String description;

    LeaveType(int defaultDays, String description) {
        this.defaultDays = defaultDays;
        this.description = description;
    }

    public int getDefaultDays() { return defaultDays; }
    public String getDescription() { return description; }
}