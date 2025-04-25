package com.logs_analytics.repository;

import com.logs_analytics.model.ApplicationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for ApplicationLog entity.
 * Provides methods for accessing and manipulating application logs in the database.
 */
@Repository
public interface ApplicationLogRepository extends JpaRepository<ApplicationLog, Long> {

    /**
     * Find logs by log level.
     */
    List<ApplicationLog> findByLogLevel(String logLevel);

    /**
     * Find logs by service.
     */
    List<ApplicationLog> findByService(String service);

    /**
     * Find logs by time range.
     */
    List<ApplicationLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find logs containing a specific message.
     */
    List<ApplicationLog> findByMessageContaining(String message);

    /**
     * Find logs by log level and service.
     */
    List<ApplicationLog> findByLogLevelAndService(String logLevel, String service);

    /**
     * Count logs by log level.
     */
    @Query("SELECT l.logLevel, COUNT(l) FROM ApplicationLog l GROUP BY l.logLevel")
    List<Object[]> countByLogLevel();

    /**
     * Find error logs with stack trace.
     */
    @Query("SELECT l FROM ApplicationLog l WHERE l.logLevel = 'ERROR' AND l.stackTrace IS NOT NULL")
    List<ApplicationLog> findErrorLogsWithStackTrace();

    /**
     * Count logs by service.
     */
    @Query("SELECT l.service, COUNT(l) FROM ApplicationLog l GROUP BY l.service")
    List<Object[]> countByService();

    /**
     * Find most recent error logs.
     */
    List<ApplicationLog> findTop10ByLogLevelOrderByTimestampDesc(String logLevel);

    /**
     * Find logs by created at time range.
     */
    List<ApplicationLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
