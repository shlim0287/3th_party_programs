package com.kafka_consumer.service;

import com.kafka_consumer.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class MessageProcessingServiceTest {

    private MessageProcessingService messageProcessingService;

    @BeforeEach
    void setUp() {
        messageProcessingService = new MessageProcessingService();
    }

    @Test
    void testProcessInfoMessage() {
        // Arrange
        Message message = createTestMessage("INFO");

        // Act
        messageProcessingService.processMessage(message);

        // Assert
        assertEquals(message, messageProcessingService.getProcessedMessage(message.getId()));
        assertEquals(1, messageProcessingService.getProcessedMessageCount());
    }

    @Test
    void testProcessWarningMessage() {
        // Arrange
        Message message = createTestMessage("WARNING");

        // Act
        messageProcessingService.processMessage(message);

        // Assert
        assertEquals(message, messageProcessingService.getProcessedMessage(message.getId()));
        assertEquals(1, messageProcessingService.getProcessedMessageCount());
    }

    @Test
    void testProcessErrorMessage() {
        // Arrange
        Message message = createTestMessage("ERROR");

        // Act
        messageProcessingService.processMessage(message);

        // Assert
        assertEquals(message, messageProcessingService.getProcessedMessage(message.getId()));
        assertEquals(1, messageProcessingService.getProcessedMessageCount());
    }

    @Test
    void testProcessDefaultMessage() {
        // Arrange
        Message message = createTestMessage("UNKNOWN");

        // Act
        messageProcessingService.processMessage(message);

        // Assert
        assertEquals(message, messageProcessingService.getProcessedMessage(message.getId()));
        assertEquals(1, messageProcessingService.getProcessedMessageCount());
    }

    @Test
    void testProcessMultipleMessages() {
        // Arrange
        Message message1 = createTestMessage("INFO");
        Message message2 = createTestMessage("WARNING");
        Message message3 = createTestMessage("ERROR");

        // Act
        messageProcessingService.processMessage(message1);
        messageProcessingService.processMessage(message2);
        messageProcessingService.processMessage(message3);

        // Assert
        assertEquals(message1, messageProcessingService.getProcessedMessage(message1.getId()));
        assertEquals(message2, messageProcessingService.getProcessedMessage(message2.getId()));
        assertEquals(message3, messageProcessingService.getProcessedMessage(message3.getId()));
        assertEquals(3, messageProcessingService.getProcessedMessageCount());
    }

    @Test
    void testProcessMessageWithNullType() {
        // Arrange
        Message message = Message.builder()
                .id(UUID.randomUUID().toString())
                .content("Test message content")
                .timestamp(LocalDateTime.now())
                .source("test")
                .metadata(Map.of("key1", "value1", "key2", "value2"))
                .build();

        // Act
        messageProcessingService.processMessage(message);

        // Assert
        assertEquals(message, messageProcessingService.getProcessedMessage(message.getId()));
        assertEquals(1, messageProcessingService.getProcessedMessageCount());
    }

    @Test
    void testProcessMessageWithNullId() {
        // Arrange
        Message message = Message.builder()
                .content("Test message content")
                .type("INFO")
                .timestamp(LocalDateTime.now())
                .source("test")
                .metadata(Map.of("key1", "value1", "key2", "value2"))
                .build();

        // Act & Assert
        assertEquals(0, messageProcessingService.getProcessedMessageCount());
    }

    @Test
    void testProcessFatalErrorMessage() {
        // Arrange
        Message message = Message.builder()
                .id(UUID.randomUUID().toString())
                .content("FATAL error occurred")
                .type("ERROR")
                .timestamp(LocalDateTime.now())
                .source("test")
                .metadata(Map.of("key1", "value1", "key2", "value2"))
                .build();

        // Act & Assert
        assertThrows(RuntimeException.class, () -> messageProcessingService.processMessage(message));
        assertEquals(0, messageProcessingService.getProcessedMessageCount());
    }

    @Test
    void testGetNonExistentMessage() {
        // Act
        Message result = messageProcessingService.getProcessedMessage("non-existent-id");

        // Assert
        assertNull(result);
    }

    private Message createTestMessage(String type) {
        return Message.builder()
                .id(UUID.randomUUID().toString())
                .content("Test message content")
                .type(type)
                .timestamp(LocalDateTime.now())
                .source("test")
                .metadata(Map.of("key1", "value1", "key2", "value2"))
                .build();
    }
}