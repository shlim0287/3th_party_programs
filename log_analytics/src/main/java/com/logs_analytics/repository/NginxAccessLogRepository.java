package com.logs_analytics.repository;

import com.logs_analytics.model.NginxAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for NginxAccessLog entity.
 * Provides methods for accessing and manipulating nginx access logs in the database.
 */
@Repository
public interface NginxAccessLogRepository extends JpaRepository<NginxAccessLog, Long> {

    /**
     * Find logs by client IP address.
     */
    List<NginxAccessLog> findByClientIp(String clientIp);

    /**
     * Find logs by status code.
     */
    List<NginxAccessLog> findByStatusCode(Integer statusCode);

    /**
     * Find logs by time range.
     */
    List<NginxAccessLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find logs by response time bucket.
     */
    List<NginxAccessLog> findByResponseTimeBucket(String responseTimeBucket);

    /**
     * Find logs by hour of day.
     */
    List<NginxAccessLog> findByHourOfDay(Integer hourOfDay);

    /**
     * Count logs by status code.
     */
    @Query("SELECT l.statusCode, COUNT(l) FROM NginxAccessLog l GROUP BY l.statusCode")
    List<Object[]> countByStatusCode();

    /**
     * Find average response time by hour of day.
     */
    @Query("SELECT l.hourOfDay, AVG(l.responseTime) FROM NginxAccessLog l GROUP BY l.hourOfDay ORDER BY l.hourOfDay")
    List<Object[]> findAverageResponseTimeByHourOfDay();

    /**
     * Find top 10 request paths by count.
     */
    @Query("SELECT l.requestPath, COUNT(l) FROM NginxAccessLog l GROUP BY l.requestPath ORDER BY COUNT(l) DESC")
    List<Object[]> findTop10RequestPaths();
}