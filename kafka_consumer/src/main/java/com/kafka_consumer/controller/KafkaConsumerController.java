package com.kafka_consumer.controller;

import com.kafka_consumer.model.Message;
import com.kafka_consumer.service.MessageProcessingService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for retrieving information about processed Kafka messages.
 */
@RestController
@RequestMapping("/api/messages")
public class KafkaConsumerController {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerController.class);
    private final MessageProcessingService messageProcessingService;

    public KafkaConsumerController(MessageProcessingService messageProcessingService) {
        this.messageProcessingService = messageProcessingService;
    }

    /**
     * Gets a processed message by ID.
     *
     * @param id the ID of the message to retrieve
     * @return the message, or a 404 if not found
     */
    @GetMapping("/{id}")
    @Timed(value = "api.message.get", description = "Time taken to retrieve a message")
    public ResponseEntity<Message> getMessage(@PathVariable String id) {
        log.info("Received request to get message with id: {}", id);
        
        Message message = messageProcessingService.getProcessedMessage(id);
        
        if (message == null) {
            log.warn("Message with id: {} not found", id);
            return ResponseEntity.notFound().build();
        }
        
        log.info("Retrieved message with id: {}", id);
        return ResponseEntity.ok(message);
    }

    /**
     * Gets statistics about processed messages.
     *
     * @return statistics about processed messages
     */
    @GetMapping("/stats")
    @Timed(value = "api.message.stats", description = "Time taken to retrieve message statistics")
    public ResponseEntity<Map<String, Object>> getStats() {
        log.info("Received request to get message statistics");
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("processedCount", messageProcessingService.getProcessedMessageCount());
        stats.put("timestamp", LocalDateTime.now());
        
        log.info("Retrieved message statistics: {}", stats);
        return ResponseEntity.ok(stats);
    }

    /**
     * Health check endpoint.
     *
     * @return a simple health check response
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(health);
    }

    /**
     * Error handler for exceptions.
     *
     * @param e the exception
     * @return an error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("Error handling request", e);
        
        Map<String, Object> error = new HashMap<>();
        error.put("status", "error");
        error.put("message", e.getMessage());
        error.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}