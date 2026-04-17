// src/main/java/com/leavemgmt/exception/ResourceNotFoundException.java
package com.leavemgmt.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Long id) {
        super(String.format("%s not found with id: %d", resource, id));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}