package com.orderprocessor.domain.entities;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTest {

    @Test
    void testOrderStatusValues() {
        assertEquals("pending", OrderStatus.PENDING.getValue());
        assertEquals("processing", OrderStatus.PROCESSING.getValue());
        assertEquals("completed", OrderStatus.COMPLETED.getValue());
        assertEquals("failed", OrderStatus.FAILED.getValue());
    }

    @Test
    void testFromString() {
        assertEquals(OrderStatus.PENDING, OrderStatus.fromString("pending"));
        assertEquals(OrderStatus.PROCESSING, OrderStatus.fromString("processing"));
        assertEquals(OrderStatus.COMPLETED, OrderStatus.fromString("completed"));
        assertEquals(OrderStatus.FAILED, OrderStatus.fromString("failed"));
    }

    @Test
    void testFromStringCaseInsensitive() {
        assertEquals(OrderStatus.PENDING, OrderStatus.fromString("PENDING"));
        assertEquals(OrderStatus.COMPLETED, OrderStatus.fromString("COMPLETED"));
    }

    @Test
    void testFromStringInvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> {
            OrderStatus.fromString("invalid");
        });
    }

    @Test
    void testToString() {
        assertEquals("pending", OrderStatus.PENDING.toString());
        assertEquals("processing", OrderStatus.PROCESSING.toString());
        assertEquals("completed", OrderStatus.COMPLETED.toString());
        assertEquals("failed", OrderStatus.FAILED.toString());
    }
} 