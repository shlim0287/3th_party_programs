package com.logs_analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Log Analytics service.
 * This service consumes log data from Kafka, stores it in MySQL using JPA,
 * and provides analytics capabilities using Elasticsearch.
 */
@SpringBootApplication
@EnableKafka
@EnableJpaRepositories
@EnableScheduling
public class LogsAnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogsAnalyticsApplication.class, args);
    }

}
