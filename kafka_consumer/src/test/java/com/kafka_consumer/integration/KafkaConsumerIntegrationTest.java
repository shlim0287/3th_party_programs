package com.kafka_consumer.integration;

import com.kafka_consumer.model.Message;
import com.kafka_consumer.service.MessageProcessingService;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = {"demo-topic", "feedback-topic"})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.group-id=test-consumer-group",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.topic.demo-topic.partitions=1",
        "spring.kafka.topic.demo-topic.replication-factor=1"
})
public class KafkaConsumerIntegrationTest {

    @Value("${spring.embedded.kafka.brokers}")
    private String brokerAddresses;

    @Autowired
    private MessageProcessingService messageProcessingService;

    private Producer<String, Message> producer;

    @BeforeEach
    public void setUp() {
        // Configure the producer
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerAddresses);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Create the producer
        DefaultKafkaProducerFactory<String, Message> producerFactory = 
                new DefaultKafkaProducerFactory<>(producerProps);
        producer = producerFactory.createProducer();
    }

    @AfterEach
    public void tearDown() {
        if (producer != null) {
            producer.close();
        }
    }

    @Test
    public void testConsumeMessage() throws Exception {
        // Create a test message
        String messageId = UUID.randomUUID().toString();
        Message message = Message.builder()
                .id(messageId)
                .content("Integration test message")
                .type("INFO")
                .timestamp(LocalDateTime.now())
                .source("integration-test")
                .build();

        // Send the message to Kafka
        producer.send(new ProducerRecord<>("demo-topic", messageId, message)).get();

        // Wait for the message to be processed
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            // Verify the message was processed
            Message processedMessage = messageProcessingService.getProcessedMessage(messageId);
            assertNotNull(processedMessage);
            assertEquals("Integration test message", processedMessage.getContent());
            assertEquals("INFO", processedMessage.getType());
            assertEquals("integration-test", processedMessage.getSource());
        });
    }

    @Test
    public void testConsumeErrorMessage() throws Exception {
        // Create a test error message
        String messageId = UUID.randomUUID().toString();
        Message message = Message.builder()
                .id(messageId)
                .content("Error message for testing")
                .type("ERROR")
                .timestamp(LocalDateTime.now())
                .source("integration-test")
                .build();

        // Send the message to Kafka
        producer.send(new ProducerRecord<>("demo-topic", messageId, message)).get();

        // Wait for the message to be processed
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            // Verify the message was processed
            Message processedMessage = messageProcessingService.getProcessedMessage(messageId);
            assertNotNull(processedMessage);
            assertEquals("Error message for testing", processedMessage.getContent());
            assertEquals("ERROR", processedMessage.getType());
            assertEquals("integration-test", processedMessage.getSource());
        });
    }
}