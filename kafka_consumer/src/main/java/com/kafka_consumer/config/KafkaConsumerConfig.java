package com.kafka_consumer.config;

import com.kafka_consumer.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for Kafka Consumer.
 * Sets up the Kafka consumer factory and listener container factory.
 */
@Configuration
@EnableKafka
@Slf4j
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;

    @Value("${spring.kafka.consumer.max-poll-records}")
    private int maxPollRecords;

    /**
     * Topic names for the application.
     * These should match the producer's topic names.
     */
    public static final String TOPIC_NAME = "demo-topic";
    public static final String FEEDBACK_TOPIC_NAME = "feedback-topic";

    /**
     * Configures the Kafka consumer factory with production-level settings.
     *
     * @return the configured consumer factory
     */
    @Bean
    public ConsumerFactory<String, Message> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // Connection properties
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, groupId + "-client");

        // Deserializer properties
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.kafka_consumer.model,com.kafka_producer.model");

        // Offset management
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);

        // Performance tuning
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);

        // Isolation level (read committed for exactly-once semantics)
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(Message.class, false)
        );
    }

    /**
     * Creates a Kafka listener container factory with error handling and manual acknowledgment.
     *
     * @return the configured listener container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Message> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Message> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // Configure manual acknowledgment
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Configure concurrency
        factory.setConcurrency(3);

        // Configure batch listening
        factory.setBatchListener(true);

        // Configure error handling with retry
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                // Retry with 1-second initial interval, max 3 attempts
                new FixedBackOff(1000L, 3L)
        );

        // Skip non-recoverable exceptions
        errorHandler.addNotRetryableExceptions(
                org.apache.kafka.common.errors.SerializationException.class,
                com.fasterxml.jackson.core.JsonParseException.class
        );

        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    /**
     * Creates a Kafka listener container factory for single message processing.
     *
     * @return the configured listener container factory for single messages
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Message> kafkaSingleListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Message> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // Configure manual acknowledgment
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Configure concurrency
        factory.setConcurrency(3);

        // Configure single message listening (not batch)
        factory.setBatchListener(false);

        // Configure error handling with retry
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                // Retry with 1-second initial interval, max 3 attempts
                new FixedBackOff(1000L, 3L)
        );

        // Skip non-recoverable exceptions
        errorHandler.addNotRetryableExceptions(
                org.apache.kafka.common.errors.SerializationException.class,
                com.fasterxml.jackson.core.JsonParseException.class
        );

        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    /**
     * Creates a Kafka listener container factory for large batch processing.
     * This demonstrates how to configure specific batch sizes for different use cases.
     *
     * @return the configured listener container factory for large batches
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Message> kafkaLargeBatchListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Message> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();

        // Create a consumer factory with custom batch size
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId + "-large-batch");
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, groupId + "-large-batch-client");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.kafka_consumer.model,com.kafka_producer.model");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);

        // Set a larger batch size (1000 records)
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1000);

        // Increase fetch size for larger batches
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024 * 1024); // 1MB minimum fetch
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 1000); // Wait up to 1 second

        // Set isolation level
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        ConsumerFactory<String, Message> largeBatchConsumerFactory = new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(Message.class, false)
        );

        factory.setConsumerFactory(largeBatchConsumerFactory);

        // Configure manual acknowledgment
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Configure concurrency (fewer threads for larger batches)
        factory.setConcurrency(2);

        // Configure batch listening
        factory.setBatchListener(true);

        // Configure error handling with retry
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                // Retry with 2-second initial interval, max 2 attempts for large batches
                new FixedBackOff(2000L, 2L)
        );

        // Skip non-recoverable exceptions
        errorHandler.addNotRetryableExceptions(
                org.apache.kafka.common.errors.SerializationException.class,
                com.fasterxml.jackson.core.JsonParseException.class
        );

        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
