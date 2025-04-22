# 카프카 학습 프로젝트

이 프로젝트는 Spring Boot를 사용한 Apache Kafka의 프로덕션 수준 구현을 보여줍니다. 두 개의 애플리케이션으로 구성되어 있습니다:
1. **Kafka Producer** - Kafka에 메시지를 생성합니다
2. **Kafka Consumer** - Kafka에서 메시지를 소비합니다

이 프로젝트는 Prometheus와 Grafana를 통한 포괄적인 모니터링과 Kafka 관리를 위한 Kafdrop UI를 포함합니다.

## 목차

- [Kafka 개념](#kafka-concepts)
- [프로젝트 구조](#project-structure)
- [설치 지침](#setup-instructions)
- [사용 예제](#usage-examples)
- [고급 기능](#advanced-features)
- [ELK 스택 및 로그 분석](#elk-stack-and-log-analytics)
- [모니터링](#monitoring)
- [모범 사례](#best-practices)
- [문제 해결](#troubleshooting)

## Kafka 개념

### Apache Kafka란 무엇인가요?

Apache Kafka는 고처리량, 내결함성, 발행-구독 메시징을 처리하도록 설계된 분산 스트리밍 플랫폼입니다. 원래 LinkedIn에서 개발되었으며 나중에 오픈 소스 Apache 프로젝트가 되었습니다.

### 주요 개념

#### 토픽

**토픽**은 레코드가 발행되는 카테고리 또는 피드 이름입니다. Kafka의 토픽은 항상 다중 구독자입니다. 즉, 토픽은 여기에 작성된 데이터를 구독하는 0개, 1개 또는 여러 개의 소비자를 가질 수 있습니다.

#### 파티션

토픽은 **파티션**되어 있습니다. 즉, 토픽이 서로 다른 Kafka 브로커에 위치한 여러 "버킷"에 분산되어 있습니다. 이를 통해 병렬 처리와 높은 처리량이 가능합니다.

#### 프로듀서

**프로듀서**는 선택한 토픽에 데이터를 발행합니다. 프로듀서는 어떤 레코드를 토픽 내의 어떤 파티션에 할당할지 선택할 책임이 있습니다.

#### 컨슈머

**컨슈머**는 선택한 토픽에서 데이터를 읽습니다. 컨슈머는 컨슈머 그룹에 속하며, 토픽에 발행된 각 레코드는 구독하는 각 컨슈머 그룹 내의 하나의 컨슈머 인스턴스에 전달됩니다.

#### 브로커

Kafka는 **브로커**라고 불리는 하나 이상의 서버 클러스터로 실행됩니다. 브로커는 레코드가 발행될 때 이를 수신하고 저장하며, 컨슈머가 요청할 때 제공하는 역할을 합니다.

#### ZooKeeper

Kafka는 클러스터를 관리하기 위해 **ZooKeeper**를 사용합니다. ZooKeeper는 컨트롤러 선출, 클러스터 멤버십, 토픽 구성, 할당량 및 ACL에 사용됩니다.

### Kafka 보장

- 프로듀서가 특정 토픽 파티션으로 보낸 메시지는 보낸 순서대로 추가됩니다.
- 컨슈머 인스턴스는 로그에 저장된 순서대로 레코드를 확인합니다.
- 복제 팩터가 N인 토픽의 경우, 로그에 커밋된 레코드를 잃지 않고 최대 N-1개의 서버 장애를 허용할 수 있습니다.

## 프로젝트 구조

### Kafka Producer

Kafka Producer 애플리케이션은 Kafka에 메시지를 전송하는 역할을 담당합니다. 다음을 포함합니다:

- 메시지 전송을 위한 **REST API**
- JSON을 사용한 **메시지 직렬화**
- **오류 처리** 및 재시도 메커니즘
- 모니터링을 위한 **메트릭**

### Kafka Consumer

Kafka Consumer 애플리케이션은 Kafka에서 메시지를 소비하는 역할을 담당합니다. 다음을 포함합니다:

- 메시지 처리를 위한 **메시지 리스너**
- JSON에서 **메시지 역직렬화**
- **오류 처리** 및 재시도 메커니즘
- 모니터링을 위한 **메트릭**
- 처리된 메시지를 검색하기 위한 **REST API**

### 지원 서비스

- **Zookeeper** - Kafka 클러스터 관리
- **Kafka** - 메시지 브로커
- **Kafdrop** - Kafka 모니터링을 위한 UI
- **Elasticsearch** - 로그 및 메시지 저장 및 검색용
- **Logstash** - 로그 수집 및 처리용
- **Kibana** - 로그 시각화 및 대시보드용
- **MySQL** - 구조화된 로그 데이터 저장용
- **Prometheus** - 메트릭 수집용
- **Grafana** - 메트릭 시각화용

## 설치 지침

### 사전 요구 사항

- Docker 및 Docker Compose
- Java 17 이상
- Maven 또는 Gradle

### 인프라 시작하기

1. 저장소 복제:
   ```bash
   git clone <repository-url>
   cd kafka-learning-project
   ```

2. Docker Compose를 사용하여 인프라 서비스 시작:
   ```bash
   docker-compose up -d
   ```

   이렇게 하면 Zookeeper, Kafka, Kafdrop, Elasticsearch, Prometheus 및 Grafana가 시작됩니다.

3. 모든 서비스가 실행 중인지 확인:
   ```bash
   docker-compose ps
   ```

### 애플리케이션 시작하기

1. Kafka Producer 애플리케이션 시작:
   ```bash
   cd kafka_producer
   ./gradlew bootRun
   ```

2. Kafka Consumer 애플리케이션 시작:
   ```bash
   cd kafka_consumer
   ./gradlew bootRun
   ```

## 사용 예제

### REST API를 통한 메시지 전송

#### 복잡한 메시지 전송

```bash
curl -X POST http://localhost:8080/api/messages \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Hello, Kafka!",
    "type": "INFO",
    "source": "curl-example",
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

### 처리된 메시지 검색

#### 특정 메시지 가져오기

```bash
curl -X GET http://localhost:8081/api/messages/{message-id}
```

`{message-id}`를 메시지 전송 시 반환된 실제 메시지 ID로 대체하세요.

#### 메시지 통계 가져오기

```bash
curl -X GET http://localhost:8081/api/messages/stats
```

### Kafdrop으로 Kafka 모니터링

1. 브라우저에서 Kafdrop 열기: http://localhost:19000
2. 토픽, 파티션 및 메시지 보기

## 고급 기능

이 프로젝트는 Kafka의 여러 고급 기능을 구현하여 실제 프로덕션 환경에서 필요한 다양한 시나리오를 지원합니다:

1. **다중 토픽 관리** - 주요 메시지용 demo-topic과 피드백용 feedback-topic을 사용합니다.
2. **양방향 통신** - 프로듀서가 컨슈머 역할을, 컨슈머가 프로듀서 역할을 수행할 수 있습니다.
3. **다양한 배치 처리 옵션** - 일반 배치, 단일 메시지, 대용량 배치 처리를 지원합니다.
4. **메시지 손실 방지 및 복구 메커니즘** - 안정적인 메시지 전송과 실패 시 복구 기능을 제공합니다.

자세한 내용은 [Kafka 고급 기능 가이드](KAFKA_ADVANCED_FEATURES.md)를 참조하세요.

## ELK 스택 및 로그 분석

이 프로젝트는 ELK(Elasticsearch, Logstash, Kibana) 스택과 MySQL을 사용한 로그 분석 기능을 포함합니다:

### 주요 구성 요소

- **Logstash**: 로그 수집 및 처리
- **Elasticsearch**: 로그 저장 및 검색
- **Kibana**: 로그 시각화 및 대시보드
- **MySQL**: 구조화된 로그 데이터 저장 (JPA 사용)
- **Log Analytics 서비스**: 로그 처리 및 분석

### 기능

1. **로그 수집 파이프라인**: Logstash를 통해 다양한 소스에서 로그 수집
2. **로그 저장 및 분석**: Elasticsearch와 MySQL에 로그 저장
3. **시각화 대시보드**: Kibana를 통한 로그 데이터 시각화
4. **머신러닝 기반 이상 탐지**: Elasticsearch의 머신러닝 기능을 활용한 이상 탐지

자세한 내용은 [ELK 스택 및 로그 분석 가이드](README-ELK.md)를 참조하세요.

## 모니터링

### Prometheus

Prometheus는 프로듀서와 컨슈머 애플리케이션 모두에서 메트릭을 수집하는 데 사용됩니다. http://localhost:9090에서 Prometheus UI에 접근할 수 있습니다.

모니터링할 주요 메트릭:

- **kafka.producer.messages.sent** - 프로듀서가 보낸 메시지 수
- **kafka.producer.messages.failed** - 전송에 실패한 메시지 수
- **kafka.producer.message.send.time** - 메시지 전송에 걸린 시간
- **kafka.consumer.messages.received** - 컨슈머가 받은 메시지 수
- **kafka.consumer.messages.processed** - 성공적으로 처리된 메시지 수
- **kafka.consumer.messages.failed** - 처리에 실패한 메시지 수
- **kafka.consumer.message.processing.time** - 메시지 처리에 걸린 시간

### Grafana

Grafana는 Prometheus에서 수집한 메트릭을 시각화하는 데 사용됩니다. http://localhost:3000에서 Grafana UI에 접근할 수 있습니다.

기본 자격 증명:
- 사용자 이름: admin
- 비밀번호: admin

## 모범 사례

### 프로듀서 모범 사례

1. **멱등성 프로듀서 사용**: 중복 메시지를 방지하기 위해 `enable.idempotence=true`를 설정하세요.
2. **적절한 확인 응답 설정**: 중요한 데이터의 경우 모든 복제본이 메시지를 수신하도록 `acks=all`을 사용하세요.
3. **재시도 로직 구현**: 일시적인 장애에 대비하여 백오프가 있는 재시도를 사용하세요.
4. **성능 모니터링**: 전송 시간, 성공률, 오류율과 같은 메트릭을 추적하세요.
5. **적절한 직렬화 사용**: JSON은 사람이 읽을 수 있지만 Avro나 Protobuf와 같은 이진 형식보다 효율성이 떨어집니다.

### 컨슈머 모범 사례

1. **수동 오프셋 커밋 사용**: 데이터 손실을 방지하기 위해 자동 커밋을 피하세요.
2. **오류 처리 구현**: 예외를 처리하고 재시도 로직을 구현하세요.
3. **컨슈머 지연 모니터링**: 컨슈머가 최신 메시지에서 얼마나 뒤처져 있는지 추적하세요.
4. **적절한 역직렬화 사용**: 프로듀서가 사용하는 직렬화 형식과 일치시키세요.
5. **정상 종료 구현**: 종료하기 전에 모든 메시지가 처리되도록 하세요.

## 문제 해결

### 일반적인 문제

1. **연결 거부**: Kafka와 Zookeeper가 실행 중인지 확인하세요.
2. **직렬화/역직렬화 오류**: 프로듀서와 컨슈머가 호환되는 직렬화 형식을 사용하는지 확인하세요.
3. **컨슈머가 메시지를 수신하지 않음**: 컨슈머 그룹 ID, 토픽 이름 및 파티션 할당을 확인하세요.
4. **프로듀서가 메시지를 전송하지 않음**: 연결 설정 및 오류 로그를 확인하세요.

### 로그 확인

- **프로듀서 로그**: 프로듀서 애플리케이션 디렉토리에서 `logs/application.log`를 확인하세요.
- **컨슈머 로그**: 컨슈머 애플리케이션 디렉토리에서 `logs/application.log`를 확인하세요.
- **Kafka 로그**: `docker-compose logs kafka`로 Docker 로그를 확인하세요.

### 도움 받기

이 문서에서 다루지 않은 문제가 발생하면 다음을 수행하세요:
1. 애플리케이션 로그에서 오류 메시지를 확인하세요.
2. [Apache Kafka 문서](https://kafka.apache.org/documentation/)를 참조하세요.
3. Stack Overflow나 Kafka 메일링 리스트에서 유사한 문제를 검색하세요.
