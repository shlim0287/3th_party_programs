package com.logs_analytics.scheduler;

import com.logs_analytics.elasticsearch.ElasticsearchService;
import com.logs_analytics.model.ApplicationLog;
import com.logs_analytics.model.NginxAccessLog;
import com.logs_analytics.model.SystemMetric;
import com.logs_analytics.repository.ApplicationLogRepository;
import com.logs_analytics.repository.NginxAccessLogRepository;
import com.logs_analytics.repository.SystemMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * sLLM 모듈을 위한 로그 데이터 스케줄러
 * 주기적으로 로그 데이터를 수집하여 sLLM 모듈로 전송합니다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LogDataScheduler {

    private final ApplicationLogRepository applicationLogRepository;
    private final NginxAccessLogRepository nginxAccessLogRepository;
    private final SystemMetricRepository systemMetricRepository;
    private final ElasticsearchService elasticsearchService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${sllm.kafka.topic:sllm-data}")
    private String sllmTopic;

    @Value("${sllm.batch.size:100}")
    private int batchSize;

    private LocalDateTime lastProcessedTime = LocalDateTime.now().minusDays(1);

    /**
     * 매일 새벽 1시에 로그 데이터를 수집하여 sLLM 모듈로 전송합니다.
     * cron 표현식: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void scheduledDataProcessing() {
        log.info("스케줄링된 로그 데이터 처리 시작: {}", LocalDateTime.now());
        processLogData();
    }

    /**
     * 로그 데이터를 수집하여 sLLM 모듈로 전송합니다.
     */
    public void processLogData() {
        try {
            LocalDateTime now = LocalDateTime.now();
            log.info("로그 데이터 처리: {} ~ {}", lastProcessedTime, now);

            // 애플리케이션 로그 처리
            processApplicationLogs(lastProcessedTime, now);

            // Nginx 액세스 로그 처리
            processNginxLogs(lastProcessedTime, now);

            // 시스템 메트릭 처리
            processSystemMetrics(lastProcessedTime, now);

            // 마지막 처리 시간 업데이트
            lastProcessedTime = now;
            log.info("로그 데이터 처리 완료. 다음 처리 시간: {}", lastProcessedTime);
        } catch (Exception e) {
            log.error("로그 데이터 처리 중 오류 발생", e);
        }
    }

    /**
     * 애플리케이션 로그를 처리합니다.
     *
     * @param startTime 시작 시간
     * @param endTime   종료 시간
     */
    private void processApplicationLogs(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            // 데이터베이스에서 로그 조회
            List<ApplicationLog> logs = applicationLogRepository.findByCreatedAtBetween(startTime, endTime);
            log.info("애플리케이션 로그 {}개 조회됨", logs.size());

            // 배치 처리
            for (int i = 0; i < logs.size(); i += batchSize) {
                int end = Math.min(i + batchSize, logs.size());
                List<ApplicationLog> batch = logs.subList(i, end);

                // Kafka로 전송
                sendToSllm("application", batch);
                log.info("애플리케이션 로그 배치 전송: {}/{}", end, logs.size());
            }
        } catch (Exception e) {
            log.error("애플리케이션 로그 처리 중 오류 발생", e);
        }
    }

    /**
     * Nginx 액세스 로그를 처리합니다.
     *
     * @param startTime 시작 시간
     * @param endTime   종료 시간
     */
    private void processNginxLogs(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            // 데이터베이스에서 로그 조회
            List<NginxAccessLog> logs = nginxAccessLogRepository.findByCreatedAtBetween(startTime, endTime);
            log.info("Nginx 액세스 로그 {}개 조회됨", logs.size());

            // 배치 처리
            for (int i = 0; i < logs.size(); i += batchSize) {
                int end = Math.min(i + batchSize, logs.size());
                List<NginxAccessLog> batch = logs.subList(i, end);

                // Kafka로 전송
                sendToSllm("nginx-access", batch);
                log.info("Nginx 액세스 로그 배치 전송: {}/{}", end, logs.size());
            }
        } catch (Exception e) {
            log.error("Nginx 액세스 로그 처리 중 오류 발생", e);
        }
    }

    /**
     * 시스템 메트릭을 처리합니다.
     *
     * @param startTime 시작 시간
     * @param endTime   종료 시간
     */
    private void processSystemMetrics(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            // 데이터베이스에서 메트릭 조회
            List<SystemMetric> metrics = systemMetricRepository.findByCreatedAtBetween(startTime, endTime);
            log.info("시스템 메트릭 {}개 조회됨", metrics.size());

            // 배치 처리
            for (int i = 0; i < metrics.size(); i += batchSize) {
                int end = Math.min(i + batchSize, metrics.size());
                List<SystemMetric> batch = metrics.subList(i, end);

                // Kafka로 전송
                sendToSllm("beats", batch);
                log.info("시스템 메트릭 배치 전송: {}/{}", end, metrics.size());
            }
        } catch (Exception e) {
            log.error("시스템 메트릭 처리 중 오류 발생", e);
        }
    }

    /**
     * 데이터를 sLLM 모듈로 전송합니다.
     *
     * @param type 데이터 유형
     * @param data 전송할 데이터
     */
    private void sendToSllm(String type, List<?> data) {
        if (data.isEmpty()) {
            return;
        }

        try {
            // 메타데이터 생성
            Map<String, Object> message = new HashMap<>();
            message.put("type", type);
            message.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            message.put("count", data.size());
            message.put("data", data);

            // Kafka로 전송
            kafkaTemplate.send(sllmTopic, message).get();
            log.info("sLLM 모듈로 데이터 전송 완료: 유형={}, 개수={}", type, data.size());
        } catch (Exception e) {
            log.error("sLLM 모듈로 데이터 전송 중 오류 발생", e);
        }
    }
}