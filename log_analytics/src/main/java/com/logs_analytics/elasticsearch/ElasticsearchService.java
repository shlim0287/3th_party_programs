package com.logs_analytics.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.JsonData;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for Elasticsearch operations.
 * Sets up indices, mappings, and machine learning jobs.
 */
@Service
@Slf4j
public class ElasticsearchService {

    private final ElasticsearchClient elasticsearchClient;

    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUri;

    /**
     * Constructor that initializes the Elasticsearch client.
     * 
     * @param elasticsearchClient the Elasticsearch client
     */
    @Autowired
    public ElasticsearchService(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
        log.info("ElasticsearchService initialized with client: {}", elasticsearchClient);
    }

    /**
     * Initialize Elasticsearch indices and machine learning jobs.
     */
    @PostConstruct
    public void init() {
        try {
            createIndicesIfNotExist();
            setupMachineLearningJobs();
        } catch (Exception e) {
            log.error("Error initializing Elasticsearch: {}", e.getMessage(), e);
        }
    }

    /**
     * Creates Elasticsearch indices if they don't exist.
     */
    private void createIndicesIfNotExist() throws IOException {
        // Create nginx access logs index
        if (!indexExists("nginx-access")) {
            createNginxAccessIndex();
        }

        // Create application logs index
        if (!indexExists("application-logs")) {
            createApplicationLogsIndex();
        }

        // Create system metrics index
        if (!indexExists("system-metrics")) {
            createSystemMetricsIndex();
        }
    }

    /**
     * Checks if an index exists.
     */
    private boolean indexExists(String indexName) throws IOException {
        return elasticsearchClient.indices().exists(
            new ExistsRequest.Builder().index(indexName).build()
        ).value();
    }

    /**
     * Creates the nginx access logs index with mappings.
     */
    private void createNginxAccessIndex() throws IOException {
        // Index settings
        Map<String, JsonData> settings = new HashMap<>();
        settings.put("index.number_of_shards", JsonData.of(1));
        settings.put("index.number_of_replicas", JsonData.of(0));

        // Index mappings
        Map<String, Map<String, Object>> properties = new HashMap<>();

        // clientip field
        Map<String, Object> clientip = new HashMap<>();
        clientip.put("type", "ip");
        properties.put("clientip", clientip);

        // timestamp field
        Map<String, Object> timestamp = new HashMap<>();
        timestamp.put("type", "date");
        properties.put("timestamp", timestamp);

        // request_method field
        Map<String, Object> requestMethod = new HashMap<>();
        requestMethod.put("type", "keyword");
        properties.put("request_method", requestMethod);

        // request_path field
        Map<String, Object> requestPath = new HashMap<>();
        requestPath.put("type", "text");
        requestPath.put("analyzer", "standard");
        properties.put("request_path", requestPath);

        // status_code field
        Map<String, Object> statusCode = new HashMap<>();
        statusCode.put("type", "integer");
        properties.put("status_code", statusCode);

        // response_size field
        Map<String, Object> responseSize = new HashMap<>();
        responseSize.put("type", "long");
        properties.put("response_size", responseSize);

        // response_time field
        Map<String, Object> responseTime = new HashMap<>();
        responseTime.put("type", "float");
        properties.put("response_time", responseTime);

        // hour_of_day field
        Map<String, Object> hourOfDay = new HashMap<>();
        hourOfDay.put("type", "integer");
        properties.put("hour_of_day", hourOfDay);

        // response_time_bucket field
        Map<String, Object> responseTimeBucket = new HashMap<>();
        responseTimeBucket.put("type", "keyword");
        properties.put("response_time_bucket", responseTimeBucket);

        // country field
        Map<String, Object> country = new HashMap<>();
        country.put("type", "keyword");
        properties.put("country", country);

        // Create mapping
        Map<String, Object> mapping = new HashMap<>();
        mapping.put("properties", properties);

        // Create index with settings and mappings
        CreateIndexResponse createIndexResponse = elasticsearchClient.indices().create(builder -> 
            builder
                .index("nginx-access")
                .settings(s -> {
                    s.numberOfShards("1");
                    s.numberOfReplicas("0");
                    return s;
                })
                .mappings(m -> {
                    m.properties("clientip", p -> p.ip(ip -> ip));
                    m.properties("timestamp", p -> p.date(d -> d));
                    m.properties("request_method", p -> p.keyword(k -> k));
                    m.properties("request_path", p -> p.text(t -> t.analyzer("standard")));
                    m.properties("status_code", p -> p.integer(i -> i));
                    m.properties("response_size", p -> p.long_(l -> l));
                    m.properties("response_time", p -> p.float_(f -> f));
                    m.properties("hour_of_day", p -> p.integer(i -> i));
                    m.properties("response_time_bucket", p -> p.keyword(k -> k));
                    m.properties("country", p -> p.keyword(k -> k));
                    return m;
                })
        );

        log.info("Index created: {}, acknowledged: {}", "nginx-access", createIndexResponse.acknowledged());
    }

    /**
     * Creates the application logs index with mappings.
     */
    private void createApplicationLogsIndex() throws IOException {
        // Index settings
        Map<String, JsonData> settings = new HashMap<>();
        settings.put("index.number_of_shards", JsonData.of(1));
        settings.put("index.number_of_replicas", JsonData.of(0));

        // Index mappings
        Map<String, Map<String, Object>> properties = new HashMap<>();

        // timestamp field
        Map<String, Object> timestamp = new HashMap<>();
        timestamp.put("type", "date");
        properties.put("timestamp", timestamp);

        // log_level field
        Map<String, Object> logLevel = new HashMap<>();
        logLevel.put("type", "keyword");
        properties.put("log_level", logLevel);

        // service field
        Map<String, Object> service = new HashMap<>();
        service.put("type", "keyword");
        properties.put("service", service);

        // message field
        Map<String, Object> message = new HashMap<>();
        message.put("type", "text");
        message.put("analyzer", "standard");
        properties.put("message", message);

        // stack_trace field
        Map<String, Object> stackTrace = new HashMap<>();
        stackTrace.put("type", "text");
        stackTrace.put("analyzer", "standard");
        properties.put("stack_trace", stackTrace);

        // Create mapping
        Map<String, Object> mapping = new HashMap<>();
        mapping.put("properties", properties);

        // Create index with settings and mappings
        CreateIndexResponse createIndexResponse = elasticsearchClient.indices().create(builder -> 
            builder
                .index("application-logs")
                .settings(s -> {
                    s.numberOfShards("1");
                    s.numberOfReplicas("0");
                    return s;
                })
                .mappings(m -> {
                    m.properties("timestamp", p -> p.date(d -> d));
                    m.properties("log_level", p -> p.keyword(k -> k));
                    m.properties("service", p -> p.keyword(k -> k));
                    m.properties("message", p -> p.text(t -> t.analyzer("standard")));
                    m.properties("stack_trace", p -> p.text(t -> t.analyzer("standard")));
                    return m;
                })
        );

        log.info("Index created: {}, acknowledged: {}", "application-logs", createIndexResponse.acknowledged());
    }

    /**
     * Creates the system metrics index with mappings.
     */
    private void createSystemMetricsIndex() throws IOException {
        // Index settings
        Map<String, JsonData> settings = new HashMap<>();
        settings.put("index.number_of_shards", JsonData.of(1));
        settings.put("index.number_of_replicas", JsonData.of(0));

        // Index mappings
        Map<String, Map<String, Object>> properties = new HashMap<>();

        // timestamp field
        Map<String, Object> timestamp = new HashMap<>();
        timestamp.put("type", "date");
        properties.put("timestamp", timestamp);

        // host field
        Map<String, Object> host = new HashMap<>();
        host.put("type", "keyword");
        properties.put("host", host);

        // metric_type field
        Map<String, Object> metricType = new HashMap<>();
        metricType.put("type", "keyword");
        properties.put("metric_type", metricType);

        // metric_name field
        Map<String, Object> metricName = new HashMap<>();
        metricName.put("type", "keyword");
        properties.put("metric_name", metricName);

        // metric_value field
        Map<String, Object> metricValue = new HashMap<>();
        metricValue.put("type", "float");
        properties.put("metric_value", metricValue);

        // cpu_usage field
        Map<String, Object> cpuUsage = new HashMap<>();
        cpuUsage.put("type", "float");
        properties.put("cpu_usage", cpuUsage);

        // memory_usage field
        Map<String, Object> memoryUsage = new HashMap<>();
        memoryUsage.put("type", "float");
        properties.put("memory_usage", memoryUsage);

        // disk_usage field
        Map<String, Object> diskUsage = new HashMap<>();
        diskUsage.put("type", "float");
        properties.put("disk_usage", diskUsage);

        // network_in field
        Map<String, Object> networkIn = new HashMap<>();
        networkIn.put("type", "float");
        properties.put("network_in", networkIn);

        // network_out field
        Map<String, Object> networkOut = new HashMap<>();
        networkOut.put("type", "float");
        properties.put("network_out", networkOut);

        // Create mapping
        Map<String, Object> mapping = new HashMap<>();
        mapping.put("properties", properties);

        // Create index with settings and mappings
        CreateIndexResponse createIndexResponse = elasticsearchClient.indices().create(builder -> 
            builder
                .index("system-metrics")
                .settings(s -> {
                    s.numberOfShards("1");
                    s.numberOfReplicas("0");
                    return s;
                })
                .mappings(m -> {
                    m.properties("timestamp", p -> p.date(d -> d));
                    m.properties("host", p -> p.keyword(k -> k));
                    m.properties("metric_type", p -> p.keyword(k -> k));
                    m.properties("metric_name", p -> p.keyword(k -> k));
                    m.properties("metric_value", p -> p.float_(f -> f));
                    m.properties("cpu_usage", p -> p.float_(f -> f));
                    m.properties("memory_usage", p -> p.float_(f -> f));
                    m.properties("disk_usage", p -> p.float_(f -> f));
                    m.properties("network_in", p -> p.float_(f -> f));
                    m.properties("network_out", p -> p.float_(f -> f));
                    return m;
                })
        );

        log.info("Index created: {}, acknowledged: {}", "system-metrics", createIndexResponse.acknowledged());
    }

    /**
     * Sets up machine learning jobs for anomaly detection.
     */
    private void setupMachineLearningJobs() {
        try {
            // Setup machine learning job for response time anomaly detection
            setupResponseTimeAnomalyDetection();

            // Setup machine learning job for error rate anomaly detection
            setupErrorRateAnomalyDetection();

            // Setup machine learning job for system metrics anomaly detection
            setupSystemMetricsAnomalyDetection();
        } catch (Exception e) {
            log.error("Error setting up machine learning jobs: {}", e.getMessage(), e);
        }
    }

    /**
     * Sets up a machine learning job for response time anomaly detection.
     */
    private void setupResponseTimeAnomalyDetection() {
        // This would use the Elasticsearch X-Pack Machine Learning API
        // For demonstration purposes, we'll just log the setup
        log.info("Setting up machine learning job for response time anomaly detection");

        // In a real implementation, this would create a job using the Elasticsearch API
        // Example: PUT _ml/anomaly_detectors/nginx_response_time_job
    }

    /**
     * Sets up a machine learning job for error rate anomaly detection.
     */
    private void setupErrorRateAnomalyDetection() {
        // This would use the Elasticsearch X-Pack Machine Learning API
        // For demonstration purposes, we'll just log the setup
        log.info("Setting up machine learning job for error rate anomaly detection");

        // In a real implementation, this would create a job using the Elasticsearch API
        // Example: PUT _ml/anomaly_detectors/error_rate_job
    }

    /**
     * Sets up a machine learning job for system metrics anomaly detection.
     */
    private void setupSystemMetricsAnomalyDetection() {
        // This would use the Elasticsearch X-Pack Machine Learning API
        // For demonstration purposes, we'll just log the setup
        log.info("Setting up machine learning job for system metrics anomaly detection");

        // In a real implementation, this would create a job using the Elasticsearch API
        // Example: PUT _ml/anomaly_detectors/system_metrics_job
    }
}
