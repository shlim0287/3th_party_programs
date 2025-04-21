package com.kafka_producer.controller;

import com.kafka_producer.model.Message;
import com.kafka_producer.service.KafkaProducerService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * REST controller for sending messages to Kafka.
 * Provides endpoints for sending messages and checking the status of sent messages.
 */
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerController {

    private final KafkaProducerService kafkaProducerService;

    /**
     * Sends a message to Kafka.
     *
     * @param message the message to send
     * @return a response entity with the result of the operation
     */
    @PostMapping
    @Timed(value = "api.message.send", description = "Time taken to process message send request")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody Message message) {
        log.info("Received request to send message: {}", message);
        
        // Set default values if not provided
        if (message.getId() == null) {
            message.setId(UUID.randomUUID().toString());
        }
        
        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }
        
        if (message.getSource() == null) {
            message.setSource("api");
        }
        
        try {
            // Send the message and wait for the result (with timeout)
            var result = kafkaProducerService.sendMessage(message)
                    .get(10, TimeUnit.SECONDS);
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("messageId", message.getId());
            response.put("topic", result.getRecordMetadata().topic());
            response.put("partition", result.getRecordMetadata().partition());
            response.put("offset", result.getRecordMetadata().offset());
            response.put("timestamp", LocalDateTime.now());
            
            log.info("Message sent successfully: {}", response);
            return ResponseEntity.ok(response);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while sending message: {}", message, e);
            return createErrorResponse("Thread interrupted while sending message", HttpStatus.INTERNAL_SERVER_ERROR);
            
        } catch (ExecutionException e) {
            log.error("Error sending message: {}", message, e);
            return createErrorResponse("Failed to send message: " + e.getCause().getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            
        } catch (TimeoutException e) {
            log.error("Timeout while sending message: {}", message, e);
            return createErrorResponse("Timeout while sending message", HttpStatus.REQUEST_TIMEOUT);
        }
    }

    /**
     * Sends a simple text message to Kafka.
     *
     * @param content the content of the message
     * @return a response entity with the result of the operation
     */
    @PostMapping("/simple")
    @Timed(value = "api.message.send.simple", description = "Time taken to process simple message send request")
    public ResponseEntity<Map<String, Object>> sendSimpleMessage(@RequestParam String content, 
                                                                @RequestParam(required = false) String type) {
        log.info("Received request to send simple message with content: {} and type: {}", content, type);
        
        // Create a new message
        Message message = Message.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .type(type != null ? type : "INFO")
                .timestamp(LocalDateTime.now())
                .source("api-simple")
                .build();
        
        try {
            // Send the message and wait for the result (with timeout)
            SendResult<String, Message> result = kafkaProducerService.sendMessage(message)
                    .get(10, TimeUnit.SECONDS);

            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("messageId", message.getId());
            response.put("topic", result.getRecordMetadata().topic());
            response.put("partition", result.getRecordMetadata().partition());
            response.put("offset", result.getRecordMetadata().offset());
            response.put("timestamp", LocalDateTime.now());
            
            log.info("Simple message sent successfully: {}", response);
            return ResponseEntity.ok(response);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while sending simple message: {}", message, e);
            return createErrorResponse("Thread interrupted while sending message", HttpStatus.INTERNAL_SERVER_ERROR);
            
        } catch (ExecutionException e) {
            log.error("Error sending simple message: {}", message, e);
            return createErrorResponse("Failed to send message: " + e.getCause().getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            
        } catch (TimeoutException e) {
            log.error("Timeout while sending simple message: {}", message, e);
            return createErrorResponse("Timeout while sending message", HttpStatus.REQUEST_TIMEOUT);
        }
    }

    /**
     * Creates an error response.
     *
     * @param message the error message
     * @param status the HTTP status
     * @return a response entity with the error details
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(status).body(response);
    }
}