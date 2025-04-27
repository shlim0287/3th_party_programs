package com.logs_analytics.repository;

import com.logs_analytics.model.ApplicationLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class ApplicationLogRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ApplicationLogRepository applicationLogRepository;

    @Test
    void testFindByLogLevel() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        
        ApplicationLog infoLog = ApplicationLog.builder()
                .timestamp(now)
                .logLevel("INFO")
                .service("test-service")
                .message("Info message")
                .source("test")
                .build();
        
        ApplicationLog errorLog = ApplicationLog.builder()
                .timestamp(now)
                .logLevel("ERROR")
                .service("test-service")
                .message("Error message")
                .stackTrace("Stack trace")
                .source("test")
                .build();
        
        entityManager.persist(infoLog);
        entityManager.persist(errorLog);
        entityManager.flush();
        
        // Act
        List<ApplicationLog> infoLogs = applicationLogRepository.findByLogLevel("INFO");
        List<ApplicationLog> errorLogs = applicationLogRepository.findByLogLevel("ERROR");
        
        // Assert
        assertEquals(1, infoLogs.size());
        assertEquals("Info message", infoLogs.get(0).getMessage());
        
        assertEquals(1, errorLogs.size());
        assertEquals("Error message", errorLogs.get(0).getMessage());
        assertEquals("Stack trace", errorLogs.get(0).getStackTrace());
    }
    
    @Test
    void testFindByService() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        
        ApplicationLog service1Log = ApplicationLog.builder()
                .timestamp(now)
                .logLevel("INFO")
                .service("service-1")
                .message("Service 1 message")
                .source("test")
                .build();
        
        ApplicationLog service2Log = ApplicationLog.builder()
                .timestamp(now)
                .logLevel("INFO")
                .service("service-2")
                .message("Service 2 message")
                .source("test")
                .build();
        
        entityManager.persist(service1Log);
        entityManager.persist(service2Log);
        entityManager.flush();
        
        // Act
        List<ApplicationLog> service1Logs = applicationLogRepository.findByService("service-1");
        List<ApplicationLog> service2Logs = applicationLogRepository.findByService("service-2");
        
        // Assert
        assertEquals(1, service1Logs.size());
        assertEquals("Service 1 message", service1Logs.get(0).getMessage());
        
        assertEquals(1, service2Logs.size());
        assertEquals("Service 2 message", service2Logs.get(0).getMessage());
    }
    
    @Test
    void testFindByTimestampBetween() {
        // Arrange
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        
        ApplicationLog oldLog = ApplicationLog.builder()
                .timestamp(yesterday)
                .logLevel("INFO")
                .service("test-service")
                .message("Old message")
                .source("test")
                .build();
        
        ApplicationLog newLog = ApplicationLog.builder()
                .timestamp(tomorrow)
                .logLevel("INFO")
                .service("test-service")
                .message("New message")
                .source("test")
                .build();
        
        entityManager.persist(oldLog);
        entityManager.persist(newLog);
        entityManager.flush();
        
        // Act
        List<ApplicationLog> recentLogs = applicationLogRepository.findByTimestampBetween(now.minusHours(1), now.plusHours(1));
        List<ApplicationLog> allLogs = applicationLogRepository.findByTimestampBetween(yesterday.minusHours(1), tomorrow.plusHours(1));
        
        // Assert
        assertEquals(0, recentLogs.size());
        assertEquals(2, allLogs.size());
    }
    
    @Test
    void testFindByMessageContaining() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        
        ApplicationLog log1 = ApplicationLog.builder()
                .timestamp(now)
                .logLevel("INFO")
                .service("test-service")
                .message("Connection established")
                .source("test")
                .build();
        
        ApplicationLog log2 = ApplicationLog.builder()
                .timestamp(now)
                .logLevel("ERROR")
                .service("test-service")
                .message("Connection refused")
                .source("test")
                .build();
        
        entityManager.persist(log1);
        entityManager.persist(log2);
        entityManager.flush();
        
        // Act
        List<ApplicationLog> connectionLogs = applicationLogRepository.findByMessageContaining("Connection");
        List<ApplicationLog> refusedLogs = applicationLogRepository.findByMessageContaining("refused");
        List<ApplicationLog> establishedLogs = applicationLogRepository.findByMessageContaining("established");
        
        // Assert
        assertEquals(2, connectionLogs.size());
        assertEquals(1, refusedLogs.size());
        assertEquals(1, establishedLogs.size());
    }
    
    @Test
    void testFindByLogLevelAndService() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        
        ApplicationLog log1 = ApplicationLog.builder()
                .timestamp(now)
                .logLevel("INFO")
                .service("service-1")
                .message("Info from service 1")
                .source("test")
                .build();
        
        ApplicationLog log2 = ApplicationLog.builder()
                .timestamp(now)
                .logLevel("ERROR")
                .service("service-1")
                .message("Error from service 1")
                .source("test")
                .build();
        
        ApplicationLog log3 = ApplicationLog.builder()
                .timestamp(now)
                .logLevel("INFO")
                .service("service-2")
                .message("Info from service 2")
                .source("test")
                .build();
        
        entityManager.persist(log1);
        entityManager.persist(log2);
        entityManager.persist(log3);
        entityManager.flush();
        
        // Act
        List<ApplicationLog> infoService1Logs = applicationLogRepository.findByLogLevelAndService("INFO", "service-1");
        List<ApplicationLog> errorService1Logs = applicationLogRepository.findByLogLevelAndService("ERROR", "service-1");
        List<ApplicationLog> infoService2Logs = applicationLogRepository.findByLogLevelAndService("INFO", "service-2");
        
        // Assert
        assertEquals(1, infoService1Logs.size());
        assertEquals("Info from service 1", infoService1Logs.get(0).getMessage());
        
        assertEquals(1, errorService1Logs.size());
        assertEquals("Error from service 1", errorService1Logs.get(0).getMessage());
        
        assertEquals(1, infoService2Logs.size());
        assertEquals("Info from service 2", infoService2Logs.get(0).getMessage());
    }
    
    @Test
    void testCountByLogLevel() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        
        ApplicationLog infoLog1 = ApplicationLog.builder()
                .timestamp(now)
                .logLevel("INFO")
                .service("test-service")
                .message("Info message 1")
                .source("test")
                .build();
        
        ApplicationLog infoLog2 = ApplicationLog.builder()
                .timestamp(now)
                .logLevel("INFO")
                .service("test-service")
                .message("Info message 2")
                .source("test")
                .build();
        
        ApplicationLog errorLog = ApplicationLog.builder()
                .timestamp(now)
                .logLevel("ERROR")
                .service("test-service")
                .message("Error message")
                .source("test")
                .build();
        
        entityManager.persist(infoLog1);
        entityManager.persist(infoLog2);
        entityManager.persist(errorLog);
        entityManager.flush();
        
        // Act
        List<Object[]> counts = applicationLogRepository.countByLogLevel();
        
        // Assert
        assertEquals(2, counts.size());
        
        // Find the INFO count
        Object[] infoCount = null;
        Object[] errorCount = null;
        for (Object[] count : counts) {
            if ("INFO".equals(count[0])) {
                infoCount = count;
            } else if ("ERROR".equals(count[0])) {
                errorCount = count;
            }
        }
        
        assertNotNull(infoCount);
        assertNotNull(errorCount);
        assertEquals(2L, infoCount[1]);
        assertEquals(1L, errorCount[1]);
    }
    
    @Test
    void testFindErrorLogsWithStackTrace() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        
        ApplicationLog errorLogWithStackTrace = ApplicationLog.builder()
                .timestamp(now)
                .logLevel("ERROR")
                .service("test-service")
                .message("Error with stack trace")
                .stackTrace("Stack trace")
                .source("test")
                .build();
        
        ApplicationLog errorLogWithoutStackTrace = ApplicationLog.builder()
                .timestamp(now)
                .logLevel("ERROR")
                .service("test-service")
                .message("Error without stack trace")
                .source("test")
                .build();
        
        entityManager.persist(errorLogWithStackTrace);
        entityManager.persist(errorLogWithoutStackTrace);
        entityManager.flush();
        
        // Act
        List<ApplicationLog> errorLogsWithStackTrace = applicationLogRepository.findErrorLogsWithStackTrace();
        
        // Assert
        assertEquals(1, errorLogsWithStackTrace.size());
        assertEquals("Error with stack trace", errorLogsWithStackTrace.get(0).getMessage());
        assertEquals("Stack trace", errorLogsWithStackTrace.get(0).getStackTrace());
    }
}