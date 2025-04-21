# Kafka 고급 기능 가이드

이 문서는 프로젝트에 구현된 Kafka의 고급 기능들을 설명합니다.

## 목차

1. [다중 토픽 관리](#1-다중-토픽-관리)
2. [양방향 통신 (프로듀서와 컨슈머의 이중 역할)](#2-양방향-통신-프로듀서와-컨슈머의-이중-역할)
3. [다양한 배치 처리 옵션](#3-다양한-배치-처리-옵션)
4. [메시지 손실 방지 및 복구 메커니즘](#4-메시지-손실-방지-및-복구-메커니즘)

## 1. 다중 토픽 관리

이 프로젝트는 두 개의 토픽을 사용합니다:

1. **demo-topic**: 주요 메시지 전송에 사용되는 토픽
2. **feedback-topic**: 컨슈머에서 프로듀서로 피드백을 보내는 데 사용되는 토픽

토픽은 `KafkaProducerConfig` 클래스에서 Spring Bean으로 생성됩니다:

```java
@Bean
public NewTopic demoTopic() {
    return TopicBuilder.name(TOPIC_NAME)
            .partitions(partitions)
            .replicas(replicationFactor)
            .build();
}

@Bean
public NewTopic feedbackTopic() {
    return TopicBuilder.name(FEEDBACK_TOPIC_NAME)
            .partitions(partitions)
            .replicas(replicationFactor)
            .build();
}
```

## 2. 양방향 통신 (프로듀서와 컨슈머의 이중 역할)

### 프로듀서의 컨슈머 역할

프로듀서 애플리케이션은 피드백 토픽을 구독하여 컨슈머 역할도 수행합니다:

```java
@KafkaListener(
        topics = KafkaProducerConfig.FEEDBACK_TOPIC_NAME,
        containerFactory = "kafkaListenerContainerFactory",
        groupId = "${spring.kafka.consumer.group-id}-producer"
)
public void consumeFeedback(ConsumerRecord<String, Message> record, Acknowledgment ack) {
    // 피드백 메시지 처리 로직
    ack.acknowledge();
}
```

### 컨슈머의 프로듀서 역할

컨슈머 애플리케이션은 피드백 토픽에 메시지를 발행하여 프로듀서 역할도 수행합니다:

```java
@Transactional
public CompletableFuture<SendResult<String, Message>> sendFeedbackMessage(Message originalMessage) {
    Message feedbackMessage = Message.builder()
            .id(UUID.randomUUID().toString())
            .content("Feedback for message: " + originalMessage.getId())
            .type("FEEDBACK")
            .timestamp(LocalDateTime.now())
            .source("consumer-service")
            .build();
    
    return kafkaTemplate.send(KafkaConsumerConfig.FEEDBACK_TOPIC_NAME, 
                             feedbackMessage.getId(), 
                             feedbackMessage);
}
```

## 3. 다양한 배치 처리 옵션

이 프로젝트는 세 가지 다른 배치 처리 방식을 구현합니다:

### 1. 일반 배치 처리

기본 배치 설정으로 메시지를 처리합니다:

```java
@KafkaListener(
        topics = KafkaConsumerConfig.TOPIC_NAME,
        containerFactory = "kafkaListenerContainerFactory"
)
public void consumeBatch(List<ConsumerRecord<String, Message>> records, Acknowledgment ack) {
    // 배치 처리 로직
}
```

### 2. 단일 메시지 처리

메시지를 하나씩 처리합니다:

```java
@KafkaListener(
        topics = KafkaConsumerConfig.TOPIC_NAME,
        containerFactory = "kafkaSingleListenerContainerFactory"
)
public void consumeSingle(ConsumerRecord<String, Message> record, Acknowledgment ack) {
    // 단일 메시지 처리 로직
}
```

### 3. 대용량 배치 처리

대량의 메시지를 한 번에 처리합니다:

```java
@KafkaListener(
        topics = KafkaConsumerConfig.TOPIC_NAME,
        containerFactory = "kafkaLargeBatchListenerContainerFactory"
)
public void consumeLargeBatch(List<ConsumerRecord<String, Message>> records, Acknowledgment ack) {
    // 대용량 배치 처리 로직
}
```

대용량 배치 처리를 위한 특별한 설정:

- **MAX_POLL_RECORDS_CONFIG**: 1000 (1000개 레코드)
- **FETCH_MIN_BYTES_CONFIG**: 1024 * 1024 (1MB 최소 페치)
- **FETCH_MAX_WAIT_MS_CONFIG**: 1000 (최대 1초 대기)

## 4. 메시지 손실 방지 및 복구 메커니즘

이 프로젝트는 메시지 손실을 방지하고 실패한 메시지를 복구하기 위한 여러 메커니즘을 구현합니다:

### 1. 안정적인 프로듀서 설정

- **ACKS_CONFIG**: "all" (모든 복제본 확인)
- **ENABLE_IDEMPOTENCE_CONFIG**: true (중복 방지)
- **RETRIES_CONFIG**: 3 (재시도 횟수)

### 2. 수동 오프셋 커밋

- **ENABLE_AUTO_COMMIT_CONFIG**: false (자동 커밋 비활성화)
- **AckMode**: MANUAL_IMMEDIATE (수동 즉시 확인)

### 3. 실패한 메시지 복구

```java
private void handleFailedMessages(List<ConsumerRecord<String, Message>> failedRecords) {
    for (ConsumerRecord<String, Message> record : failedRecords) {
        // 복구 메시지 생성 및 피드백 토픽으로 전송
        Message recoveryMessage = createRecoveryMessage(record.value());
        sendFeedbackMessage(recoveryMessage);
    }
}
```

### 4. 트랜잭션 사용

```java
@Transactional
public CompletableFuture<SendResult<String, Message>> sendMessage(String key, Message message) {
    // 트랜잭션 내에서 메시지 전송
}
```

이러한 고급 기능들을 통해 Kafka를 사용한 메시징 시스템의 안정성, 확장성, 유연성을 크게 향상시킬 수 있습니다.