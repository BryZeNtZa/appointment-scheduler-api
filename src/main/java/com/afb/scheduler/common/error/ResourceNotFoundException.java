package com.afb.scheduler.common.error;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String resource, String reference) {
        return new ResourceNotFoundException("%s not found: %s".formatted(resource, reference));
    }
}
