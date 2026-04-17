// src/main/java/com/leavemgmt/exception/InsufficientLeaveException.java
package com.leavemgmt.exception;

public class InsufficientLeaveException extends RuntimeException {

    private final int requestedDays;
    private final int availableDays;

    public InsufficientLeaveException(int requestedDays, int availableDays) {
        super(String.format(
            "Insufficient leave balance. Requested: %d days, Available: %d days",
            requestedDays, availableDays));
        this.requestedDays = requestedDays;
        this.availableDays = availableDays;
    }

    public int getRequestedDays() { return requestedDays; }
    public int getAvailableDays() { return availableDays; }
}