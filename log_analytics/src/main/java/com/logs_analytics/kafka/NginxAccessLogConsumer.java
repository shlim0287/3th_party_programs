package com.logs_analytics.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logs_analytics.model.NginxAccessLog;
import com.logs_analytics.repository.NginxAccessLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Kafka consumer for Nginx access logs.
 * Consumes messages from the nginx-access-logs topic and stores them in the database.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NginxAccessLogConsumer {

    private final NginxAccessLogRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * Consumes messages from the nginx-access-logs topic.
     *
     * @param message the message from Kafka
     * @param ack     the acknowledgment
     */
    @KafkaListener(topics = "nginx-access-logs", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message, Acknowledgment ack) {
        try {
            log.debug("Received nginx access log message: {}", message);
            JsonNode jsonNode = objectMapper.readTree(message);

            NginxAccessLog accessLog = NginxAccessLog.builder()
                    .clientIp(getTextValue(jsonNode, "clientip"))
                    .timestamp(parseTimestamp(jsonNode))
                    .requestMethod(getTextValue(jsonNode, "verb"))
                    .requestPath(getTextValue(jsonNode, "request"))
                    .statusCode(getIntValue(jsonNode, "response"))
                    .responseSize(getLongValue(jsonNode, "bytes"))
                    .referrer(getTextValue(jsonNode, "referrer"))
                    .userAgent(getTextValue(jsonNode, "agent"))
                    .responseTime(getDoubleValue(jsonNode, "response_time"))
                    .country(getGeoipValue(jsonNode, "country_name"))
                    .hourOfDay(getIntValue(jsonNode, "hour_of_day"))
                    .responseTimeBucket(getTextValue(jsonNode, "response_time_bucket"))
                    .build();

            repository.save(accessLog);
            log.info("Saved nginx access log: {}", accessLog.getId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing nginx access log message: {}", message, e);
            // Depending on your error handling strategy, you might want to:
            // 1. Acknowledge the message anyway (to avoid reprocessing)
            // 2. Not acknowledge and let Kafka retry
            // 3. Send to a dead letter queue
            ack.acknowledge(); // For now, we'll acknowledge to avoid endless retries
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
     * Gets an integer value from the JSON node.
     */
    private Integer getIntValue(JsonNode jsonNode, String field) {
        return jsonNode.has(field) ? jsonNode.get(field).asInt() : null;
    }

    /**
     * Gets a long value from the JSON node.
     */
    private Long getLongValue(JsonNode jsonNode, String field) {
        return jsonNode.has(field) ? jsonNode.get(field).asLong() : null;
    }

    /**
     * Gets a double value from the JSON node.
     */
    private Double getDoubleValue(JsonNode jsonNode, String field) {
        return jsonNode.has(field) ? jsonNode.get(field).asDouble() : null;
    }

    /**
     * Gets a value from the geoip object in the JSON node.
     */
    private String getGeoipValue(JsonNode jsonNode, String field) {
        return jsonNode.has("geoip") && jsonNode.get("geoip").has(field)
                ? jsonNode.get("geoip").get(field).asText()
                : null;
    }
}