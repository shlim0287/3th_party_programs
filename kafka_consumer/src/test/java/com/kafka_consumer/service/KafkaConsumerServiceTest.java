package com.kafka_consumer.service;

import com.kafka_consumer.model.Message;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KafkaConsumerServiceTest {

    @Mock
    private KafkaTemplate<String, Message> kafkaTemplate;

    @Mock
    private MessageProcessingService messageProcessingService;

    @Mock
    private Acknowledgment acknowledgment;

    private MeterRegistry meterRegistry;
    private KafkaConsumerService kafkaConsumerService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        kafkaConsumerService = new KafkaConsumerService(meterRegistry, messageProcessingService, kafkaTemplate);
    }

    @Test
    void testConsumeSingle() {
        // Arrange
        Message message = createTestMessage();
        ConsumerRecord<String, Message> record = new ConsumerRecord<>(
                "demo-topic", 0, 0L, UUID.randomUUID().toString(), message);

        // Act
        kafkaConsumerService.consumeSingle(record, acknowledgment);

        // Assert
        verify(messageProcessingService, times(1)).processMessage(message);
        verify(acknowledgment, times(1)).acknowledge();

        // Verify metrics
        assertEquals(1, meterRegistry.get("kafka.consumer.messages.received").counter().count());
        assertEquals(1, meterRegistry.get("kafka.consumer.messages.processed").counter().count());
    }

    @Test
    void testConsumeBatch() {
        // Arrange
        List<ConsumerRecord<String, Message>> records = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Message message = createTestMessage();
            ConsumerRecord<String, Message> record = new ConsumerRecord<>(
                    "demo-topic", 0, i, UUID.randomUUID().toString(), message);
            records.add(record);
        }

        // Act
        kafkaConsumerService.consumeBatch(records, acknowledgment);

        // Assert
        verify(messageProcessingService, times(3)).processMessage(any(Message.class));
        verify(acknowledgment, times(1)).acknowledge();

        // Verify metrics
        assertEquals(3, meterRegistry.get("kafka.consumer.messages.received").counter().count());
        assertEquals(3, meterRegistry.get("kafka.consumer.messages.processed").counter().count());
    }

    @Test
    void testConsumeLargeBatch() {
        // Arrange
        List<ConsumerRecord<String, Message>> records = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Message message = createTestMessage();
            ConsumerRecord<String, Message> record = new ConsumerRecord<>(
                    "demo-topic", 0, i, UUID.randomUUID().toString(), message);
            records.add(record);
        }

        // Act
        kafkaConsumerService.consumeLargeBatch(records, acknowledgment);

        // Assert
        verify(messageProcessingService, times(5)).processMessage(any(Message.class));
        verify(acknowledgment, times(1)).acknowledge();

        // Verify metrics
        assertEquals(5, meterRegistry.get("kafka.consumer.messages.received").counter().count());
        assertEquals(5, meterRegistry.get("kafka.consumer.messages.processed").counter().count());
    }

    @Test
    void testSendFeedbackMessage() {
        // Arrange
        Message originalMessage = createTestMessage();
        
        RecordMetadata recordMetadata = new RecordMetadata(
                new TopicPartition("feedback-topic", 0),
                0L, 0, 0L, 0, 0);

        SendResult<String, Message> sendResult = new SendResult<>(
                new ProducerRecord<>("feedback-topic", originalMessage.getId(), originalMessage),
                recordMetadata);

        when(kafkaTemplate.send(anyString(), anyString(), any(Message.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        // Act
        CompletableFuture<SendResult<String, Message>> result = kafkaConsumerService.sendFeedbackMessage(originalMessage);

        // Assert
        assertNotNull(result);
        assertEquals(sendResult, result.join());

        // Verify metrics
        assertEquals(1, meterRegistry.get("kafka.consumer.messages.sent").counter().count());
    }

    @Test
    void testConsumeSingleWithNullMessage() {
        // Arrange
        ConsumerRecord<String, Message> record = new ConsumerRecord<>(
                "demo-topic", 0, 0L, UUID.randomUUID().toString(), null);

        // Act
        kafkaConsumerService.consumeSingle(record, acknowledgment);

        // Assert
        verify(messageProcessingService, never()).processMessage(any());
        verify(acknowledgment, times(1)).acknowledge();

        // Verify metrics
        assertEquals(1, meterRegistry.get("kafka.consumer.messages.received").counter().count());
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