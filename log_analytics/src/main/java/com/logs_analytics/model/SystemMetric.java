package com.logs_analytics.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity class for storing system metrics from Beats.
 * Maps to the system_metrics table in the database.
 */
@Entity
@Table(name = "system_metrics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "host")
    private String host;

    @Column(name = "metric_type")
    private String metricType;

    @Column(name = "metric_name")
    private String metricName;

    @Column(name = "metric_value")
    private Double metricValue;

    @Column(name = "cpu_usage")
    private Double cpuUsage;

    @Column(name = "memory_usage")
    private Double memoryUsage;

    @Column(name = "disk_usage")
    private Double diskUsage;

    @Column(name = "network_in")
    private Double networkIn;

    @Column(name = "network_out")
    private Double networkOut;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}