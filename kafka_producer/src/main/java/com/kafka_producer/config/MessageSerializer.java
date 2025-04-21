package com.kafka_producer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kafka_producer.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

/**
 * Custom Kafka serializer for Message objects.
 * Converts Message objects to JSON format before sending to Kafka.
 */
@Slf4j
public class MessageSerializer implements Serializer<Message> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructor that registers the JavaTimeModule to handle Java 8 date/time types.
     */
    public MessageSerializer() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Configure this class.
     *
     * @param configs configs in key/value pairs
     * @param isKey   whether is for key or value
     */
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // Nothing to configure
    }

    /**
     * Convert Message to byte array.
     *
     * @param topic topic associated with data
     * @param data  typed data
     * @return serialized bytes
     */
    @Override
    public byte[] serialize(String topic, Message data) {
        if (data == null) {
            log.warn("Null data received for serialization for topic: {}", topic);
            return null;
        }

        try {
            log.debug("Serializing message: {} for topic: {}", data, topic);
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            log.error("Error serializing Message: {} for topic: {}", data, topic, e);
            throw new SerializationException("Error serializing JSON message", e);
        }
    }

    /**
     * Close this serializer.
     */
    @Override
    public void close() {
        // Nothing to close
    }
}