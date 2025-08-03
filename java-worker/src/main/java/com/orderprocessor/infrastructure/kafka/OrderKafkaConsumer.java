package com.orderprocessor.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderprocessor.application.dtos.OrderMessage;
import com.orderprocessor.application.usecases.ProcessOrderUseCase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Kafka consumer for order messages
 */
@Component
public class OrderKafkaConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderKafkaConsumer.class);

    private final ProcessOrderUseCase processOrderUseCase;
    private final ObjectMapper objectMapper;
    private final FailedMessageHandler failedMessageHandler;

    @Value("${app.kafka.consumer.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${app.kafka.consumer.retry.backoff-seconds:5}")
    private int backoffSeconds;

    public OrderKafkaConsumer(ProcessOrderUseCase processOrderUseCase,
                             @Qualifier("kafkaObjectMapper") ObjectMapper objectMapper,
                             FailedMessageHandler failedMessageHandler) {
        this.processOrderUseCase = processOrderUseCase;
        this.objectMapper = objectMapper;
        this.failedMessageHandler = failedMessageHandler;
    }

    /**
     * Kafka listener for order messages
     */
    @KafkaListener(
            topics = "${app.kafka.topic.orders}",
            groupId = "${app.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderMessage(ConsumerRecord<String, String> record,
                                  Acknowledgment acknowledgment,
                                  @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                  @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                  @Header(KafkaHeaders.OFFSET) long offset) {

        String message = record.value();
        String key = record.key();

        logger.info("Received message from topic: {}, partition: {}, offset: {}, key: {}", 
                topic, partition, offset, key);

        processMessage(message, key, record)
                .doOnSuccess(result -> {
                    logger.info("Message processed successfully: {}", key);
                    acknowledgment.acknowledge();
                })
                .doOnError(error -> {
                    logger.error("Error processing message {}: {}", key, error.getMessage());
                    handleFailedMessage(message, key, error);
                    acknowledgment.acknowledge(); // Acknowledge to avoid infinite retry
                })
                .subscribe();
    }

    private Mono<Void> processMessage(String message, String key, ConsumerRecord<String, String> record) {
        return Mono.fromCallable(() -> deserializeMessage(message))
                .flatMap(orderMessage -> processOrderUseCase.processOrder(orderMessage))
                .retryWhen(Retry.backoff(maxRetryAttempts, Duration.ofSeconds(backoffSeconds))
                        .filter(throwable -> !(throwable instanceof IllegalArgumentException)))
                .then();
    }

    private OrderMessage deserializeMessage(String message) throws JsonProcessingException {
        logger.info("Starting manual deserialization for message: {}", message.substring(0, Math.min(100, message.length())));
        try {
            // Manual deserialization to avoid typing issues
            if (message == null || message.trim().isEmpty()) {
                throw new JsonProcessingException("Message is null or empty") {};
            }
            
            // Simple JSON parsing without Jackson typing
            String orderId = extractField(message, "orderId");
            String customerId = extractField(message, "customerId");
            String productIdsStr = extractField(message, "productIds");
            String timestampStr = extractField(message, "timestamp");
            
            // Parse product IDs
            List<String> productIds = parseProductIds(productIdsStr);
            
            // Create OrderMessage
            OrderMessage orderMessage = new OrderMessage(orderId, customerId, productIds);
            
            // Parse timestamp if present
            if (timestampStr != null && !timestampStr.equals("null")) {
                try {
                    LocalDateTime timestamp = LocalDateTime.parse(timestampStr.replace("\"", ""));
                    orderMessage.setTimestamp(timestamp);
                } catch (Exception e) {
                    logger.warn("Could not parse timestamp: {}", timestampStr);
                }
            }
            
            return orderMessage;
        } catch (Exception e) {
            logger.error("Error deserializing message: {}", e.getMessage());
            throw new JsonProcessingException("Failed to deserialize message", e) {};
        }
    }
    
    private String extractField(String json, String fieldName) {
        // For productIds, we need to extract the array, not just a string
        if ("productIds".equals(fieldName)) {
            String pattern = "\"" + fieldName + "\"\\s*:\\s*\\[([^\\]]*)\\]";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return "[" + m.group(1) + "]";
            }
        } else {
            // For other fields, extract string values
            String pattern = "\"" + fieldName + "\"\\s*:\\s*\"([^\"]*)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        }
        return null;
    }
    
    private List<String> parseProductIds(String productIdsStr) {
        List<String> productIds = new ArrayList<>();
        if (productIdsStr == null || productIdsStr.equals("null")) {
            return productIds;
        }
        
        logger.info("Parsing product IDs from: {}", productIdsStr);
        
        // Remove outer brackets and split by comma
        String cleanStr = productIdsStr.trim();
        if (cleanStr.startsWith("[") && cleanStr.endsWith("]")) {
            cleanStr = cleanStr.substring(1, cleanStr.length() - 1);
        }
        
        // Split by comma and clean each product ID
        String[] parts = cleanStr.split(",");
        for (String part : parts) {
            String cleanPart = part.trim().replace("\"", "");
            if (!cleanPart.isEmpty()) {
                productIds.add(cleanPart);
                logger.info("Added product ID: {}", cleanPart);
            }
        }
        
        logger.info("Parsed {} product IDs: {}", productIds.size(), productIds);
        return productIds;
    }

    private void handleFailedMessage(String message, String key, Throwable error) {
        failedMessageHandler.handleFailedMessage(key, message, error)
                .doOnSuccess(v -> logger.info("Failed message handled for key: {}", key))
                .doOnError(handlerError -> logger.error("Error handling failed message for key {}: {}", 
                        key, handlerError.getMessage()))
                .subscribe();
    }
} 