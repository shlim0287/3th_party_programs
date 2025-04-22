package com.logs_analytics.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logs_analytics.model.SystemMetric;
import com.logs_analytics.repository.SystemMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Kafka consumer for system metrics.
 * Consumes messages from the beats-logs topic and stores them in the database.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SystemMetricConsumer {

    private final SystemMetricRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * Consumes messages from the beats-logs topic.
     *
     * @param message the message from Kafka
     * @param ack     the acknowledgment
     */
    @KafkaListener(topics = "beats-logs", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message, Acknowledgment ack) {
        try {
            log.debug("Received system metric message: {}", message);
            JsonNode jsonNode = objectMapper.readTree(message);

            SystemMetric systemMetric = SystemMetric.builder()
                    .timestamp(parseTimestamp(jsonNode))
                    .host(getTextValue(jsonNode, "host"))
                    .metricType(getTextValue(jsonNode, "metric_type"))
                    .metricName(getTextValue(jsonNode, "metric_name"))
                    .metricValue(getDoubleValue(jsonNode, "metric_value"))
                    .cpuUsage(getMetricValue(jsonNode, "cpu", "usage"))
                    .memoryUsage(getMetricValue(jsonNode, "memory", "usage"))
                    .diskUsage(getMetricValue(jsonNode, "disk", "usage"))
                    .networkIn(getMetricValue(jsonNode, "network", "in"))
                    .networkOut(getMetricValue(jsonNode, "network", "out"))
                    .build();

            repository.save(systemMetric);
            log.info("Saved system metric: {}", systemMetric.getId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing system metric message: {}", message, e);
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

    /**
     * Gets a double value from the JSON node.
     */
    private Double getDoubleValue(JsonNode jsonNode, String field) {
        return jsonNode.has(field) ? jsonNode.get(field).asDouble() : null;
    }

    /**
     * Gets a metric value from a nested structure in the JSON node.
     */
    private Double getMetricValue(JsonNode jsonNode, String category, String field) {
        if (jsonNode.has(category) && jsonNode.get(category).has(field)) {
            return jsonNode.get(category).get(field).asDouble();
        }
        return null;
    }
}