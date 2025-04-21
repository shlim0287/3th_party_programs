package com.kafka_consumer.service;

import com.kafka_consumer.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Service for processing messages received from Kafka.
 * In a real-world scenario, this would contain business logic for handling messages.
 */
@Service
public class MessageProcessingService {

    private final static Logger log = getLogger(MessageProcessingService.class);

    // In-memory store for processed messages (for demonstration purposes)
    private final ConcurrentMap<String, Message> processedMessages = new ConcurrentHashMap<>();
    
    /**
     * Processes a message received from Kafka.
     * This is where you would implement your business logic.
     *
     * @param message the message to process
     */
    public void processMessage(Message message) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        log.info("Processing message: {}", message);
        
        // Simulate processing based on message type
        switch (message.getType() != null ? message.getType().toUpperCase() : "INFO") {
            case "INFO":
                processInfoMessage(message);
                break;
            case "WARNING":
                processWarningMessage(message);
                break;
            case "ERROR":
                processErrorMessage(message);
                break;
            default:
                processDefaultMessage(message);
        }
        
        // Store the processed message
        processedMessages.put(message.getId(), message);
        
        log.info("Successfully processed message with id: {}", message.getId());
    }
    
    /**
     * Processes an INFO type message.
     *
     * @param message the message to process
     */
    private void processInfoMessage(Message message) {
        log.info("Processing INFO message: {}", message.getContent());
        // Simulate some processing time
        simulateProcessingTime(50);
    }
    
    /**
     * Processes a WARNING type message.
     *
     * @param message the message to process
     */
    private void processWarningMessage(Message message) {
        log.warn("Processing WARNING message: {}", message.getContent());
        // Simulate some processing time
        simulateProcessingTime(100);
    }
    
    /**
     * Processes an ERROR type message.
     *
     * @param message the message to process
     */
    private void processErrorMessage(Message message) {
        log.error("Processing ERROR message: {}", message.getContent());
        // Simulate some processing time
        simulateProcessingTime(200);
        
        // For demonstration purposes, we could throw an exception for some error messages
        // to show how error handling works
        if (message.getContent() != null && message.getContent().contains("FATAL")) {
            throw new RuntimeException("Simulated fatal error in message processing");
        }
    }
    
    /**
     * Processes a message with an unknown type.
     *
     * @param message the message to process
     */
    private void processDefaultMessage(Message message) {
        log.info("Processing message with unknown type: {}", message.getContent());
        // Simulate some processing time
        simulateProcessingTime(75);
    }
    
    /**
     * Simulates processing time by sleeping for the specified number of milliseconds.
     *
     * @param milliseconds the number of milliseconds to sleep
     */
    private void simulateProcessingTime(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Processing interrupted", e);
        }
    }
    
    /**
     * Gets a processed message by ID.
     *
     * @param id the ID of the message to retrieve
     * @return the message, or null if not found
     */
    public Message getProcessedMessage(String id) {
        return processedMessages.get(id);
    }
    
    /**
     * Gets the count of processed messages.
     *
     * @return the count of processed messages
     */
    public int getProcessedMessageCount() {
        return processedMessages.size();
    }
}