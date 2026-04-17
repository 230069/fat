// src/main/java/com/leavemgmt/exception/LeaveConflictException.java
package com.leavemgmt.exception;

public class LeaveConflictException extends RuntimeException {

    public LeaveConflictException(String message) {
        super(message);
    }
}