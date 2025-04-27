package com.kafka_producer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafka_producer.model.Message;
import com.kafka_producer.service.KafkaProducerService;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class KafkaProducerControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private KafkaProducerService kafkaProducerService;

    private KafkaProducerController kafkaProducerController;

    private Message testMessage;
    private SendResult<String, Message> sendResult;

    @BeforeEach
    void setUp() {
        // Initialize controller and MockMvc
        kafkaProducerController = new KafkaProducerController(kafkaProducerService);
        mockMvc = MockMvcBuilders.standaloneSetup(kafkaProducerController).build();

        // Configure ObjectMapper for Java 8 date/time
        objectMapper.findAndRegisterModules();

        // Create test message
        testMessage = Message.builder()
                .id(UUID.randomUUID().toString())
                .content("Test message content")
                .type("INFO")
                .timestamp(LocalDateTime.now())
                .source("test")
                .metadata(Map.of("key1", "value1", "key2", "value2"))
                .build();

        // Create send result
        RecordMetadata recordMetadata = new RecordMetadata(
                new TopicPartition("demo-topic", 0),
                0L, 0, 0L, 0, 0);

        sendResult = new SendResult<>(
                new ProducerRecord<>("demo-topic", testMessage.getId(), testMessage),
                recordMetadata);
    }

    @Test
    void testSendMessage() throws Exception {
        // Arrange
        when(kafkaProducerService.sendMessage(any(Message.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        // Act & Assert
        mockMvc.perform(post("/api/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testMessage)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.messageId").value(testMessage.getId()))
                .andExpect(jsonPath("$.topic").value("demo-topic"))
                .andExpect(jsonPath("$.partition").value(0))
                .andExpect(jsonPath("$.offset").value(0));
    }

    @Test
    void testSendSimpleMessage() throws Exception {
        // Arrange
        when(kafkaProducerService.sendMessage(any(Message.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        // Act & Assert
        mockMvc.perform(post("/api/messages/simple")
                .param("content", "Simple test message")
                .param("type", "INFO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.topic").value("demo-topic"))
                .andExpect(jsonPath("$.partition").value(0))
                .andExpect(jsonPath("$.offset").value(0));
    }

    @Test
    void testSendMessageWithError() throws Exception {
        // Arrange
        CompletableFuture<SendResult<String, Message>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Test exception"));

        when(kafkaProducerService.sendMessage(any(Message.class)))
                .thenReturn(future);

        // Act & Assert
        mockMvc.perform(post("/api/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testMessage)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Failed to send message: Test exception"));
    }

    @Test
    void testSendMessageWithTimeout() throws Exception {
        // Arrange
        CompletableFuture<SendResult<String, Message>> future = new CompletableFuture<>();
        future.completeExceptionally(new java.util.concurrent.TimeoutException("Timeout"));

        when(kafkaProducerService.sendMessage(any(Message.class)))
                .thenReturn(future);

        // Act & Assert
        mockMvc.perform(post("/api/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testMessage)))
                .andExpect(status().isRequestTimeout())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Timeout while sending message"));
    }
}
