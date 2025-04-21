package com.kafka_consumer.service;

import com.kafka_consumer.config.KafkaConsumerConfig;
import com.kafka_consumer.model.Message;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for consuming messages from Kafka.
 * Includes metrics, error handling, and both batch and single message processing.
 */
@Slf4j
@Service
public class KafkaConsumerService {

    private final MeterRegistry meterRegistry;
    private final MessageProcessingService messageProcessingService;
    private final KafkaTemplate<String, Message> kafkaTemplate;

    // Metrics
    private final Counter messagesReceivedCounter;
    private final Counter messagesProcessedCounter;
    private final Counter messagesFailedCounter;
    private final Counter messagesSentCounter;
    private final Timer messageProcessingTimer;

    /**
     * Constructor that initializes metrics.
     */
    public KafkaConsumerService(MeterRegistry meterRegistry, 
                               MessageProcessingService messageProcessingService,
                               KafkaTemplate<String, Message> kafkaTemplate) {
        this.meterRegistry = meterRegistry;
        this.messageProcessingService = messageProcessingService;
        this.kafkaTemplate = kafkaTemplate;

        // Initialize metrics
        this.messagesReceivedCounter = Counter.builder("kafka.consumer.messages.received")
                .description("Number of messages received from Kafka")
                .register(meterRegistry);

        this.messagesProcessedCounter = Counter.builder("kafka.consumer.messages.processed")
                .description("Number of messages successfully processed")
                .register(meterRegistry);

        this.messagesFailedCounter = Counter.builder("kafka.consumer.messages.failed")
                .description("Number of messages that failed processing")
                .register(meterRegistry);

        this.messagesSentCounter = Counter.builder("kafka.consumer.messages.sent")
                .description("Number of feedback messages sent to producer")
                .register(meterRegistry);

        this.messageProcessingTimer = Timer.builder("kafka.consumer.message.processing.time")
                .description("Time taken to process a message")
                .register(meterRegistry);
    }

    /**
     * Batch listener for processing multiple messages at once.
     * This is more efficient for high-throughput scenarios.
     *
     * @param records the batch of records to process
     * @param ack the acknowledgment to manually commit offsets
     */
    @KafkaListener(
            topics = KafkaConsumerConfig.TOPIC_NAME,
            containerFactory = "kafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeBatch(List<ConsumerRecord<String, Message>> records, Acknowledgment ack) {
        if (records.isEmpty()) {
            log.debug("Received empty batch");
            ack.acknowledge();
            return;
        }

        log.info("Received batch of {} messages from topic: {}", 
                records.size(), KafkaConsumerConfig.TOPIC_NAME);
        messagesReceivedCounter.increment(records.size());

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            // Process each message in the batch
            for (ConsumerRecord<String, Message> record : records) {
                processRecord(record);
            }

            // Acknowledge the entire batch
            ack.acknowledge();

            long duration = sample.stop(messageProcessingTimer);
            log.info("Successfully processed batch of {} messages in {} ms", 
                    records.size(), TimeUnit.NANOSECONDS.toMillis(duration));

        } catch (Exception e) {
            log.error("Error processing batch of {} messages", records.size(), e);
            messagesFailedCounter.increment(records.size());

            // In a real-world scenario, you might want to:
            // 1. Send failed messages to a dead-letter queue
            // 2. Acknowledge only the successfully processed messages
            // 3. Implement a retry mechanism

            // For this example, we'll just rethrow to trigger the retry mechanism
            throw e;
        }
    }

    /**
     * Single message listener for processing one message at a time.
     * This is useful for messages that require ordered processing.
     *
     * @param record the record to process
     * @param ack the acknowledgment to manually commit offset
     */
    @KafkaListener(
            topics = KafkaConsumerConfig.TOPIC_NAME,
            containerFactory = "kafkaSingleListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}-single"
    )
    public void consumeSingle(ConsumerRecord<String, Message> record, Acknowledgment ack) {
        log.info("Received single message with key: {} from topic: {} partition: {} offset: {}", 
                record.key(), record.topic(), record.partition(), record.offset());
        messagesReceivedCounter.increment();

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            processRecord(record);
            ack.acknowledge();

            long duration = sample.stop(messageProcessingTimer);
            log.info("Successfully processed message with key: {} in {} ms", 
                    record.key(), TimeUnit.NANOSECONDS.toMillis(duration));

        } catch (Exception e) {
            log.error("Error processing message with key: {}", record.key(), e);
            messagesFailedCounter.increment();

            // For this example, we'll just rethrow to trigger the retry mechanism
            throw e;
        }
    }

    /**
     * Processes a single record.
     *
     * @param record the record to process
     */
    private void processRecord(ConsumerRecord<String, Message> record) {
        Message message = record.value();

        if (message == null) {
            log.warn("Received null message from topic: {} partition: {} offset: {}", 
                    record.topic(), record.partition(), record.offset());
            return;
        }

        log.debug("Processing message with id: {} and content: {}", message.getId(), message.getContent());

        try {
            // Process the message using the message processing service
            messageProcessingService.processMessage(message);
            messagesProcessedCounter.increment();

            // Send feedback message for processed messages
            if (message.getType() != null && message.getType().equals("REQUIRES_FEEDBACK")) {
                sendFeedbackMessage(message);
            }

        } catch (Exception e) {
            log.error("Error processing message with id: {}", message.getId(), e);
            throw e;
        }
    }

    /**
     * Large batch listener for processing many messages at once.
     * This demonstrates how to configure specific batch sizes for different use cases.
     *
     * @param records the large batch of records to process
     * @param ack the acknowledgment to manually commit offsets
     */
    @KafkaListener(
            topics = KafkaConsumerConfig.TOPIC_NAME,
            containerFactory = "kafkaLargeBatchListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}-large-batch"
    )
    public void consumeLargeBatch(List<ConsumerRecord<String, Message>> records, Acknowledgment ack) {
        if (records.isEmpty()) {
            log.debug("Received empty large batch");
            ack.acknowledge();
            return;
        }

        log.info("Received large batch of {} messages from topic: {}", 
                records.size(), KafkaConsumerConfig.TOPIC_NAME);
        messagesReceivedCounter.increment(records.size());

        Timer.Sample sample = Timer.start(meterRegistry);

        // Track successfully processed records for partial acknowledgment
        List<ConsumerRecord<String, Message>> successfulRecords = new ArrayList<>();
        List<ConsumerRecord<String, Message>> failedRecords = new ArrayList<>();

        try {
            // Process each message in the batch
            for (ConsumerRecord<String, Message> record : records) {
                try {
                    processRecord(record);
                    successfulRecords.add(record);
                } catch (Exception e) {
                    log.error("Error processing message in large batch with key: {}", record.key(), e);
                    failedRecords.add(record);
                    messagesFailedCounter.increment();
                }
            }

            // Acknowledge the entire batch
            ack.acknowledge();

            long duration = sample.stop(messageProcessingTimer);
            log.info("Successfully processed {} messages out of {} in large batch in {} ms", 
                    successfulRecords.size(), records.size(), TimeUnit.NANOSECONDS.toMillis(duration));

            // Handle failed records - in a real system, you might send these to a dead-letter queue
            if (!failedRecords.isEmpty()) {
                log.warn("{} messages failed processing in large batch", failedRecords.size());
                handleFailedMessages(failedRecords);
            }

        } catch (Exception e) {
            log.error("Catastrophic error processing large batch of {} messages", records.size(), e);
            messagesFailedCounter.increment(records.size() - successfulRecords.size());

            // In a real-world scenario with large batches, you might want to:
            // 1. Acknowledge only the successfully processed messages
            // 2. Send failed messages to a dead-letter queue
            // 3. Implement a more sophisticated retry mechanism

            throw e;
        }
    }

    /**
     * Handles failed messages by sending them to a recovery mechanism.
     * This demonstrates a pattern for handling message loss.
     *
     * @param failedRecords the list of records that failed processing
     */
    private void handleFailedMessages(List<ConsumerRecord<String, Message>> failedRecords) {
        log.info("Implementing recovery for {} failed messages", failedRecords.size());

        // In a real implementation, you might:
        // 1. Store failed messages in a database for later retry
        // 2. Send them to a dead-letter topic for manual inspection
        // 3. Implement a retry mechanism with backoff

        for (ConsumerRecord<String, Message> record : failedRecords) {
            Message message = record.value();
            if (message == null) continue;

            // Example: Create a recovery message
            Message recoveryMessage = Message.builder()
                    .id("recovery-" + message.getId())
                    .content("Recovery for: " + message.getContent())
                    .type("RECOVERY")
                    .timestamp(java.time.LocalDateTime.now())
                    .source("error-recovery-service")
                    .metadata(java.util.Map.of(
                        "originalMessageId", message.getId(),
                        "failureTimestamp", java.time.LocalDateTime.now().toString(),
                        "topic", record.topic(),
                        "partition", String.valueOf(record.partition()),
                        "offset", String.valueOf(record.offset())
                    ))
                    .build();

            // Send to feedback topic for the producer to handle
            sendFeedbackMessage(recoveryMessage);

            log.info("Sent recovery message for failed message with id: {}", message.getId());
        }
    }

    /**
     * Sends a feedback message to the producer via the feedback topic.
     * This demonstrates how a consumer can also act as a producer.
     *
     * @param originalMessage the original message that was processed
     * @return a CompletableFuture that will be completed when the send operation completes
     */
    @Transactional
    public CompletableFuture<SendResult<String, Message>> sendFeedbackMessage(Message originalMessage) {
        Message feedbackMessage = Message.builder()
                .id(UUID.randomUUID().toString())
                .content("Feedback for message: " + originalMessage.getId())
                .type("FEEDBACK")
                .timestamp(LocalDateTime.now())
                .source("consumer-service")
                .metadata(Map.of(
                    "originalMessageId", originalMessage.getId(),
                    "processingTime", LocalDateTime.now().toString()
                ))
                .build();

        log.info("Sending feedback message with id: {} to topic: {}", 
                feedbackMessage.getId(), KafkaConsumerConfig.FEEDBACK_TOPIC_NAME);

        CompletableFuture<SendResult<String, Message>> future =
                kafkaTemplate.send(KafkaConsumerConfig.FEEDBACK_TOPIC_NAME, feedbackMessage.getId(), feedbackMessage);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                messagesSentCounter.increment();
                log.info("Feedback message sent successfully to topic: {} partition: {} offset: {}", 
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send feedback message with id: {}", feedbackMessage.getId(), ex);
            }
        });

        return future;
    }
}
