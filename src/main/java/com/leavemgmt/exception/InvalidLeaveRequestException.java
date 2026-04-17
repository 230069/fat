// src/main/java/com/leavemgmt/exception/InvalidLeaveRequestException.java
package com.leavemgmt.exception;

public class InvalidLeaveRequestException extends RuntimeException {

    public InvalidLeaveRequestException(String message) {
        super(message);
    }
}