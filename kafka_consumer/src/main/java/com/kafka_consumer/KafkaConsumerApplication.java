package com.kafka_consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Main application class for the Kafka Consumer.
 * Enables Spring Boot auto-configuration and retry support.
 */
@SpringBootApplication
@EnableRetry
public class KafkaConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaConsumerApplication.class, args);
    }

}
