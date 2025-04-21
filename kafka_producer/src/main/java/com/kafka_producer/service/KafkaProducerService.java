package com.kafka_producer.service;

import com.kafka_producer.config.KafkaProducerConfig;
import com.kafka_producer.model.Message;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for producing messages to Kafka.
 * Includes retry logic, metrics, and comprehensive error handling.
 */
@Slf4j
@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, Message> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    // Metrics
    private final Counter messagesSentCounter;
    private final Counter messagesFailedCounter;
    private final Counter messagesReceivedCounter;
    private final Timer messageSendTimer;

    /**
     * Constructor that initializes metrics.
     */
    public KafkaProducerService(KafkaTemplate<String, Message> kafkaTemplate, MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.meterRegistry = meterRegistry;

        // Initialize metrics
        this.messagesSentCounter = Counter.builder("kafka.producer.messages.sent")
                .description("Number of messages successfully sent to Kafka")
                .register(meterRegistry);

        this.messagesFailedCounter = Counter.builder("kafka.producer.messages.failed")
                .description("Number of messages that failed to send to Kafka")
                .register(meterRegistry);

        this.messagesReceivedCounter = Counter.builder("kafka.producer.messages.received")
                .description("Number of messages received from feedback topic")
                .register(meterRegistry);

        this.messageSendTimer = Timer.builder("kafka.producer.message.send.time")
                .description("Time taken to send a message to Kafka")
                .register(meterRegistry);
    }

    /**
     * Sends a message to Kafka with the specified key.
     * Includes retry logic for transient failures.
     *
     * @param key the message key
     * @param message the message to send
     * @return a CompletableFuture that will be completed when the send operation completes
     */
    @Retryable(
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @Transactional
    public CompletableFuture<SendResult<String, Message>> sendMessage(String key, Message message) {
        if (message.getId() == null) {
            message.setId(UUID.randomUUID().toString());
        }

        log.info("Sending message with key: {} and id: {} to topic: {}", 
                key, message.getId(), KafkaProducerConfig.TOPIC_NAME);

        Timer.Sample sample = Timer.start(meterRegistry);

        CompletableFuture<SendResult<String, Message>> future =
                CompletableFuture.supplyAsync(() ->
                        kafkaTemplate.executeInTransaction(
                                operations -> operations.send(
                                KafkaProducerConfig.TOPIC_NAME,
                                key,
                                message
                ).join()));

        future.whenComplete((result, ex) -> {
            long duration = sample.stop(messageSendTimer);

            if (ex == null) {
                messagesSentCounter.increment();
                log.info("Message sent successfully to topic: {} partition: {} offset: {} in {} ms",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        TimeUnit.NANOSECONDS.toMillis(duration));
            } else {
                messagesFailedCounter.increment();
                log.error("Failed to send message with key: {} and id: {} to topic: {}",
                        key, message.getId(), KafkaProducerConfig.TOPIC_NAME, ex);
            }
        });

        return future;
    }

    /**
     * Sends a message to Kafka with a random UUID as the key.
     *
     * @param message the message to send
     * @return a CompletableFuture that will be completed when the send operation completes
     */
    @Transactional
    public CompletableFuture<SendResult<String, Message>> sendMessage(Message message) {
        return sendMessage(UUID.randomUUID().toString(), message);
    }

    /**
     * Listens for messages on the feedback topic.
     * This demonstrates how a producer can also act as a consumer.
     *
     * @param record the record received from Kafka
     * @param ack the acknowledgment to manually commit offset
     */
    @KafkaListener(
            topics = KafkaProducerConfig.FEEDBACK_TOPIC_NAME,
            containerFactory = "kafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}-producer"
    )
    public void consumeFeedback(ConsumerRecord<String, Message> record, Acknowledgment ack) {
        log.info("Producer received feedback message with key: {} from topic: {} partition: {} offset: {}", 
                record.key(), record.topic(), record.partition(), record.offset());
        messagesReceivedCounter.increment();

        Message message = record.value();
        if (message == null) {
            log.warn("Received null message from feedback topic");
            ack.acknowledge();
            return;
        }

        log.info("Processing feedback message: {}", message);

        // Here you would implement your feedback processing logic
        // For example, you might update some internal state or trigger another action

        // Acknowledge the message
        ack.acknowledge();
        log.info("Feedback message processed and acknowledged");
    }
}
