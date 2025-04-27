package com.logs_analytics.integration;

import com.logs_analytics.model.ApplicationLog;
import com.logs_analytics.repository.ApplicationLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=password",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.elasticsearch.rest.uris=http://localhost:9200"
})
public class LogAnalyticsIntegrationTest {

    @Autowired
    private ApplicationLogRepository applicationLogRepository;

    @Test
    @Transactional
    public void testSaveAndRetrieveApplicationLog() {
        // Create a test log
        ApplicationLog log = ApplicationLog.builder()
                .timestamp(LocalDateTime.now())
                .logLevel("INFO")
                .service("test-service")
                .message("Integration test message")
                .source("integration-test")
                .build();

        // Save the log
        ApplicationLog savedLog = applicationLogRepository.save(log);

        // Verify the log was saved
        assertNotNull(savedLog.getId());

        // Retrieve the log
        ApplicationLog retrievedLog = applicationLogRepository.findById(savedLog.getId()).orElse(null);

        // Verify the log was retrieved
        assertNotNull(retrievedLog);
        assertEquals("INFO", retrievedLog.getLogLevel());
        assertEquals("test-service", retrievedLog.getService());
        assertEquals("Integration test message", retrievedLog.getMessage());
        assertEquals("integration-test", retrievedLog.getSource());
    }

    @Test
    @Transactional
    public void testFindByLogLevel() {
        // Create test logs
        ApplicationLog infoLog = ApplicationLog.builder()
                .timestamp(LocalDateTime.now())
                .logLevel("INFO")
                .service("test-service")
                .message("Info message")
                .source("integration-test")
                .build();

        ApplicationLog errorLog = ApplicationLog.builder()
                .timestamp(LocalDateTime.now())
                .logLevel("ERROR")
                .service("test-service")
                .message("Error message")
                .stackTrace("Stack trace")
                .source("integration-test")
                .build();

        // Save the logs
        applicationLogRepository.save(infoLog);
        applicationLogRepository.save(errorLog);

        // Find logs by log level
        List<ApplicationLog> infoLogs = applicationLogRepository.findByLogLevel("INFO");
        List<ApplicationLog> errorLogs = applicationLogRepository.findByLogLevel("ERROR");

        // Verify the logs were found
        assertFalse(infoLogs.isEmpty());
        assertEquals("Info message", infoLogs.get(0).getMessage());

        assertFalse(errorLogs.isEmpty());
        assertEquals("Error message", errorLogs.get(0).getMessage());
        assertEquals("Stack trace", errorLogs.get(0).getStackTrace());
    }

    @Test
    @Transactional
    public void testFindByService() {
        // Create test logs
        ApplicationLog service1Log = ApplicationLog.builder()
                .timestamp(LocalDateTime.now())
                .logLevel("INFO")
                .service("service-1")
                .message("Service 1 message")
                .source("integration-test")
                .build();

        ApplicationLog service2Log = ApplicationLog.builder()
                .timestamp(LocalDateTime.now())
                .logLevel("INFO")
                .service("service-2")
                .message("Service 2 message")
                .source("integration-test")
                .build();

        // Save the logs
        applicationLogRepository.save(service1Log);
        applicationLogRepository.save(service2Log);

        // Find logs by service
        List<ApplicationLog> service1Logs = applicationLogRepository.findByService("service-1");
        List<ApplicationLog> service2Logs = applicationLogRepository.findByService("service-2");

        // Verify the logs were found
        assertFalse(service1Logs.isEmpty());
        assertEquals("Service 1 message", service1Logs.get(0).getMessage());

        assertFalse(service2Logs.isEmpty());
        assertEquals("Service 2 message", service2Logs.get(0).getMessage());
    }

    @Test
    @Transactional
    public void testFindByMessageContaining() {
        // Create test logs
        ApplicationLog log1 = ApplicationLog.builder()
                .timestamp(LocalDateTime.now())
                .logLevel("INFO")
                .service("test-service")
                .message("Connection established")
                .source("integration-test")
                .build();

        ApplicationLog log2 = ApplicationLog.builder()
                .timestamp(LocalDateTime.now())
                .logLevel("ERROR")
                .service("test-service")
                .message("Connection refused")
                .source("integration-test")
                .build();

        // Save the logs
        applicationLogRepository.save(log1);
        applicationLogRepository.save(log2);

        // Find logs by message containing
        List<ApplicationLog> connectionLogs = applicationLogRepository.findByMessageContaining("Connection");
        List<ApplicationLog> refusedLogs = applicationLogRepository.findByMessageContaining("refused");
        List<ApplicationLog> establishedLogs = applicationLogRepository.findByMessageContaining("established");

        // Verify the logs were found
        assertEquals(2, connectionLogs.size());
        assertEquals(1, refusedLogs.size());
        assertEquals(1, establishedLogs.size());
    }
}