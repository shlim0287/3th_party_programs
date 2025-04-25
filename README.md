로그 분석 및 AI 시스템

## 프로젝트 개요
이 프로젝트는 분산 시스템의 로그 데이터를 수집, 처리, 분석하고 AI를 활용하여 인사이트를 제공하는 종합적인 플랫폼입니다. ELK 스택(Elasticsearch, Logstash, Kibana), Apache Kafka, Spring Boot, 그리고 경량 언어 모델(sLLM)을 통합하여 로그 데이터의 실시간 처리와 고급 분석 기능을 제공합니다.

## 기술 스택

### 백엔드
- **Spring Boot**: 마이크로서비스 아키텍처 구현
- **Apache Kafka**: 분산 이벤트 스트리밍 플랫폼
- **Elasticsearch**: 로그 데이터 저장 및 검색
- **Logstash**: 로그 데이터 수집 및 변환
- **Kibana**: 로그 데이터 시각화
- **Python**: sLLM 모듈 구현
- **Ollama/Llama 3**: 경량 언어 모델

### 모니터링 및 관측성
- **Prometheus**: 메트릭 수집
- **Grafana**: 메트릭 시각화
- **Filebeat/Metricbeat**: 로그 및 메트릭 수집

### 데이터베이스
- **MySQL**: 로그 데이터 영구 저장

## 프로젝트 아키텍처

```
                                  ┌─────────────┐
                                  │   Kibana    │
                                  │  (시각화)   │
                                  └──────┬──────┘
                                         │
┌─────────────┐    ┌──────────────┐    ┌─┴───────────┐    ┌─────────────┐
│  로그 소스   │    │  Logstash    │    │Elasticsearch│    │ Prometheus  │
│(애플리케이션)│───▶│ (수집/변환)  │───▶│  (저장/검색) │◀──│  (메트릭)   │
└──────┬──────┘    └───────┬──────┘    └──────┬──────┘    └─────────────┘
       │                  │                   │
       │                  │                   │
       │           ┌──────▼──────┐     ┌──────▼──────┐
       └─────────▶│    Kafka     │───▶│Log Analytics│
                   │  (메시징)   │     │   (분석)     │
                   └──────┬──────┘     └──────┬──────┘
                          │                   │
                          │                   │
                   ┌──────▼───────┐     ┌─────▼───────┐
                   │Kafka Producer│     │    sLLM     │
                   │Kafka Consumer│◀────│ (AI 분석)   │
                   └──────────────┘     └─────────────┘
```

## 모듈 구성

### 1. Kafka Producer
메시지를 생성하여 Kafka 토픽으로 전송하는 Spring Boot 애플리케이션입니다. 고가용성, 확장성, 신뢰성을 갖춘 메시지 생산자 역할을 수행합니다.

[자세한 내용](kafka_producer/README.md)

### 2. Kafka Consumer
Kafka 토픽으로부터 메시지를 소비하고 처리하는 Spring Boot 애플리케이션입니다. 다양한 소비 전략과 오류 처리 메커니즘을 제공합니다.

[자세한 내용](kafka_consumer/README.md)

### 3. Log Analytics
다양한 소스에서 로그 데이터를 수집, 저장, 분석하는 Spring Boot 애플리케이션입니다. Elasticsearch를 통해 고급 분석 및 시각화 기능을 제공합니다.

[자세한 내용](log_analytics/README.md)

### 4. sLLM (Small Language Model)
경량 언어 모델(Llama 3)을 활용하여 로그 데이터를 분석하고 인사이트를 제공하는 Python 기반 애플리케이션입니다.

[자세한 내용](sLLM/README.md)

### 5. Logstash
다양한 소스에서 데이터를 수집, 변환하여 Elasticsearch로 전송하는 데이터 처리 파이프라인입니다.

### 6. Prometheus
시스템 및 애플리케이션 메트릭을 수집하고 모니터링하는 도구입니다.

## 데이터 흐름

1. **데이터 수집**:
   - 애플리케이션에서 생성된 로그는 Logstash를 통해 수집됩니다.
   - 시스템 메트릭은 Beats(Filebeat, Metricbeat)를 통해 수집됩니다.

2. **데이터 처리**:
   - Logstash는 수집된 데이터를 파싱, 변환, 보강하여 Kafka 토픽으로 전송합니다.
   - Kafka는 데이터 스트림을 관리하고 소비자에게 제공합니다.

3. **데이터 저장**:
   - Elasticsearch는 로그 데이터를 저장하고 검색 기능을 제공합니다.
   - MySQL은 로그 데이터의 영구 저장소 역할을 합니다.

4. **데이터 분석**:
   - Log Analytics 모듈은 로그 데이터를 분석하고 인사이트를 추출합니다.
   - sLLM 모듈은 AI를 활용하여 로그 데이터에 대한 고급 분석을 수행합니다.

5. **데이터 시각화**:
   - Kibana는 Elasticsearch의 데이터를 시각화합니다.
   - Grafana는 Prometheus의 메트릭을 시각화합니다.

## 주요 기능

### 로그 수집 및 처리
- 다양한 소스(애플리케이션 로그, Nginx 액세스 로그, 시스템 메트릭)에서 로그 데이터 수집
- 로그 데이터 정규화 및 구조화
- 실시간 로그 스트리밍

### 로그 분석
- 로그 데이터 검색 및 필터링
- 로그 패턴 분석
- 이상 탐지
- 트렌드 분석

### AI 기반 인사이트
- 로그 메시지 감정 분석
- 이상 징후 자동 탐지
- 대량 로그 데이터 요약
- 자동 파인튜닝을 통한 모델 개선

### 모니터링 및 알림
- 시스템 및 애플리케이션 메트릭 모니터링
- 대시보드를 통한 시각화
- 이상 상황 알림

## 설치 및 실행

### 사전 요구사항
- Java 17 이상
- Python 3.12 이상
- Docker 및 Docker Compose
- Kafka 클러스터
- Elasticsearch 클러스터
- MySQL 데이터베이스

### 설치 방법

1. 저장소 클론
   ```bash
   git clone https://github.com/yourusername/YoHanKi.git
   cd YoHanKi
   ```

2. Docker Compose로 인프라 시작
   ```bash
   docker-compose up -d
   ```

3. 각 모듈 빌드 및 실행
   ```bash
   # Kafka Producer
   cd kafka_producer
   ./gradlew bootRun

   # Kafka Consumer
   cd ../kafka_consumer
   ./gradlew bootRun

   # Log Analytics
   cd ../log_analytics
   ./gradlew bootRun

   # sLLM
   cd ../sLLM
   python -m venv .venv
   Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process
   source .venv/bin/activate  # Linux/Mac
   .venv\Scripts\activate     # Windows
   pip install -r requirements.txt
   python main.py
   ```

## 사용 예제

### 로그 데이터 전송
```bash
curl -X POST http://localhost:8080/api/messages \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Application started successfully",
    "type": "INFO",
    "source": "user-service",
    "metadata": {
      "version": "1.0.0",
      "environment": "production"
    }
  }'
```

### 로그 분석 API 호출
```bash
curl -X POST http://localhost:8083/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Connection refused: connect",
    "analysis_type": "anomaly"
  }'
```

### Kibana 대시보드 접근
웹 브라우저에서 `http://localhost:5601` 접속

## 프로젝트 확장

### 새로운 로그 소스 추가
1. Logstash 파이프라인 구성 파일 수정
2. Kafka 토픽 생성
3. Log Analytics 모듈에 새 모델 및 리포지토리 추가

### 새로운 분석 기능 추가
1. Log Analytics 모듈에 새 분석 서비스 추가
2. sLLM 모듈에 새 분석 엔드포인트 추가
3. Kibana에 새 시각화 추가

## 문제 해결

### 일반적인 문제
- **Kafka 연결 오류**: Kafka 브로커 주소 및 포트 확인
- **Elasticsearch 연결 오류**: Elasticsearch 클러스터 상태 확인
- **로그 파싱 오류**: Logstash 파이프라인 구성 확인

### 로그 확인
- Kafka Producer: `kafka_producer/logs/application.log`
- Kafka Consumer: `kafka_consumer/logs/application.log`
- Log Analytics: `log_analytics/logs/log-analytics.log`
- sLLM: `sLLM/sllm.log`

## 기여 방법
1. 저장소 포크
2. 기능 브랜치 생성 (`git checkout -b feature/amazing-feature`)
3. 변경 사항 커밋 (`git commit -m 'Add some amazing feature'`)
4. 브랜치 푸시 (`git push origin feature/amazing-feature`)
5. Pull Request 생성