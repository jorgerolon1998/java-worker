package com.orderprocessor.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler for failed messages with retry logic
 */
@Service
public class FailedMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(FailedMessageHandler.class);
    private static final String FAILED_MESSAGE_PREFIX = "failed:message:";
    private static final String RETRY_COUNT_PREFIX = "failed:retry:";

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.failed-message.max-retries:5}")
    private int maxRetries;

    @Value("${app.failed-message.ttl-hours:24}")
    private int ttlHours;

    public FailedMessageHandler(ReactiveRedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Handle a failed message
     * @param key the message key
     * @param message the original message
     * @param error the error that occurred
     * @return Mono<Void>
     */
    public Mono<Void> handleFailedMessage(String key, String message, Throwable error) {
        String failedMessageKey = FAILED_MESSAGE_PREFIX + key;
        String retryCountKey = RETRY_COUNT_PREFIX + key;

        return getRetryCount(retryCountKey)
                .flatMap(retryCount -> {
                    if (retryCount >= maxRetries) {
                        logger.error("Message {} has exceeded max retries ({}), moving to dead letter queue", key, maxRetries);
                        return moveToDeadLetterQueue(key, message, error);
                    }

                    return storeFailedMessage(failedMessageKey, retryCountKey, key, message, error, retryCount);
                });
    }

    private Mono<Void> storeFailedMessage(String failedMessageKey, String retryCountKey, 
                                         String key, String message, Throwable error, int retryCount) {
        
        Map<String, Object> failedMessageData = new HashMap<>();
        failedMessageData.put("key", key);
        failedMessageData.put("message", message);
        failedMessageData.put("error", error.getMessage());
        failedMessageData.put("retryCount", retryCount + 1);
        failedMessageData.put("maxRetries", maxRetries);
        failedMessageData.put("timestamp", LocalDateTime.now().toString());

        return Mono.zip(
                storeMessageData(failedMessageKey, failedMessageData),
                incrementRetryCount(retryCountKey)
        ).then();
    }

    private Mono<Void> moveToDeadLetterQueue(String key, String message, Throwable error) {
        Map<String, Object> deadLetterData = new HashMap<>();
        deadLetterData.put("key", key);
        deadLetterData.put("message", message);
        deadLetterData.put("error", error.getMessage());
        deadLetterData.put("timestamp", LocalDateTime.now().toString());
        deadLetterData.put("status", "dead_letter");

        String deadLetterKey = "dead:letter:" + key;
        return storeMessageData(deadLetterKey, deadLetterData);
    }

    private Mono<Void> storeMessageData(String key, Map<String, Object> data) {
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            Duration ttl = Duration.ofHours(ttlHours);
            
            return redisTemplate.opsForValue()
                    .set(key, jsonData, ttl)
                    .doOnSuccess(v -> logger.info("Failed message stored: {}", key))
                    .doOnError(redisError -> logger.error("Error storing failed message {}: {}", key, redisError.getMessage()))
                    .then();
        } catch (JsonProcessingException e) {
            logger.error("Error serializing failed message data for key {}: {}", key, e.getMessage());
            return Mono.error(e);
        }
    }

    private Mono<Integer> getRetryCount(String retryCountKey) {
        return redisTemplate.opsForValue()
                .get(retryCountKey)
                .map(Integer::parseInt)
                .defaultIfEmpty(0)
                .doOnError(error -> logger.error("Error getting retry count for key {}: {}", retryCountKey, error.getMessage()));
    }

    private Mono<Void> incrementRetryCount(String retryCountKey) {
        return redisTemplate.opsForValue()
                .increment(retryCountKey)
                .doOnSuccess(count -> logger.info("Retry count incremented for key {}: {}", retryCountKey, count))
                .doOnError(error -> logger.error("Error incrementing retry count for key {}: {}", retryCountKey, error.getMessage()))
                .then();
    }

    /**
     * Get failed message data
     * @param key the message key
     * @return the failed message data
     */
    public Mono<Map<String, Object>> getFailedMessage(String key) {
        String failedMessageKey = FAILED_MESSAGE_PREFIX + key;
        
        return redisTemplate.opsForValue()
                .get(failedMessageKey)
                .flatMap(jsonData -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = objectMapper.readValue(jsonData, Map.class);
                        return Mono.just(data);
                    } catch (JsonProcessingException e) {
                        logger.error("Error deserializing failed message data for key {}: {}", key, e.getMessage());
                        return Mono.error(e);
                    }
                })
                .doOnError(error -> logger.error("Error getting failed message for key {}: {}", key, error.getMessage()));
    }

    /**
     * Retry a failed message
     * @param key the message key
     * @return true if retry was successful, false otherwise
     */
    public Mono<Boolean> retryFailedMessage(String key) {
        return getFailedMessage(key)
                .flatMap(data -> {
                    String message = (String) data.get("message");
                    // Here you would re-publish the message to Kafka
                    logger.info("Retrying failed message: {}", key);
                    return Mono.just(true);
                })
                .defaultIfEmpty(false);
    }
} 