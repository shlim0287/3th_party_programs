package com.logs_analytics.repository;

import com.logs_analytics.model.SystemMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for SystemMetric entity.
 * Provides methods for accessing and manipulating system metrics in the database.
 */
@Repository
public interface SystemMetricRepository extends JpaRepository<SystemMetric, Long> {

    /**
     * Find metrics by host.
     */
    List<SystemMetric> findByHost(String host);

    /**
     * Find metrics by metric type.
     */
    List<SystemMetric> findByMetricType(String metricType);

    /**
     * Find metrics by time range.
     */
    List<SystemMetric> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find metrics by host and metric type.
     */
    List<SystemMetric> findByHostAndMetricType(String host, String metricType);

    /**
     * Find average CPU usage by host.
     */
    @Query("SELECT s.host, AVG(s.cpuUsage) FROM SystemMetric s WHERE s.cpuUsage IS NOT NULL GROUP BY s.host")
    List<Object[]> findAverageCpuUsageByHost();

    /**
     * Find average memory usage by host.
     */
    @Query("SELECT s.host, AVG(s.memoryUsage) FROM SystemMetric s WHERE s.memoryUsage IS NOT NULL GROUP BY s.host")
    List<Object[]> findAverageMemoryUsageByHost();

    /**
     * Find average disk usage by host.
     */
    @Query("SELECT s.host, AVG(s.diskUsage) FROM SystemMetric s WHERE s.diskUsage IS NOT NULL GROUP BY s.host")
    List<Object[]> findAverageDiskUsageByHost();

    /**
     * Find total network traffic by host.
     */
    @Query("SELECT s.host, SUM(s.networkIn), SUM(s.networkOut) FROM SystemMetric s WHERE s.networkIn IS NOT NULL AND s.networkOut IS NOT NULL GROUP BY s.host")
    List<Object[]> findTotalNetworkTrafficByHost();

    /**
     * Find latest metrics for each host.
     */
    @Query("SELECT s FROM SystemMetric s WHERE s.timestamp = (SELECT MAX(s2.timestamp) FROM SystemMetric s2 WHERE s2.host = s.host)")
    List<SystemMetric> findLatestMetricsForEachHost();
}