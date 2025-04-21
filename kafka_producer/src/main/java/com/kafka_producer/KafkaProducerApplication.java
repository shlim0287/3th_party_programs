package com.kafka_producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Main application class for the Kafka Producer.
 * Enables Spring Boot auto-configuration and retry support.
 */
@SpringBootApplication
@EnableRetry
public class KafkaProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaProducerApplication.class, args);
    }

}
