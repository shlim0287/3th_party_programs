package com.kafka_producer.integration;

import com.kafka_producer.model.Message;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(partitions = 1, topics = {"demo-topic"})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.producer.client-id=test-producer",
        "spring.kafka.consumer.group-id=test-consumer-group",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.topic.demo-topic.partitions=1",
        "spring.kafka.topic.demo-topic.replication-factor=1"
})
public class KafkaProducerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private Consumer<String, Message> consumer;

    @Value("${spring.embedded.kafka.brokers}")
    private String brokerAddresses;

    @BeforeEach
    public void setUp() {
        // Configure the consumer
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerAddresses);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.kafka_producer.model");

        // Create the consumer
        DefaultKafkaConsumerFactory<String, Message> consumerFactory = 
                new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(),
                        new JsonDeserializer<>(Message.class, false));
        consumer = consumerFactory.createConsumer();
        consumer.subscribe(Collections.singletonList("demo-topic"));
    }

    @AfterEach
    public void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    public void testSendMessage() {
        // Create a test message
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("content", "Integration test message");
        requestBody.put("type", "INFO");
        requestBody.put("source", "integration-test");

        // Send the message using the REST API
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/messages", requestBody, Map.class);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("success", response.getBody().get("status"));
        assertNotNull(response.getBody().get("messageId"));
        assertEquals("demo-topic", response.getBody().get("topic"));

        // Verify the message was received by Kafka
        ConsumerRecords<String, Message> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        assertFalse(records.isEmpty());

        // Verify the message content
        Message receivedMessage = records.iterator().next().value();
        assertEquals("Integration test message", receivedMessage.getContent());
        assertEquals("INFO", receivedMessage.getType());
        assertEquals("integration-test", receivedMessage.getSource());
    }

    @Test
    public void testSendSimpleMessage() {
        // Send a simple message using the REST API
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/messages/simple?content=Simple integration test&type=WARNING", 
                null, Map.class);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("success", response.getBody().get("status"));
        assertNotNull(response.getBody().get("messageId"));
        assertEquals("demo-topic", response.getBody().get("topic"));

        // Verify the message was received by Kafka
        ConsumerRecords<String, Message> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        assertFalse(records.isEmpty());

        // Verify the message content
        Message receivedMessage = records.iterator().next().value();
        assertEquals("Simple integration test", receivedMessage.getContent());
        assertEquals("WARNING", receivedMessage.getType());
        assertEquals("api-simple", receivedMessage.getSource());
    }
}
