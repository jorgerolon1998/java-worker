package com.orderprocessor.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Customer details enriched from external API
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerDetails {

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotBlank(message = "Customer name is required")
    private String name;

    @Email(message = "Email must be valid")
    private String email;

    @NotNull(message = "Customer status is required")
    private CustomerStatus status;

    @NotNull(message = "Credit limit is required")
    @Positive(message = "Credit limit must be positive")
    private BigDecimal creditLimit;

    private BigDecimal currentBalance;

    // Constructors
    public CustomerDetails() {}

    public CustomerDetails(String customerId, String name, String email, CustomerStatus status, BigDecimal creditLimit) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.status = status;
        this.creditLimit = creditLimit;
    }

    // Business logic
    public boolean isActive() {
        return CustomerStatus.ACTIVE.equals(this.status);
    }

    public boolean hasAvailableCredit(BigDecimal amount) {
        if (currentBalance == null) {
            currentBalance = BigDecimal.ZERO;
        }
        return creditLimit.subtract(currentBalance).compareTo(amount) >= 0;
    }

    // Getters and Setters
    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public void setStatus(CustomerStatus status) {
        this.status = status;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    @Override
    public String toString() {
        return "CustomerDetails{" +
                "customerId='" + customerId + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", status=" + status +
                ", creditLimit=" + creditLimit +
                ", currentBalance=" + currentBalance +
                '}';
    }
} 