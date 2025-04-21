package com.kafka_consumer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kafka_consumer.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Custom Kafka deserializer for Message objects.
 * Converts JSON data from Kafka back to Message objects.
 */
@Slf4j
public class MessageDeserializer implements Deserializer<Message> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructor that registers the JavaTimeModule to handle Java 8 date/time types.
     */
    public MessageDeserializer() {
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
     * Deserialize a record value from a byte array into a Message object.
     *
     * @param topic topic associated with the data
     * @param data  serialized bytes
     * @return deserialized typed data
     */
    @Override
    public Message deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            String json = new String(data, StandardCharsets.UTF_8);
            log.debug("Deserializing message from topic: {} with data: {}", topic, json);
            return objectMapper.readValue(data, Message.class);
        } catch (Exception e) {
            log.error("Error deserializing JSON message from topic: {}", topic, e);
            throw new SerializationException("Error deserializing JSON message", e);
        }
    }

    /**
     * Close this deserializer.
     */
    @Override
    public void close() {
        // Nothing to close
    }
}