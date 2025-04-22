package com.logs_analytics.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity class for storing application logs.
 * Maps to the application_logs table in the database.
 */
@Entity
@Table(name = "application_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "log_level")
    private String logLevel;

    @Column(name = "service")
    private String service;

    @Column(name = "message", length = 4096)
    private String message;

    @Column(name = "stack_trace", length = 8192)
    private String stackTrace;

    @Column(name = "source")
    private String source;

    @Column(name = "thread_name")
    private String threadName;

    @Column(name = "logger_name")
    private String loggerName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}