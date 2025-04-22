package com.logs_analytics.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity class for storing Nginx access logs.
 * Maps to the nginx_access_logs table in the database.
 */
@Entity
@Table(name = "nginx_access_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NginxAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_ip")
    private String clientIp;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "request_method")
    private String requestMethod;

    @Column(name = "request_path", length = 1024)
    private String requestPath;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "response_size")
    private Long responseSize;

    @Column(name = "referrer", length = 1024)
    private String referrer;

    @Column(name = "user_agent", length = 1024)
    private String userAgent;

    @Column(name = "response_time")
    private Double responseTime;

    @Column(name = "country")
    private String country;

    @Column(name = "hour_of_day")
    private Integer hourOfDay;

    @Column(name = "response_time_bucket")
    private String responseTimeBucket;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}