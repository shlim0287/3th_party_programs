# Log Analytics Module

## 개요
Log Analytics 모듈은 다양한 소스(애플리케이션 로그, Nginx 액세스 로그, 시스템 메트릭)에서 로그 데이터를 수집, 저장, 분석하는 Spring Boot 애플리케이션입니다. 이 모듈은 Kafka에서 로그 데이터를 소비하여 MySQL에 저장하고, Elasticsearch를 통해 고급 분석 및 시각화 기능을 제공합니다.

## 기술 스택
- **Spring Boot**: 애플리케이션 프레임워크
- **Apache Kafka**: 로그 데이터 스트리밍
- **MySQL**: 로그 데이터 영구 저장
- **Elasticsearch**: 로그 데이터 검색 및 분석
- **Kibana**: 로그 데이터 시각화
- **Spring Data JPA**: 데이터베이스 액세스
- **Lombok**: 보일러플레이트 코드 감소

## 주요 컴포넌트

### 1. 모델 (Model)
로그 데이터를 표현하는 세 가지 주요 엔티티 클래스가 있습니다:

#### ApplicationLog
애플리케이션 로그를 표현하는 엔티티입니다.

```java
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
    
    private LocalDateTime timestamp;
    private String logLevel;
    private String service;
    private String message;
    private String stackTrace;
    private String source;
    private String threadName;
    private String loggerName;
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

#### NginxAccessLog
Nginx 웹 서버의 액세스 로그를 표현하는 엔티티입니다.

```java
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
    
    private String clientIp;
    private LocalDateTime timestamp;
    private String requestMethod;
    private String requestPath;
    private Integer statusCode;
    private Long responseSize;
    private String referrer;
    private String userAgent;
    private Double responseTime;
    private String country;
    private Integer hourOfDay;
    private String responseTimeBucket;
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

#### SystemMetric
시스템 성능 메트릭을 표현하는 엔티티입니다.

```java
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
    
    private LocalDateTime timestamp;
    private String host;
    private String metricType;
    private String metricName;
    private Double metricValue;
    private Double cpuUsage;
    private Double memoryUsage;
    private Double diskUsage;
    private Double networkIn;
    private Double networkOut;
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

### 2. Kafka 컨슈머 (Kafka Consumers)
Kafka 토픽에서 로그 데이터를 소비하는 세 가지 컨슈머 클래스가 있습니다:

#### ApplicationLogConsumer
애플리케이션 로그를 소비하고 처리합니다.

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class ApplicationLogConsumer {
    private final ApplicationLogRepository repository;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(topics = "application-logs", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message, Acknowledgment ack) {
        try {
            log.debug("Received application log message: {}", message);
            JsonNode jsonNode = objectMapper.readTree(message);
            
            ApplicationLog applicationLog = ApplicationLog.builder()
                    .timestamp(parseTimestamp(jsonNode))
                    .logLevel(getTextValue(jsonNode, "log_level"))
                    .service(getTextValue(jsonNode, "service"))
                    .message(getTextValue(jsonNode, "log_message"))
                    .stackTrace(getTextValue(jsonNode, "stack_trace"))
                    .source(getTextValue(jsonNode, "source"))
                    .threadName(getTextValue(jsonNode, "thread_name"))
                    .loggerName(getTextValue(jsonNode, "logger_name"))
                    .build();
            
            repository.save(applicationLog);
            log.info("Saved application log: {}", applicationLog.getId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing application log message: {}", message, e);
            ack.acknowledge();
        }
    }
    
    // 타임스탬프 파싱 및 기타 유틸리티 메서드
}
```

#### NginxAccessLogConsumer
Nginx 액세스 로그를 소비하고 처리합니다.

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class NginxAccessLogConsumer {
    private final NginxAccessLogRepository repository;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(topics = "nginx-access-logs", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message, Acknowledgment ack) {
        try {
            log.debug("Received nginx access log message: {}", message);
            JsonNode jsonNode = objectMapper.readTree(message);
            
            NginxAccessLog nginxAccessLog = NginxAccessLog.builder()
                    .clientIp(getTextValue(jsonNode, "client_ip"))
                    .timestamp(parseTimestamp(jsonNode))
                    .requestMethod(getTextValue(jsonNode, "request_method"))
                    .requestPath(getTextValue(jsonNode, "request_path"))
                    .statusCode(getIntValue(jsonNode, "status_code"))
                    .responseSize(getLongValue(jsonNode, "response_size"))
                    .referrer(getTextValue(jsonNode, "referrer"))
                    .userAgent(getTextValue(jsonNode, "user_agent"))
                    .responseTime(getDoubleValue(jsonNode, "response_time"))
                    .country(getTextValue(jsonNode, "country"))
                    .hourOfDay(getIntValue(jsonNode, "hour_of_day"))
                    .responseTimeBucket(getTextValue(jsonNode, "response_time_bucket"))
                    .build();
            
            repository.save(nginxAccessLog);
            log.info("Saved nginx access log: {}", nginxAccessLog.getId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing nginx access log message: {}", message, e);
            ack.acknowledge();
        }
    }
    
    // 타임스탬프 파싱 및 기타 유틸리티 메서드
}
```

#### SystemMetricConsumer
시스템 메트릭을 소비하고 처리합니다.

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class SystemMetricConsumer {
    private final SystemMetricRepository repository;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(topics = "beats-logs", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message, Acknowledgment ack) {
        try {
            log.debug("Received system metric message: {}", message);
            JsonNode jsonNode = objectMapper.readTree(message);
            
            SystemMetric systemMetric = SystemMetric.builder()
                    .timestamp(parseTimestamp(jsonNode))
                    .host(getTextValue(jsonNode, "host"))
                    .metricType(getTextValue(jsonNode, "metric_type"))
                    .metricName(getTextValue(jsonNode, "metric_name"))
                    .metricValue(getDoubleValue(jsonNode, "metric_value"))
                    .cpuUsage(getDoubleValue(jsonNode, "cpu_usage"))
                    .memoryUsage(getDoubleValue(jsonNode, "memory_usage"))
                    .diskUsage(getDoubleValue(jsonNode, "disk_usage"))
                    .networkIn(getDoubleValue(jsonNode, "network_in"))
                    .networkOut(getDoubleValue(jsonNode, "network_out"))
                    .build();
            
            repository.save(systemMetric);
            log.info("Saved system metric: {}", systemMetric.getId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing system metric message: {}", message, e);
            ack.acknowledge();
        }
    }
    
    // 타임스탬프 파싱 및 기타 유틸리티 메서드
}
```

### 3. Elasticsearch 서비스 (Elasticsearch Service)
Elasticsearch 인덱스를 설정하고 관리하는 서비스입니다.

```java
@Service
@Slf4j
public class ElasticsearchService {
    private final ElasticsearchClient elasticsearchClient;
    
    @PostConstruct
    public void init() {
        try {
            createIndicesIfNotExist();
            setupMachineLearningJobs();
        } catch (Exception e) {
            log.error("Error initializing Elasticsearch", e);
        }
    }
    
    // Elasticsearch 인덱스 생성 메서드
    private void createIndicesIfNotExist() throws IOException {
        if (!indexExists("nginx-access")) {
            createNginxAccessIndex();
        }
        
        if (!indexExists("application-logs")) {
            createApplicationLogsIndex();
        }
        
        if (!indexExists("system-metrics")) {
            createSystemMetricsIndex();
        }
    }
    
    // 머신러닝 작업 설정 메서드
    private void setupMachineLearningJobs() {
        setupResponseTimeAnomalyDetection();
        setupErrorRateAnomalyDetection();
        setupSystemMetricsAnomalyDetection();
    }
    
    // 각 인덱스 및 머신러닝 작업 설정을 위한 메서드들
}
```

### 4. Kibana 대시보드 서비스 (Kibana Dashboard Service)
Kibana 대시보드 설정을 위한 지침을 제공하는 서비스입니다.

```java
@Service
@Slf4j
public class KibanaDashboardService {
    @PostConstruct
    public void init() {
        logKibanaDashboardInstructions();
    }
    
    private void logKibanaDashboardInstructions() {
        log.info("=== Kibana Dashboard Setup Instructions ===");
        // 웹 트래픽 대시보드 설정 지침
        logWebTrafficDashboardInstructions();
        // 애플리케이션 성능 대시보드 설정 지침
        logApplicationPerformanceDashboardInstructions();
        // 시스템 모니터링 대시보드 설정 지침
        logSystemMonitoringDashboardInstructions();
        // 머신러닝 대시보드 설정 지침
        logMachineLearningDashboardInstructions();
    }
    
    // 각 대시보드 설정 지침을 위한 메서드들
}
```

### 5. 리포지토리 (Repositories)
JPA를 사용하여 로그 데이터를 데이터베이스에 저장하고 조회하는 리포지토리 인터페이스입니다.

```java
public interface ApplicationLogRepository extends JpaRepository<ApplicationLog, Long> {
    List<ApplicationLog> findByLogLevel(String logLevel);
    List<ApplicationLog> findByServiceAndTimestampBetween(String service, LocalDateTime start, LocalDateTime end);
}

public interface NginxAccessLogRepository extends JpaRepository<NginxAccessLog, Long> {
    List<NginxAccessLog> findByStatusCodeBetween(int minStatus, int maxStatus);
    List<NginxAccessLog> findByResponseTimeBucket(String bucket);
}

public interface SystemMetricRepository extends JpaRepository<SystemMetric, Long> {
    List<SystemMetric> findByHostAndMetricType(String host, String metricType);
    List<SystemMetric> findByCpuUsageGreaterThan(double threshold);
}
```

## 설정 파일 (application.yml)

애플리케이션의 설정은 `application.yml` 파일에 정의되어 있으며, 다음과 같은 주요 설정을 포함합니다:

### JPA 및 데이터소스 설정
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true
    show-sql: true
    open-in-view: false
  
  datasource:
    url: jdbc:mysql://localhost:3306/logs_analytics?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: loguser
    password: logpassword
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
```

### Kafka 설정
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:29092
    consumer:
      group-id: log-analytics-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
    listener:
      ack-mode: MANUAL_IMMEDIATE
      concurrency: 3
```

### Elasticsearch 설정
```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
    connection-timeout: 1s
    socket-timeout: 30s
```

### 서버 설정
```yaml
server:
  port: 8082
  servlet:
    context-path: /api
```

## ELK 스택 개념 설명

### ELK 스택이란?
ELK 스택은 Elasticsearch, Logstash, Kibana의 세 가지 오픈 소스 프로젝트를 조합한 로그 관리 및 분석 솔루션입니다. 최근에는 Beats가 추가되어 ELKB 스택이라고도 합니다.

#### Elasticsearch
Elasticsearch는 분산형 RESTful 검색 및 분석 엔진입니다. 주요 특징은 다음과 같습니다:

- **분산 아키텍처**: 여러 노드에 데이터를 분산 저장하여 확장성과 고가용성 제공
- **실시간 검색**: 거의 실시간으로 데이터 색인 및 검색 가능
- **전문 검색**: 텍스트 데이터에 대한 강력한 전문 검색 기능
- **분석 기능**: 집계, 통계, 머신러닝 등 다양한 분석 기능 제공
- **스키마리스**: 유연한 데이터 모델로 구조화되지 않은 데이터도 처리 가능

#### Logstash
Logstash는 다양한 소스에서 데이터를 수집, 변환하여 Elasticsearch와 같은 저장소로 전송하는 데이터 처리 파이프라인입니다:

- **입력 플러그인**: 파일, Kafka, Beats 등 다양한 소스에서 데이터 수집
- **필터 플러그인**: 데이터 파싱, 변환, 구조화, 보강 등 처리
- **출력 플러그인**: Elasticsearch, Kafka 등 다양한 대상으로 데이터 전송
- **코덱**: 데이터 인코딩/디코딩 (JSON, CSV 등)

#### Kibana
Kibana는 Elasticsearch 데이터를 시각화하고 탐색하기 위한 대시보드 플랫폼입니다:

- **데이터 시각화**: 차트, 그래프, 지도 등 다양한 시각화 도구 제공
- **대시보드**: 여러 시각화를 조합하여 종합적인 대시보드 구성
- **검색 기능**: Elasticsearch 데이터를 직관적으로 검색하고 필터링
- **머신러닝**: 이상 탐지, 예측 등 머신러닝 기능 제공
- **모니터링**: Elasticsearch 클러스터 상태 모니터링

#### Beats
Beats는 데이터를 수집하여 Elasticsearch나 Logstash로 전송하는 경량 에이전트입니다:

- **Filebeat**: 로그 파일 수집
- **Metricbeat**: 시스템 및 서비스 메트릭 수집
- **Packetbeat**: 네트워크 패킷 데이터 수집
- **Heartbeat**: 시스템 가용성 모니터링
- **Auditbeat**: 감사 데이터 수집
- **Winlogbeat**: Windows 이벤트 로그 수집

### ELK 스택의 데이터 흐름
1. **데이터 수집**: Beats 또는 Logstash가 다양한 소스에서 데이터 수집
2. **데이터 처리**: Logstash가 데이터를 파싱, 변환, 보강
3. **데이터 저장**: 처리된 데이터가 Elasticsearch에 저장
4. **데이터 시각화**: Kibana를 통해 데이터 시각화 및 분석

## 로그 분석 개념 설명

### 로그 분석이란?
로그 분석은 시스템, 애플리케이션, 네트워크 장비 등에서 생성된 로그 데이터를 수집, 처리, 분석하여 유용한 정보를 추출하는 과정입니다. 주요 목적은 다음과 같습니다:

- **문제 해결**: 오류 및 장애 원인 파악
- **성능 모니터링**: 시스템 및 애플리케이션 성능 추적
- **보안 모니터링**: 보안 위협 및 이상 행동 탐지
- **비즈니스 인사이트**: 사용자 행동 및 트렌드 분석
- **규정 준수**: 감사 및 규정 준수 요구사항 충족

### 로그 분석 방법론

#### 1. 로그 수집
- **중앙 집중화**: 모든 로그를 중앙 저장소에 수집
- **표준화**: 다양한 형식의 로그를 표준 형식으로 변환
- **실시간 수집**: 지연 없이 로그 데이터 수집

#### 2. 로그 처리
- **파싱**: 비구조화된 로그를 구조화된 형식으로 변환
- **필터링**: 관련 없는 데이터 제거
- **보강**: 추가 정보(지리적 위치, IP 정보 등)로 로그 데이터 보강
- **정규화**: 일관된 형식으로 데이터 변환

#### 3. 로그 분석
- **검색 및 쿼리**: 특정 조건에 맞는 로그 검색
- **집계 및 통계**: 로그 데이터에 대한 통계 분석
- **상관 관계 분석**: 여러 로그 간의 관계 분석
- **이상 탐지**: 비정상적인 패턴 식별
- **머신러닝**: 자동화된 패턴 인식 및 예측

#### 4. 로그 시각화
- **대시보드**: 주요 지표를 한눈에 볼 수 있는 대시보드
- **차트 및 그래프**: 시간에 따른 추세 시각화
- **히트맵**: 데이터 밀도 시각화
- **지리적 시각화**: 지리적 분포 시각화

### 로그 분석의 주요 사용 사례

#### 1. 애플리케이션 모니터링
- 오류 및 예외 추적
- 성능 병목 현상 식별
- 사용자 경험 모니터링

#### 2. 인프라 모니터링
- 서버 리소스 사용량 추적
- 네트워크 트래픽 분석
- 하드웨어 장애 예측

#### 3. 보안 모니터링
- 침입 탐지
- 이상 행동 식별
- 보안 사고 대응

#### 4. 비즈니스 인텔리전스
- 사용자 행동 분석
- 전환율 및 이탈률 추적
- 제품 사용 패턴 분석

## 사용 방법

### 1. 로그 데이터 흐름 설정
1. 애플리케이션에서 Kafka로 로그 전송
2. Logstash를 통해 로그 처리 및 Elasticsearch로 전송
3. Log Analytics 모듈에서 Kafka로부터 로그 소비 및 MySQL에 저장
4. Kibana를 통해 로그 데이터 시각화 및 분석

### 2. Kibana 대시보드 설정
KibanaDashboardService에서 제공하는 지침에 따라 다음 대시보드를 설정합니다:

1. 웹 트래픽 대시보드
2. 애플리케이션 성능 대시보드
3. 시스템 모니터링 대시보드
4. 머신러닝 대시보드

### 3. 로그 분석 수행
Kibana를 통해 다음과 같은 분석을 수행할 수 있습니다:

- 오류 발생 추세 분석
- 웹 트래픽 패턴 분석
- 시스템 리소스 사용량 추적
- 이상 탐지 및 알림 설정

## 모니터링

### Prometheus 메트릭
다음 엔드포인트에서 Prometheus 형식의 메트릭을 확인할 수 있습니다:
```
http://localhost:8082/api/actuator/prometheus
```

### 주요 메트릭
- `log_analytics.application_logs.processed`: 처리된 애플리케이션 로그 수
- `log_analytics.nginx_logs.processed`: 처리된 Nginx 액세스 로그 수
- `log_analytics.system_metrics.processed`: 처리된 시스템 메트릭 수
- `log_analytics.processing.time`: 로그 처리에 소요된 시간

## 트러블슈팅

### 일반적인 문제 해결

#### 1. 로그 데이터가 수집되지 않음
- Kafka 토픽 확인: `kafka-topics.sh --list --bootstrap-server localhost:29092`
- Kafka 컨슈머 그룹 상태 확인: `kafka-consumer-groups.sh --describe --bootstrap-server localhost:29092 --group log-analytics-group`
- Logstash 파이프라인 상태 확인: `curl -X GET "localhost:9600/_node/stats/pipelines?pretty"`

#### 2. 데이터베이스 연결 오류
- MySQL 서버 실행 여부 확인
- 데이터베이스 접속 정보 확인: `application.yml`의 `spring.datasource` 설정
- 데이터베이스 사용자 권한 확인

#### 3. Elasticsearch 연결 오류
- Elasticsearch 서버 실행 여부 확인: `curl -X GET "localhost:9200/_cluster/health?pretty"`
- Elasticsearch 접속 정보 확인: `application.yml`의 `spring.elasticsearch` 설정
- Elasticsearch 인덱스 확인: `curl -X GET "localhost:9200/_cat/indices?v"`

## 성능 최적화 팁

1. **배치 처리 활용**: 대량의 로그 처리 시 배치 처리 활용
2. **인덱스 최적화**: MySQL 및 Elasticsearch 인덱스 최적화
3. **커넥션 풀 조정**: 데이터베이스 커넥션 풀 설정 최적화
4. **캐싱 활용**: 자주 조회되는 데이터 캐싱
5. **JVM 메모리 설정**: 애플리케이션 메모리 사용량에 맞게 JVM 설정 조정

## 보안 고려사항

1. **데이터 암호화**: 민감한 로그 데이터 암호화
2. **접근 제어**: Elasticsearch 및 Kibana 접근 제어 설정
3. **인증 및 권한 부여**: 적절한 인증 및 권한 부여 메커니즘 구현
4. **데이터 보존 정책**: 로그 데이터 보존 기간 설정
5. **감사 로깅**: 시스템 접근 및 변경 사항 감사 로깅