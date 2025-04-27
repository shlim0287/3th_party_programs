# 테스트 문서

이 문서는 프로젝트의 테스트 접근 방식에 대한 개요를 제공하며, 단위 테스트, 통합 테스트 및 부하 테스트를 포함합니다.

## 목차

- [테스트 전략](#테스트-전략)
- [단위 테스트](#단위-테스트)
  - [Kafka Producer](#kafka-producer)
  - [Kafka Consumer](#kafka-consumer)
  - [sLLM](#sllm)
  - [Log Analytics](#log-analytics)
- [통합 테스트](#통합-테스트)
- [부하 테스트](#부하-테스트)
- [테스트 실행](#테스트-실행)

## 테스트 전략

이 프로젝트의 테스트 전략은 다층적 접근 방식을 따릅니다:

1. **단위 테스트**: 개별 컴포넌트를 격리하여 테스트
2. **통합 테스트**: 컴포넌트 간 상호작용 테스트
3. **부하 테스트**: 부하 상황에서 시스템 성능 테스트

## 단위 테스트

### Kafka Producer

Kafka Producer 모듈의 단위 테스트는 다음을 포함합니다:

- `KafkaProducerService`: 메시지 전송 기능, 오류 처리 및 메트릭 테스트
- `KafkaProducerController`: 메시지 전송을 위한 REST 엔드포인트 테스트

Kafka Producer 테스트 실행 방법:

```bash
cd kafka_producer
./gradlew test
```

### Kafka Consumer

Kafka Consumer 모듈의 단위 테스트는 다음을 포함합니다:

- `KafkaConsumerService`: 메시지 소비, 배치 처리 및 오류 처리 테스트
- `MessageProcessingService`: 다양한 메시지 유형에 대한 메시지 처리 로직 테스트

Kafka Consumer 테스트 실행 방법:

```bash
cd kafka_consumer
./gradlew test
```

### sLLM

sLLM 모듈의 단위 테스트는 다음을 포함합니다:

- `OllamaClient`: Ollama API와의 상호작용 테스트 (텍스트 생성, 감정 분석, 이상 탐지, 요약 포함)
- `DataProcessor`: Elasticsearch에서 데이터 가져오기 및 변환을 포함한 로그 데이터 처리 테스트

sLLM 테스트 실행 방법:

```bash
cd sLLM
python -m unittest discover tests
```

### Log Analytics

Log Analytics 모듈의 단위 테스트는 다음을 포함합니다:

- `ApplicationLogRepository`: 애플리케이션 로그에 대한 데이터베이스 작업 테스트
- `NginxAccessLogRepository`: Nginx 액세스 로그에 대한 데이터베이스 작업 테스트
- `SystemMetricRepository`: 시스템 메트릭에 대한 데이터베이스 작업 테스트

Log Analytics 테스트 실행 방법:

```bash
cd log_analytics
./gradlew test
```

## 통합 테스트

통합 테스트는 시스템의 다양한 컴포넌트가 올바르게 함께 작동하는지 확인합니다. 주요 통합 시나리오는 다음과 같습니다:

1. **Kafka Producer에서 Consumer로의 흐름**: Kafka Producer가 생성한 메시지가 Kafka Consumer에 의해 올바르게 소비되는지 테스트
2. **Log Analytics에서 Elasticsearch로의 흐름**: 로그 데이터가 Elasticsearch에 올바르게 저장되고 검색되는지 테스트
3. **sLLM 분석 흐름**: 로그 데이터가 sLLM 모듈에 의해 올바르게 처리되고 분석되는지 테스트

통합 테스트를 실행하려면 모든 컴포넌트가 실행 중이어야 합니다:

```bash
# Kafka 및 Elasticsearch 시작
docker-compose up -d

# 통합 테스트 실행
./gradlew integrationTest
```

## 부하 테스트

부하 테스트는 JMeter를 사용하여 높은 트래픽 시나리오를 시뮬레이션하고 시스템 성능을 측정합니다. 부하 테스트는 다음을 포함합니다:

1. **Kafka Producer 부하 테스트**: 시스템이 대량의 메시지 생성을 처리하는 능력 테스트
2. **Kafka Consumer 부하 테스트**: 시스템이 대량의 메시지를 처리하는 능력 테스트
3. **Log Analytics 쿼리 부하 테스트**: 시스템이 대량의 로그 쿼리를 처리하는 능력 테스트

### JMeter로 부하 테스트 실행

1. [https://jmeter.apache.org/download_jmeter.cgi](https://jmeter.apache.org/download_jmeter.cgi)에서 JMeter 설치
2. JMeter를 열고 `load_tests/kafka_load_test.jmx`에서 테스트 계획 로드
3. 필요에 따라 테스트 매개변수 구성
4. 테스트 실행 및 결과 분석

## 테스트 실행

### 사전 요구 사항

- Java 17 이상
- Python 3.12 이상
- Docker 및 Docker Compose
- Gradle

### 모든 테스트 실행

프로젝트의 모든 테스트를 실행하는 방법:

```bash
# 모든 모듈에 대한 단위 테스트 실행
./gradlew test

# Python 단위 테스트 실행
cd sLLM
python -m unittest discover tests

# 통합 테스트 실행
./gradlew integrationTest
```

### 지속적 통합

프로젝트는 GitHub Actions를 사용하여 각 커밋에서 자동으로 테스트를 실행하도록 구성되어 있습니다. CI 파이프라인은 다음을 포함합니다:

1. 프로젝트 빌드
2. 단위 테스트 실행
3. 통합 테스트 실행
4. 테스트 보고서 생성

테스트 보고서는 GitHub Actions 워크플로우 아티팩트에서 확인할 수 있습니다.
