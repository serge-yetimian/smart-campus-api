package com.smartcampus.exception;

public class LinkedResourceNotFoundException extends RuntimeException {
    private String resourceId;

    public LinkedResourceNotFoundException(String resourceId) {
        super("Linked resource not found: " + resourceId);
        this.resourceId = resourceId;
    }

    public String getResourceId() {
        return resourceId;
    }
}