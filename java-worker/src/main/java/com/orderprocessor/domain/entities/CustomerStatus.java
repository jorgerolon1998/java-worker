package com.orderprocessor.domain.entities;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Customer status enumeration
 */
public enum CustomerStatus {
    ACTIVE("active"),
    INACTIVE("inactive"),
    SUSPENDED("suspended"),
    BLOCKED("blocked");

    private final String value;

    CustomerStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static CustomerStatus fromString(String text) {
        for (CustomerStatus status : CustomerStatus.values()) {
            if (status.value.equalsIgnoreCase(text)) {
                return status;
            }
        }
        throw new IllegalArgumentException("No constant with text " + text + " found");
    }

    @JsonValue
    @Override
    public String toString() {
        return value;
    }
} 