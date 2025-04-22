package com.logs_analytics.kibana;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


/**
 * Service for Kibana dashboard setup.
 * Provides instructions for setting up Kibana dashboards.
 */
@Service
@Slf4j
public class KibanaDashboardService {

    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUri;

    /**
     * Initialize Kibana dashboard setup instructions.
     */
    @PostConstruct
    public void init() {
        logKibanaDashboardInstructions();
    }

    /**
     * Logs instructions for setting up Kibana dashboards.
     */
    private void logKibanaDashboardInstructions() {
        log.info("=== Kibana Dashboard Setup Instructions ===");
        log.info("1. Access Kibana at http://localhost:5601");
        log.info("2. Create index patterns for the following indices:");
        log.info("   - nginx-access-*");
        log.info("   - application-logs-*");
        log.info("   - system-metrics-*");
        log.info("3. Set up the following dashboards:");
        
        logWebTrafficDashboardInstructions();
        logApplicationPerformanceDashboardInstructions();
        logSystemMonitoringDashboardInstructions();
        logMachineLearningDashboardInstructions();
    }

    /**
     * Logs instructions for setting up the Web Traffic dashboard.
     */
    private void logWebTrafficDashboardInstructions() {
        log.info("=== Web Traffic Dashboard ===");
        log.info("1. Create a new dashboard named 'Web Traffic Dashboard'");
        log.info("2. Add the following visualizations:");
        log.info("   a. Requests Over Time (Line chart)");
        log.info("      - X-axis: timestamp");
        log.info("      - Y-axis: count()");
        log.info("   b. Status Code Distribution (Pie chart)");
        log.info("      - Slice by: status_code");
        log.info("      - Size by: count()");
        log.info("   c. Top 10 Request Paths (Horizontal bar chart)");
        log.info("      - Y-axis: request_path.keyword");
        log.info("      - X-axis: count()");
        log.info("      - Sort by: count() descending");
        log.info("      - Size: 10");
        log.info("   d. Geographic Distribution (Coordinate map)");
        log.info("      - Metrics: count()");
        log.info("      - Buckets: Geohash on clientip.geo");
        log.info("   e. Response Time by Hour of Day (Line chart)");
        log.info("      - X-axis: hour_of_day");
        log.info("      - Y-axis: avg(response_time)");
        log.info("   f. Response Time Buckets (Pie chart)");
        log.info("      - Slice by: response_time_bucket");
        log.info("      - Size by: count()");
    }

    /**
     * Logs instructions for setting up the Application Performance dashboard.
     */
    private void logApplicationPerformanceDashboardInstructions() {
        log.info("=== Application Performance Dashboard ===");
        log.info("1. Create a new dashboard named 'Application Performance Dashboard'");
        log.info("2. Add the following visualizations:");
        log.info("   a. Log Levels Distribution (Pie chart)");
        log.info("      - Slice by: log_level");
        log.info("      - Size by: count()");
        log.info("   b. Logs Over Time (Line chart)");
        log.info("      - X-axis: timestamp");
        log.info("      - Y-axis: count()");
        log.info("      - Split series: log_level");
        log.info("   c. Top Services with Errors (Horizontal bar chart)");
        log.info("      - Filter: log_level = 'ERROR'");
        log.info("      - Y-axis: service.keyword");
        log.info("      - X-axis: count()");
        log.info("      - Sort by: count() descending");
        log.info("   d. Recent Error Messages (Data table)");
        log.info("      - Filter: log_level = 'ERROR'");
        log.info("      - Columns: timestamp, service, message");
        log.info("      - Sort by: timestamp descending");
    }

    /**
     * Logs instructions for setting up the System Monitoring dashboard.
     */
    private void logSystemMonitoringDashboardInstructions() {
        log.info("=== System Monitoring Dashboard ===");
        log.info("1. Create a new dashboard named 'System Monitoring Dashboard'");
        log.info("2. Add the following visualizations:");
        log.info("   a. CPU Usage by Host (Line chart)");
        log.info("      - X-axis: timestamp");
        log.info("      - Y-axis: avg(cpu_usage)");
        log.info("      - Split series: host.keyword");
        log.info("   b. Memory Usage by Host (Line chart)");
        log.info("      - X-axis: timestamp");
        log.info("      - Y-axis: avg(memory_usage)");
        log.info("      - Split series: host.keyword");
        log.info("   c. Disk Usage by Host (Gauge)");
        log.info("      - Metrics: avg(disk_usage)");
        log.info("      - Group by: host.keyword");
        log.info("   d. Network Traffic by Host (Area chart)");
        log.info("      - X-axis: timestamp");
        log.info("      - Y-axis: sum(network_in), sum(network_out)");
        log.info("      - Split series: host.keyword");
    }

    /**
     * Logs instructions for setting up the Machine Learning dashboard.
     */
    private void logMachineLearningDashboardInstructions() {
        log.info("=== Machine Learning Dashboard ===");
        log.info("1. Create a new dashboard named 'Machine Learning Dashboard'");
        log.info("2. Set up machine learning jobs in Kibana:");
        log.info("   a. Response Time Anomaly Detection");
        log.info("      - Go to Machine Learning > Anomaly Detection");
        log.info("      - Create new job > Single metric");
        log.info("      - Index pattern: nginx-access-*");
        log.info("      - Time field: timestamp");
        log.info("      - Job name: nginx_response_time_anomaly");
        log.info("      - Function: avg");
        log.info("      - Field: response_time");
        log.info("      - Bucket span: 15m");
        log.info("      - Create job and start datafeeds");
        log.info("   b. Error Rate Anomaly Detection");
        log.info("      - Go to Machine Learning > Anomaly Detection");
        log.info("      - Create new job > Single metric");
        log.info("      - Index pattern: application-logs-*");
        log.info("      - Time field: timestamp");
        log.info("      - Job name: error_rate_anomaly");
        log.info("      - Filter: log_level = 'ERROR'");
        log.info("      - Function: count");
        log.info("      - Bucket span: 15m");
        log.info("      - Create job and start datafeeds");
        log.info("   c. System Metrics Anomaly Detection");
        log.info("      - Go to Machine Learning > Anomaly Detection");
        log.info("      - Create new job > Multi metric");
        log.info("      - Index pattern: system-metrics-*");
        log.info("      - Time field: timestamp");
        log.info("      - Job name: system_metrics_anomaly");
        log.info("      - Split data by: host.keyword");
        log.info("      - Add metrics: avg(cpu_usage), avg(memory_usage), avg(disk_usage)");
        log.info("      - Bucket span: 15m");
        log.info("      - Create job and start datafeeds");
        log.info("3. Add the following visualizations to the dashboard:");
        log.info("   a. Response Time Anomalies (Anomaly chart)");
        log.info("      - Job: nginx_response_time_anomaly");
        log.info("   b. Error Rate Anomalies (Anomaly chart)");
        log.info("      - Job: error_rate_anomaly");
        log.info("   c. System Metrics Anomalies (Anomaly chart)");
        log.info("      - Job: system_metrics_anomaly");
    }
}