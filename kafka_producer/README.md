# Kafka Producer Module

## 개요
Kafka Producer 모듈은 메시지를 생성하여 Kafka 토픽으로 전송하는 Spring Boot 애플리케이션입니다. 이 모듈은 고가용성, 확장성, 신뢰성을 갖춘 메시지 생산자 역할을 수행하며, 다양한 고급 기능(재시도, 트랜잭션, 모니터링 등)을 포함하고 있습니다.

## 기술 스택
- **Spring Boot**: 애플리케이션 프레임워크
- **Apache Kafka**: 분산 스트리밍 플랫폼
- **Spring Kafka**: Kafka와 Spring의 통합
- **Micrometer**: 애플리케이션 메트릭 수집
- **Prometheus**: 메트릭 모니터링
- **Lombok**: 보일러플레이트 코드 감소

## 주요 컴포넌트

### 1. 모델 (Model)
`Message` 클래스는 Kafka로 전송되는 메시지의 데이터 구조를 정의합니다.

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
`KafkaProducerConfig` 클래스는 Kafka 프로듀서 관련 설정을 담당합니다:

#### 주요 설정 항목:
- **토픽 생성**: 애플리케이션에서 사용할 토픽 정의 및 생성
- **프로듀서 팩토리**: Kafka 프로듀서 인스턴스 생성 설정
- **Kafka 템플릿**: 메시지 전송을 위한 고수준 API 제공
- **컨슈머 설정**: 피드백 메시지 수신을 위한 컨슈머 설정
- **리스너 컨테이너 팩토리**: Kafka 리스너 설정

```java
@Configuration
@Slf4j
public class KafkaProducerConfig {
    // 토픽 이름 상수
    public static final String TOPIC_NAME = "demo-topic";
    public static final String FEEDBACK_TOPIC_NAME = "feedback-topic";

    // 토픽 생성 Bean
    @Bean
    public NewTopic demoTopic() {
        // 토픽 생성 로직
        return TopicBuilder.name(TOPIC_NAME)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic feedbackTopic() {
        // 피드백 토픽 생성 로직
        return TopicBuilder.name(FEEDBACK_TOPIC_NAME)
                .partitions(3)
                .replicas(1)
                .build();
    }

    // 프로듀서 팩토리 Bean
    @Bean
    public ProducerFactory<String, Message> producerFactory() {
        // 프로듀서 설정
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    // Kafka 템플릿 Bean
    @Bean
    public KafkaTemplate<String, Message> kafkaTemplate() {
        // Kafka 템플릿 생성
        return new KafkaTemplate<>(producerFactory());
    }

    // 컨슈머 팩토리 Bean (피드백용)
    @Bean
    public ConsumerFactory<String, Message> consumerFactory() {
        // 컨슈머 설정
        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
    }

    // 리스너 컨테이너 팩토리 Bean
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Message> kafkaListenerContainerFactory() {
        // 리스너 컨테이너 팩토리 설정
        ConcurrentKafkaListenerContainerFactory<String, Message> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
```

### 3. 서비스 (Service)
`KafkaProducerService` 클래스는 Kafka로 메시지를 전송하는 핵심 로직을 구현합니다:

#### 주요 기능:
- **메시지 전송**: 비동기적으로 메시지를 Kafka로 전송
- **재시도 로직**: 일시적인 오류 발생 시 자동 재시도
- **트랜잭션 지원**: 메시지 전송의 원자성 보장
- **메트릭 수집**: 메시지 전송 성공/실패 및 성능 지표 수집
- **피드백 수신**: 피드백 토픽으로부터 메시지 수신 및 처리

```java
@Slf4j
@Service
public class KafkaProducerService {
    // Kafka 템플릿 및 메트릭 레지스트리
    private final KafkaTemplate<String, Message> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    // 메트릭 카운터 및 타이머
    private final Counter messagesSentCounter;
    private final Counter messagesFailedCounter;
    private final Counter messagesReceivedCounter;
    private final Timer messageSendTimer;

    // 메시지 전송 메서드 (키 지정)
    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2))
    @Transactional
    public CompletableFuture<SendResult<String, Message>> sendMessage(String key, Message message) {
        // 메시지 전송 로직
        return kafkaTemplate.send(TOPIC_NAME, key, message);
    }

    // 메시지 전송 메서드 (키 자동 생성)
    @Transactional
    public CompletableFuture<SendResult<String, Message>> sendMessage(Message message) {
        // 랜덤 키로 메시지 전송
        return sendMessage(UUID.randomUUID().toString(), message);
    }

    // 피드백 메시지 수신 리스너
    @KafkaListener(topics = KafkaProducerConfig.FEEDBACK_TOPIC_NAME, 
                  containerFactory = "kafkaListenerContainerFactory")
    public void consumeFeedback(ConsumerRecord<String, Message> record, Acknowledgment ack) {
        // 피드백 메시지 처리 로직
        log.info("Received feedback: {}", record.value());
        ack.acknowledge();
    }
}
```

### 4. 컨트롤러 (Controller)
`KafkaProducerController` 클래스는 REST API를 통해 메시지 전송 기능을 노출합니다:

#### 주요 엔드포인트:
- **POST /api/messages**: 전체 Message 객체를 Kafka로 전송
- **POST /api/messages/simple**: 간단한 텍스트 메시지를 Kafka로 전송

```java
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerController {
    private final KafkaProducerService kafkaProducerService;

    // 전체 메시지 전송 엔드포인트
    @PostMapping
    @Timed(value = "api.message.send", description = "Time taken to process message send request")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody Message message) {
        // 메시지 전송 및 응답 생성 로직
        kafkaProducerService.sendMessage(message);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    // 간단한 메시지 전송 엔드포인트
    @PostMapping("/simple")
    @Timed(value = "api.message.send.simple", description = "Time taken to process simple message send request")
    public ResponseEntity<Map<String, Object>> sendSimpleMessage(@RequestParam String content, 
                                                               @RequestParam(required = false) String type) {
        // 간단한 메시지 생성 및 전송 로직
        Message message = Message.builder()
                .content(content)
                .type(type != null ? type : "INFO")
                .build();
        return sendMessage(message);
    }
}
```

### 5. 직렬화 (Serialization)
`MessageSerializer` 클래스는 Message 객체를 Kafka로 전송하기 위한 직렬화를 담당합니다:

```java
public class MessageSerializer extends JsonSerializer<Message> {
    // Message 객체를 JSON으로 직렬화
}
```

## 설정 파일 (application.yml)

애플리케이션의 설정은 `application.yml` 파일에 정의되어 있으며, 다음과 같은 주요 설정을 포함합니다:

### Kafka 프로듀서 설정
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:29092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all                     # 모든 복제본이 메시지를 받았는지 확인
      retries: 3                    # 전송 실패 시 재시도 횟수
      batch-size: 16384             # 배치 크기 (bytes)
      compression-type: snappy      # 압축 타입
      enable-idempotence: true      # 멱등성 활성화 (중복 전송 방지)
```

### Kafka 컨슈머 설정 (피드백용)
```yaml
spring:
  kafka:
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      group-id: ${spring.application.name}-consumer-group
      auto-offset-reset: earliest   # 초기 오프셋 위치
      enable-auto-commit: false     # 자동 커밋 비활성화 (수동 커밋 사용)
```

### 토픽 설정
```yaml
spring:
  kafka:
    topic:
      demo-topic:                   # 애플리케이션에서 사용하는 토픽 이름
        partitions: 3               # 파티션 수
        replication-factor: 1       # 복제 팩터 (개발 환경에서는 1, 프로덕션에서는 최소 3 권장)
        configs:                    # 토픽별 설정
          retention.ms: 604800000   # 데이터 보존 기간 (7일)
          min.insync.replicas: 1    # 최소 동기화 복제본 수
```

### 모니터링 설정 (Actuator)
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

## Kafka 개념 설명

### Kafka란?
Apache Kafka는 고성능, 고가용성, 확장성을 갖춘 분산 스트리밍 플랫폼입니다. 주요 특징은 다음과 같습니다:

- **분산 시스템**: 여러 서버(브로커)에 걸쳐 데이터를 분산 저장하여 확장성과 내결함성 제공
- **실시간 처리**: 데이터를 실시간으로 처리할 수 있는 스트리밍 기능 제공
- **높은 처리량**: 초당 수백만 개의 메시지를 처리할 수 있는 고성능 아키텍처
- **영속성**: 디스크에 데이터를 저장하여 내구성 보장
- **복제**: 데이터를 여러 브로커에 복제하여 고가용성 제공

### 주요 개념

#### 1. 토픽 (Topic)
- 메시지가 저장되는 논리적인 채널
- 여러 파티션으로 분할되어 병렬 처리 가능
- 각 메시지는 토픽 내에서 순차적으로 저장됨

#### 2. 파티션 (Partition)
- 토픽을 여러 부분으로 나눈 물리적 단위
- 각 파티션은 순서가 보장된 메시지 시퀀스
- 파티션 수를 늘려 처리량 향상 가능

#### 3. 프로듀서 (Producer)
- 토픽에 메시지를 발행하는 클라이언트
- 메시지 키를 기반으로 특정 파티션에 메시지 할당
- 배치 처리, 압축, 재시도 등의 기능 제공

#### 4. 컨슈머 (Consumer)
- 토픽에서 메시지를 구독하는 클라이언트
- 컨슈머 그룹을 통해 병렬 처리 가능
- 오프셋을 통해 메시지 소비 위치 관리

#### 5. 브로커 (Broker)
- Kafka 서버 인스턴스
- 토픽의 파티션을 저장하고 관리
- 클라이언트 요청 처리

#### 6. 주키퍼 (ZooKeeper)
- Kafka 클러스터 관리 및 조정
- 브로커 상태 모니터링
- 토픽 구성 정보 저장

## 사용 방법

### 1. 메시지 전송 (REST API)

#### 전체 메시지 전송
```bash
curl -X POST http://localhost:8080/api/messages \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Hello, Kafka!",
    "type": "INFO",
    "source": "api-test",
    "metadata": {
      "key1": "value1",
      "key2": "value2"
    }
  }'
```

#### 간단한 메시지 전송
```bash
curl -X POST "http://localhost:8080/api/messages/simple?content=Hello%20Kafka&type=INFO"
```

### 2. 메시지 확인
Kafka 토픽에 저장된 메시지는 다음과 같은 방법으로 확인할 수 있습니다:

#### Kafka CLI 사용
```bash
kafka-console-consumer.sh --bootstrap-server localhost:29092 --topic demo-topic --from-beginning
```

#### Kafka UI 도구 사용
- Kafka UI: http://localhost:8080/kafka-ui/
- Conduktor: https://www.conduktor.io/

## 모니터링

### Prometheus 메트릭
다음 엔드포인트에서 Prometheus 형식의 메트릭을 확인할 수 있습니다:
```
http://localhost:8080/actuator/prometheus
```

### 주요 메트릭
- `kafka.producer.messages.sent`: 성공적으로 전송된 메시지 수
- `kafka.producer.messages.failed`: 전송 실패한 메시지 수
- `kafka.producer.message.send.time`: 메시지 전송에 소요된 시간
- `kafka.producer.messages.received`: 피드백 토픽에서 수신한 메시지 수

## 트러블슈팅

### 일반적인 문제 해결

#### 1. 연결 오류
```
Connection to Kafka broker failed
```
- 브로커 주소 확인: `spring.kafka.bootstrap-servers` 설정 확인
- Kafka 서버 실행 여부 확인
- 네트워크 연결 확인

#### 2. 직렬화 오류
```
Failed to serialize message
```
- 직렬화 설정 확인: `spring.kafka.producer.value-serializer` 설정 확인
- Message 객체 구조 확인

#### 3. 토픽 생성 실패
```
Failed to create topic
```
- Kafka 권한 확인
- `auto.create.topics.enable` 설정 확인
- 토픽 이름 유효성 확인

## 성능 최적화 팁

1. **배치 처리 활용**: `batch-size`와 `linger-ms` 설정을 조정하여 처리량 향상
2. **압축 사용**: `compression-type`을 설정하여 네트워크 대역폭 절약
3. **적절한 파티션 수 설정**: 토픽의 파티션 수를 처리량에 맞게 조정
4. **비동기 전송 활용**: 동기식 전송보다 비동기식 전송이 처리량이 높음
5. **버퍼 메모리 조정**: `buffer-memory` 설정을 시스템 리소스에 맞게 조정

## 보안 고려사항

1. **SSL/TLS 암호화**: 프로덕션 환경에서는 SSL/TLS 암호화 사용 권장
2. **SASL 인증**: 사용자 인증을 위한 SASL 메커니즘 활용
3. **ACL 설정**: 토픽별 접근 제어 설정
4. **네트워크 분리**: Kafka 클러스터를 별도의 네트워크 세그먼트에 배치
