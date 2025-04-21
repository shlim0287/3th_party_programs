package com.kafka_producer.config;

import com.kafka_producer.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for Kafka Producer.
 * Sets up the Kafka producer factory, template, and topic configuration.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.producer.client-id}")
    private String clientId;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;

    @Value("${spring.kafka.consumer.max-poll-records}")
    private int maxPollRecords;

    @Value("${spring.kafka.topic.demo-topic.partitions}")
    private int partitions;

    @Value("${spring.kafka.topic.demo-topic.replication-factor}")
    private short replicationFactor;

    /**
     * Topic names for the application.
     * In a real-world scenario, you might have multiple topics.
     */
    public static final String TOPIC_NAME = "demo-topic";
    public static final String FEEDBACK_TOPIC_NAME = "feedback-topic";

    /**
     * Creates a new Kafka topic with the specified configuration.
     *
     * @return the configured topic
     */
    @Bean
    public NewTopic demoTopic() {
        log.info("Creating topic: {} with {} partitions and replication factor: {}", 
                TOPIC_NAME, partitions, replicationFactor);
        return TopicBuilder.name(TOPIC_NAME)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }

    /**
     * Creates a new feedback topic for bidirectional communication.
     * This topic will be used for messages flowing from consumer to producer.
     *
     * @return the configured feedback topic
     */
    @Bean
    public NewTopic feedbackTopic() {
        log.info("Creating feedback topic: {} with {} partitions and replication factor: {}", 
                FEEDBACK_TOPIC_NAME, partitions, replicationFactor);
        return TopicBuilder.name(FEEDBACK_TOPIC_NAME)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }

    /**
     * Configures the Kafka producer factory with production-level settings.
     *
     * @return the configured producer factory
     */
    @Bean
    public ProducerFactory<String, Message> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Existing configuration...
        configProps.put("reconnect.backoff.ms", "1000");
        configProps.put("reconnect.backoff.max.ms", "10000");
        configProps.put("metadata.max.age.ms", "1000");
        configProps.put("connections.max.idle.ms", "30000");
        configProps.put(ProducerConfig.CLIENT_DNS_LOOKUP_CONFIG, "use_all_dns_ips");
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);

        // Add transaction ID prefix configuration
        configProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, clientId + "-tx-");

        DefaultKafkaProducerFactory<String, Message> factory = new DefaultKafkaProducerFactory<>(configProps);
        // Set transaction ID prefix
        factory.setTransactionIdPrefix(clientId + "-tx-");

        log.info("Configured Kafka producer with bootstrap servers: {}", bootstrapServers);
        return factory;
    }

    /**
     * Creates a KafkaTemplate using the producer factory.
     *
     * @return the configured KafkaTemplate
     */
    @Bean
    public KafkaTemplate<String, Message> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Configures the Kafka consumer factory for the producer service.
     * This allows the producer to also act as a consumer for the feedback topic.
     *
     * @return the configured consumer factory
     */
    @Bean
    public ConsumerFactory<String, Message> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // Connection properties
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId + "-producer");
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId + "-consumer");

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
     * Creates a Kafka listener container factory for the producer service.
     * This allows the producer to listen to the feedback topic.
     *
     * @return the configured listener container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Message> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Message> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // Configure manual acknowledgment
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Configure concurrency
        factory.setConcurrency(2);

        // Configure batch listening (false for single message processing)
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
}
