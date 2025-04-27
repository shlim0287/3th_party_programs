package com.kafka_producer.service;

import com.kafka_producer.model.Message;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KafkaProducerServiceTest {

    @Mock
    private KafkaTemplate<String, Message> kafkaTemplate;

    @Mock
    private Acknowledgment acknowledgment;

    private MeterRegistry meterRegistry;
    private KafkaProducerService kafkaProducerService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        kafkaProducerService = new KafkaProducerService(kafkaTemplate, meterRegistry);
    }

    @Test
    void testSendMessage() {
        // Arrange
        Message message = createTestMessage();
        String key = UUID.randomUUID().toString();

        RecordMetadata recordMetadata = new RecordMetadata(
                new TopicPartition("demo-topic", 0),
                0L, 0, 0L, 0, 0);

        SendResult<String, Message> sendResult = new SendResult<>(
                new ProducerRecord<>("demo-topic", key, message),
                recordMetadata);

        CompletableFuture<SendResult<String, Message>> future = CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.executeInTransaction(any())).thenReturn(future);

        // Act
        CompletableFuture<SendResult<String, Message>> result = kafkaProducerService.sendMessage(key, message);

        // Assert
        assertNotNull(result);
        assertEquals(sendResult, result.join());

        // Verify metrics
        assertEquals(1, meterRegistry.get("kafka.producer.messages.sent").counter().count());
    }

    @Test
    void testSendMessageWithoutKey() {
        // Arrange
        Message message = createTestMessage();

        RecordMetadata recordMetadata = new RecordMetadata(
                new TopicPartition("demo-topic", 0),
                0L, 0, 0L, 0, 0);

        SendResult<String, Message> sendResult = new SendResult<>(
                new ProducerRecord<>("demo-topic", null, message),
                recordMetadata);

        CompletableFuture<SendResult<String, Message>> future = CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.executeInTransaction(any())).thenReturn(future);

        // Act
        CompletableFuture<SendResult<String, Message>> result = kafkaProducerService.sendMessage(message);

        // Assert
        assertNotNull(result);
        assertEquals(sendResult, result.join());

        // Verify metrics
        assertEquals(1, meterRegistry.get("kafka.producer.messages.sent").counter().count());
    }

    @Test
    void testConsumeFeedback() {
        // Arrange
        Message message = createTestMessage();
        ConsumerRecord<String, Message> record = new ConsumerRecord<>(
                "feedback-topic", 0, 0L, UUID.randomUUID().toString(), message);

        // Act
        kafkaProducerService.consumeFeedback(record, acknowledgment);

        // Assert
        verify(acknowledgment, times(1)).acknowledge();

        // Verify metrics
        assertEquals(1, meterRegistry.get("kafka.producer.messages.received").counter().count());
    }

    @Test
    void testConsumeFeedbackWithNullMessage() {
        // Arrange
        ConsumerRecord<String, Message> record = new ConsumerRecord<>(
                "feedback-topic", 0, 0L, UUID.randomUUID().toString(), null);

        // Act
        kafkaProducerService.consumeFeedback(record, acknowledgment);

        // Assert
        verify(acknowledgment, times(1)).acknowledge();

        // Verify metrics
        assertEquals(1, meterRegistry.get("kafka.producer.messages.received").counter().count());
    }

    private Message createTestMessage() {
        return Message.builder()
                .id(UUID.randomUUID().toString())
                .content("Test message content")
                .type("INFO")
                .timestamp(LocalDateTime.now())
                .source("test")
                .metadata(Map.of("key1", "value1", "key2", "value2"))
                .build();
    }
}
