package com.logs_analytics.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logs_analytics.model.ApplicationLog;
import com.logs_analytics.repository.ApplicationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Kafka consumer for application logs.
 * Consumes messages from the application-logs topic and stores them in the database.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ApplicationLogConsumer {

    private final ApplicationLogRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * Consumes messages from the application-logs topic.
     *
     * @param message the message from Kafka
     * @param ack     the acknowledgment
     */
    @KafkaListener(topics = "application-logs", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message, Acknowledgment ack) {
        try {
            log.debug("Received application log message: {}", message);
            JsonNode jsonNode = objectMapper.readTree(message);

            ApplicationLog applicationLog = ApplicationLog.builder()
                    .timestamp(parseTimestamp(jsonNode))
                    .logLevel(getTextValue(jsonNode, "log_level"))
                    .service(getTextValue(jsonNode, "service"))
                    .message(getTextValue(jsonNode, "log_message"))
                    .stackTrace(getTextValue(jsonNode, "stack_trace"))
                    .source(getTextValue(jsonNode, "source"))
                    .threadName(getTextValue(jsonNode, "thread_name"))
                    .loggerName(getTextValue(jsonNode, "logger_name"))
                    .build();

            repository.save(applicationLog);
            log.info("Saved application log: {}", applicationLog.getId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing application log message: {}", message, e);
            // Acknowledge to avoid endless retries
            ack.acknowledge();
        }
    }

    /**
     * Parses the timestamp from the JSON node.
     */
    private LocalDateTime parseTimestamp(JsonNode jsonNode) {
        try {
            if (jsonNode.has("@timestamp")) {
                return LocalDateTime.parse(
                        jsonNode.get("@timestamp").asText(),
                        DateTimeFormatter.ISO_DATE_TIME
                );
            } else if (jsonNode.has("timestamp")) {
                // Handle different timestamp formats
                String timestamp = jsonNode.get("timestamp").asText();
                if (timestamp.contains("T")) {
                    return LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME);
                } else {
                    // Assume epoch millis
                    return LocalDateTime.ofEpochSecond(
                            Long.parseLong(timestamp) / 1000,
                            0,
                            ZoneOffset.UTC
                    );
                }
            }
            return LocalDateTime.now();
        } catch (Exception e) {
            log.warn("Error parsing timestamp, using current time: {}", e.getMessage());
            return LocalDateTime.now();
        }
    }

    /**
     * Gets a text value from the JSON node.
     */
    private String getTextValue(JsonNode jsonNode, String field) {
        return jsonNode.has(field) ? jsonNode.get(field).asText() : null;
    }
}