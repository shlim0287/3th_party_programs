# Kafka Consumer Module

## 개요
Kafka Consumer 모듈은 Kafka 토픽으로부터 메시지를 소비하고 처리하는 Spring Boot 애플리케이션입니다. 이 모듈은 다양한 소비 전략(단일, 배치, 대용량 배치)을 제공하며, 메시지 처리, 오류 처리, 모니터링 기능을 포함하고 있습니다.

## 기술 스택
- **Spring Boot**: 애플리케이션 프레임워크
- **Apache Kafka**: 분산 스트리밍 플랫폼
- **Spring Kafka**: Kafka와 Spring의 통합
- **Micrometer**: 애플리케이션 메트릭 수집
- **Prometheus**: 메트릭 모니터링
- **Lombok**: 보일러플레이트 코드 감소

## 주요 컴포넌트

### 1. 모델 (Model)
`Message` 클래스는 Kafka에서 수신하는 메시지의 데이터 구조를 정의합니다.

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private String id;                          // 메시지 고유 식별자
    private String content;                     // 메시지 내용
    private String type;                        // 메시지 유형 (INFO, WARNING, ERROR 등)
    private LocalDateTime timestamp;            // 메시지 생성 시간
    private String source;                      // 메시지 소스 시스템
    private java.util.Map<String, String> metadata; // 추가 메타데이터
}
```

### 2. 설정 (Configuration)
`KafkaConsumerConfig` 클래스는 Kafka 컨슈머 관련 설정을 담당합니다:

#### 주요 설정 항목:
- **컨슈머 팩토리**: Kafka 컨슈머 인스턴스 생성 설정
- **리스너 컨테이너 팩토리**: 다양한 소비 전략에 맞는 리스너 설정
  - 표준 배치 처리 리스너
  - 단일 메시지 처리 리스너
  - 대용량 배치 처리 리스너
- **오류 처리**: 메시지 처리 실패 시 재시도 및 오류 처리 전략

```java
@Configuration
@EnableKafka
@Slf4j
public class KafkaConsumerConfig {
    // 토픽 이름 상수
    public static final String TOPIC_NAME = "demo-topic";
    public static final String FEEDBACK_TOPIC_NAME = "feedback-topic";
    
    // 컨슈머 팩토리 Bean
    @Bean
    public ConsumerFactory<String, Message> consumerFactory() {
        // 컨슈머 설정
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        
        return new DefaultKafkaConsumerFactory<>(props, 
                new StringDeserializer(),
                new JsonDeserializer<>(Message.class, false));
    }
    
    // 표준 배치 리스너 컨테이너 팩토리 Bean
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Message> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Message> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(1000, 3)));
        
        return factory;
    }
    
    // 단일 메시지 리스너 컨테이너 팩토리 Bean
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Message> kafkaSingleListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Message> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setBatchListener(false);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(1000, 3)));
        
        return factory;
    }
    
    // 대용량 배치 리스너 컨테이너 팩토리 Bean
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Message> kafkaLargeBatchListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Message> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.getContainerProperties().setPollTimeout(5000);
        factory.setBatchErrorHandler(new BatchErrorHandler() {
            @Override
            public void handle(Exception thrownException, ConsumerRecords<?, ?> data) {
                log.error("Error in batch processing", thrownException);
                // 배치 오류 처리 로직
            }
        });
        
        return factory;
    }
}
```

### 3. 역직렬화 (Deserialization)
`MessageDeserializer` 클래스는 Kafka에서 수신한 JSON 메시지를 Message 객체로 역직렬화합니다:

```java
public class MessageDeserializer extends JsonDeserializer<Message> {
    public MessageDeserializer() {
        super(Message.class);
        this.addTrustedPackages("com.kafka_consumer.model");
    }
}
```

### 4. 서비스 (Service)

#### KafkaConsumerService
`KafkaConsumerService` 클래스는 Kafka에서 메시지를 소비하는 핵심 로직을 구현합니다:

```java
@Service
@Slf4j
public class KafkaConsumerService {
    private final MeterRegistry meterRegistry;
    private final MessageProcessingService messageProcessingService;
    private final KafkaTemplate<String, Message> kafkaTemplate;
    
    // 메트릭 카운터 및 타이머
    private final Counter messagesProcessedCounter;
    private final Counter messagesFailedCounter;
    private final Counter messagesFeedbackCounter;
    private final Timer messageProcessingTimer;
    
    // 배치 메시지 소비 리스너
    @KafkaListener(
            topics = KafkaConsumerConfig.TOPIC_NAME,
            containerFactory = "kafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeBatch(List<ConsumerRecord<String, Message>> records, Acknowledgment ack) {
        log.info("Received batch of {} records", records.size());
        
        // 배치 처리 로직
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            for (ConsumerRecord<String, Message> record : records) {
                processRecord(record);
            }
            
            // 모든 메시지 처리 후 커밋
            ack.acknowledge();
            log.info("Successfully processed batch of {} records", records.size());
            
        } catch (Exception e) {
            log.error("Error processing batch", e);
            messagesFailedCounter.increment(records.size());
            // 오류 처리 로직
        } finally {
            sample.stop(messageProcessingTimer);
        }
    }
    
    // 단일 메시지 소비 리스너
    @KafkaListener(
            topics = KafkaConsumerConfig.TOPIC_NAME,
            containerFactory = "kafkaSingleListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}-single"
    )
    public void consumeSingle(ConsumerRecord<String, Message> record, Acknowledgment ack) {
        log.info("Received single record: {}", record.key());
        
        // 단일 메시지 처리 로직
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            processRecord(record);
            ack.acknowledge();
            log.info("Successfully processed record: {}", record.key());
            
        } catch (Exception e) {
            log.error("Error processing record: {}", record.key(), e);
            messagesFailedCounter.increment();
            // 오류 처리 로직
        } finally {
            sample.stop(messageProcessingTimer);
        }
    }
    
    // 대용량 배치 메시지 소비 리스너
    @KafkaListener(
            topics = KafkaConsumerConfig.TOPIC_NAME,
            containerFactory = "kafkaLargeBatchListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}-large"
    )
    public void consumeLargeBatch(List<ConsumerRecord<String, Message>> records, Acknowledgment ack) {
        log.info("Received large batch of {} records", records.size());
        
        // 대용량 배치 처리 로직
        Timer.Sample sample = Timer.start(meterRegistry);
        List<ConsumerRecord<String, Message>> failedRecords = new ArrayList<>();
        
        try {
            // 병렬 처리
            records.parallelStream().forEach(record -> {
                try {
                    processRecord(record);
                } catch (Exception e) {
                    log.error("Error processing record in large batch: {}", record.key(), e);
                    synchronized (failedRecords) {
                        failedRecords.add(record);
                    }
                }
            });
            
            // 실패한 메시지 처리
            if (!failedRecords.isEmpty()) {
                handleFailedMessages(failedRecords);
            }
            
            // 모든 메시지 처리 후 커밋
            ack.acknowledge();
            log.info("Successfully processed large batch of {} records ({} failed)", 
                    records.size(), failedRecords.size());
            
        } catch (Exception e) {
            log.error("Error processing large batch", e);
            messagesFailedCounter.increment(records.size());
            // 오류 처리 로직
        } finally {
            sample.stop(messageProcessingTimer);
        }
    }
    
    // 메시지 처리 메서드
    private void processRecord(ConsumerRecord<String, Message> record) {
        Message message = record.value();
        
        if (message == null) {
            log.warn("Received null message from topic: {} partition: {} offset: {}", 
                    record.topic(), record.partition(), record.offset());
            return;
        }
        
        log.info("Processing message: {} from topic: {} partition: {} offset: {}", 
                message.getId(), record.topic(), record.partition(), record.offset());
        
        // 메시지 처리 서비스 호출
        messageProcessingService.processMessage(message);
        messagesProcessedCounter.increment();
        
        // 피드백 메시지 전송 (필요 시)
        sendFeedbackMessage(message);
    }
    
    // 피드백 메시지 전송 메서드
    @Transactional
    public CompletableFuture<SendResult<String, Message>> sendFeedbackMessage(Message originalMessage) {
        // 피드백 메시지 생성 및 전송 로직
        Message feedbackMessage = Message.builder()
                .id(UUID.randomUUID().toString())
                .content("Processed: " + originalMessage.getContent())
                .type("FEEDBACK")
                .timestamp(LocalDateTime.now())
                .source("consumer-feedback")
                .build();
        
        log.info("Sending feedback message for original message id: {}", originalMessage.getId());
        messagesFeedbackCounter.increment();
        
        return kafkaTemplate.send(KafkaConsumerConfig.FEEDBACK_TOPIC_NAME, 
                originalMessage.getId(), feedbackMessage);
    }
}
```

#### MessageProcessingService
`MessageProcessingService` 클래스는 Kafka에서 수신한 메시지를 처리하는 비즈니스 로직을 구현합니다:

```java
@Service
public class MessageProcessingService {
    private final static Logger log = getLogger(MessageProcessingService.class);
    
    // 처리된 메시지 저장소 (예시용)
    private final ConcurrentMap<String, Message> processedMessages = new ConcurrentHashMap<>();
    
    // 메시지 처리 메서드
    public void processMessage(Message message) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        log.info("Processing message: {}", message);
        
        // 메시지 유형에 따른 처리
        switch (message.getType() != null ? message.getType().toUpperCase() : "INFO") {
            case "INFO":
                processInfoMessage(message);
                break;
            case "WARNING":
                processWarningMessage(message);
                break;
            case "ERROR":
                processErrorMessage(message);
                break;
            default:
                processDefaultMessage(message);
        }
        
        // 처리된 메시지 저장
        processedMessages.put(message.getId(), message);
        
        log.info("Successfully processed message with id: {}", message.getId());
    }
    
    // 처리된 메시지 조회 메서드
    public Message getProcessedMessage(String id) {
        return processedMessages.get(id);
    }
    
    // 처리된 메시지 수 조회 메서드
    public int getProcessedMessageCount() {
        return processedMessages.size();
    }
    
    // 메시지 유형별 처리 메서드들
    private void processInfoMessage(Message message) {
        log.info("Processing INFO message: {}", message.getContent());
        // 실제 비즈니스 로직 구현
    }
    
    private void processWarningMessage(Message message) {
        log.warn("Processing WARNING message: {}", message.getContent());
        // 실제 비즈니스 로직 구현
    }
    
    private void processErrorMessage(Message message) {
        log.error("Processing ERROR message: {}", message.getContent());
        // 실제 비즈니스 로직 구현
    }
    
    private void processDefaultMessage(Message message) {
        log.info("Processing message with unknown type: {}", message.getContent());
        // 실제 비즈니스 로직 구현
    }
}
```

### 5. 컨트롤러 (Controller)
`KafkaConsumerController` 클래스는 REST API를 통해 처리된 메시지 정보를 제공합니다:

```java
@RestController
@RequestMapping("/api/messages")
public class KafkaConsumerController {
    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerController.class);
    private final MessageProcessingService messageProcessingService;
    
    // 특정 메시지 조회 엔드포인트
    @GetMapping("/{id}")
    @Timed(value = "api.message.get", description = "Time taken to retrieve a message")
    public ResponseEntity<Message> getMessage(@PathVariable String id) {
        log.info("Received request to get message with id: {}", id);
        
        Message message = messageProcessingService.getProcessedMessage(id);
        
        if (message == null) {
            log.warn("Message with id: {} not found", id);
            return ResponseEntity.notFound().build();
        }
        
        log.info("Retrieved message with id: {}", id);
        return ResponseEntity.ok(message);
    }
    
    // 메시지 통계 조회 엔드포인트
    @GetMapping("/stats")
    @Timed(value = "api.message.stats", description = "Time taken to retrieve message statistics")
    public ResponseEntity<Map<String, Object>> getStats() {
        log.info("Received request to get message statistics");
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("processedCount", messageProcessingService.getProcessedMessageCount());
        stats.put("timestamp", LocalDateTime.now());
        
        log.info("Retrieved message statistics: {}", stats);
        return ResponseEntity.ok(stats);
    }
    
    // 헬스 체크 엔드포인트
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(health);
    }
}
```

## 설정 파일 (application.yml)

애플리케이션의 설정은 `application.yml` 파일에 정의되어 있으며, 다음과 같은 주요 설정을 포함합니다:

### Kafka 컨슈머 설정
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:29092
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      group-id: ${spring.application.name}-consumer-group
      auto-offset-reset: earliest   # 초기 오프셋 위치
      enable-auto-commit: false     # 자동 커밋 비활성화 (수동 커밋 사용)
      max-poll-records: 500         # 한 번에 가져올 최대 레코드 수
```

### Kafka 리스너 설정
```yaml
spring:
  kafka:
    listener:
      ack-mode: MANUAL_IMMEDIATE    # 수동 커밋 모드
      concurrency: 3                # 리스너 스레드 수
      poll-timeout: 5000            # 폴링 타임아웃 (ms)
      type: BATCH                   # 리스너 타입 (SINGLE, BATCH)
```

### 서버 설정
```yaml
server:
  port: 8081                        # 서버 포트 (프로듀서와 다름)
  shutdown: graceful                # 우아한 종료
```

### 모니터링 설정 (Actuator)
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true
```

## Kafka 컨슈머 개념 설명

### 컨슈머 그룹 (Consumer Group)
컨슈머 그룹은 같은 토픽을 구독하는 컨슈머들의 집합입니다. 주요 특징은 다음과 같습니다:

- **병렬 처리**: 여러 컨슈머가 토픽의 파티션을 분담하여 병렬로 메시지 처리
- **로드 밸런싱**: 파티션은 그룹 내 컨슈머들에게 자동으로 분배됨
- **고가용성**: 컨슈머가 실패하면 해당 파티션은 다른 컨슈머에게 재할당됨
- **확장성**: 컨슈머를 추가하여 처리량 향상 가능 (파티션 수까지)

### 오프셋 관리 (Offset Management)
오프셋은 파티션 내에서 메시지의 위치를 나타내는 숫자입니다:

- **자동 커밋**: `enable-auto-commit: true`로 설정 시 주기적으로 오프셋 자동 커밋
- **수동 커밋**: `enable-auto-commit: false`로 설정 시 명시적으로 오프셋 커밋 필요
- **커밋 모드**: 
  - `RECORD`: 각 레코드 처리 후 커밋
  - `BATCH`: 배치 처리 후 커밋
  - `TIME`: 일정 시간마다 커밋
  - `COUNT`: 일정 개수마다 커밋
  - `MANUAL`: 수동 커밋
  - `MANUAL_IMMEDIATE`: 즉시 수동 커밋

### 배치 처리 (Batch Processing)
배치 처리는 여러 메시지를 한 번에 처리하는 방식입니다:

- **효율성**: 네트워크 및 처리 오버헤드 감소
- **처리량**: 단일 메시지 처리보다 높은 처리량 달성
- **설정**: `spring.kafka.listener.type: BATCH`로 설정
- **배치 크기**: `spring.kafka.consumer.max-poll-records`로 설정

### 오류 처리 (Error Handling)
메시지 처리 중 오류 발생 시 대응 방법:

- **재시도**: `DefaultErrorHandler`와 `FixedBackOff`를 사용하여 재시도 설정
- **데드 레터 토픽**: 처리 실패한 메시지를 별도 토픽으로 이동
- **오류 로깅**: 오류 정보 기록 및 모니터링
- **트랜잭션**: 메시지 처리와 오프셋 커밋을 원자적으로 처리

## 사용 방법

### 1. 처리된 메시지 조회 (REST API)

#### 특정 메시지 조회
```bash
curl -X GET http://localhost:8081/api/messages/{message-id}
```

#### 메시지 통계 조회
```bash
curl -X GET http://localhost:8081/api/messages/stats
```

#### 헬스 체크
```bash
curl -X GET http://localhost:8081/api/messages/health
```

### 2. 메시지 소비 확인
Kafka 컨슈머는 자동으로 토픽에서 메시지를 소비합니다. 로그를 통해 메시지 소비 및 처리 상태를 확인할 수 있습니다:

```
tail -f logs/application.log
```

## 모니터링

### Prometheus 메트릭
다음 엔드포인트에서 Prometheus 형식의 메트릭을 확인할 수 있습니다:
```
http://localhost:8081/actuator/prometheus
```

### 주요 메트릭
- `kafka.consumer.messages.processed`: 성공적으로 처리된 메시지 수
- `kafka.consumer.messages.failed`: 처리 실패한 메시지 수
- `kafka.consumer.messages.feedback`: 피드백으로 전송된 메시지 수
- `kafka.consumer.message.processing.time`: 메시지 처리에 소요된 시간

## 트러블슈팅

### 일반적인 문제 해결

#### 1. 메시지 소비가 되지 않음
```
No messages consumed from topic
```
- 토픽 존재 여부 확인: `kafka-topics.sh --list --bootstrap-server localhost:29092`
- 컨슈머 그룹 상태 확인: `kafka-consumer-groups.sh --describe --bootstrap-server localhost:29092 --group kafka_consumer-consumer-group`
- 오프셋 리셋 고려: `auto-offset-reset: earliest` 설정 확인

#### 2. 역직렬화 오류
```
Failed to deserialize message
```
- 메시지 형식 확인: 프로듀서와 컨슈머의 메시지 형식이 일치하는지 확인
- 역직렬화 설정 확인: `spring.kafka.consumer.value-deserializer` 설정 확인
- 신뢰할 수 있는 패키지 설정: `spring.kafka.consumer.properties.spring.json.trusted.packages` 설정 확인

#### 3. 커밋 오류
```
Failed to commit offset
```
- 커밋 모드 확인: `spring.kafka.listener.ack-mode` 설정 확인
- 수동 커밋 코드 확인: `ack.acknowledge()` 호출 여부 확인
- 브로커 연결 상태 확인

## 성능 최적화 팁

1. **배치 크기 조정**: `max-poll-records` 설정을 처리량에 맞게 조정
2. **컨슈머 수 증가**: `concurrency` 설정을 증가시켜 병렬 처리 향상
3. **폴링 간격 조정**: `fetch-max-wait` 설정을 조정하여 지연 시간과 처리량 균형 조정
4. **병렬 스트림 활용**: 대용량 배치 처리 시 `parallelStream()`을 사용하여 CPU 활용도 향상
5. **메모리 설정 최적화**: JVM 힙 메모리 설정을 처리량에 맞게 조정

## 보안 고려사항

1. **SSL/TLS 암호화**: 프로덕션 환경에서는 SSL/TLS 암호화 사용 권장
2. **SASL 인증**: 사용자 인증을 위한 SASL 메커니즘 활용
3. **ACL 설정**: 토픽별 접근 제어 설정
4. **네트워크 분리**: Kafka 클러스터를 별도의 네트워크 세그먼트에 배치
5. **민감 정보 처리**: 메시지에 포함된 민감 정보 처리 시 암호화 또는 마스킹 고려