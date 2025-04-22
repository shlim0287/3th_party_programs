# ELK 스택 및 로그 분석

이 문서는 프로젝트의 ELK(Elasticsearch, Logstash, Kibana) 스택 및 로그 분석 구성 요소에 대한 정보를 제공합니다.

## 목차

- [개요](#개요)
- [아키텍처](#아키텍처)
- [구성 요소](#구성-요소)
  - [Logstash](#logstash)
  - [Elasticsearch](#elasticsearch)
  - [Kibana](#kibana)
  - [MySQL](#mysql)
  - [로그 분석 서비스](#로그-분석-서비스)
- [머신러닝 기능](#머신러닝-기능)
- [설치 지침](#설치-지침)
- [사용 예제](#사용-예제)

## 개요

ELK 스택과 로그 분석 구성 요소는 포괄적인 로그 수집, 처리, 저장 및 시각화 기능을 제공합니다. 이 시스템은 다양한 소스에서 로그를 수집하고, Logstash를 사용하여 처리하며, Elasticsearch(검색 및 분석용)와 MySQL(구조화된 저장용) 모두에 저장하고, Kibana를 사용하여 시각화합니다.

## 아키텍처

```
로그 소스 → Logstash → Kafka → Logstash → Elasticsearch → Kibana
                                  ↓
                                MySQL
```

1. **로그 수집**: 다양한 소스(nginx, 애플리케이션, 시스템 메트릭)에서 로그를 수집합니다.
2. **로그 처리**: Logstash가 로그를 처리하고 보강합니다.
3. **메시지 큐**: Kafka가 버퍼 및 분배 지점 역할을 합니다.
4. **저장**: 로그는 Elasticsearch(검색용)와 MySQL(구조화된 저장용) 모두에 저장됩니다.
5. **시각화**: Kibana가 대시보드와 시각화를 제공합니다.
6. **머신러닝**: Elasticsearch의 머신러닝 기능이 이상을 감지합니다.

## 구성 요소

### Logstash

Logstash는 로그를 수집, 처리 및 전달하는 역할을 담당합니다. 두 가지 주요 파이프라인이 있습니다:

1. **입력 파이프라인**: 파일 및 Beats에서 로그를 수집하고, 처리한 후 Kafka로 전송합니다.
2. **출력 파이프라인**: Kafka에서 로그를 소비하고, 추가 처리 후 Elasticsearch로 전송합니다.

설정 파일:
- `logstash/config/logstash.yml`: 주요 Logstash 설정
- `logstash/pipeline/input.conf`: 입력 파이프라인 설정
- `logstash/pipeline/output.conf`: 출력 파이프라인 설정

### Elasticsearch

Elasticsearch는 빠른 검색 및 분석을 위해 로그 데이터를 저장하고 인덱싱합니다. 또한 이상 탐지를 위한 머신러닝 기능을 제공합니다.

인덱스:
- `nginx-access-*`: nginx 액세스 로그 저장
- `application-logs-*`: 애플리케이션 로그 저장
- `system-metrics-*`: 시스템 메트릭 저장

### Kibana

Kibana는 Elasticsearch에 저장된 로그 데이터의 시각화 및 탐색을 제공합니다. 다음과 같은 대시보드를 포함합니다:

1. **웹 트래픽 대시보드**: nginx 액세스 로그 시각화
2. **애플리케이션 성능 대시보드**: 애플리케이션 로그 시각화
3. **시스템 모니터링 대시보드**: 시스템 메트릭 시각화
4. **머신러닝 대시보드**: 이상 탐지 결과 표시

### MySQL

MySQL은 장기 보존 및 SQL 기반 분석을 위한 로그 데이터의 구조화된 저장소를 제공합니다. 객체-관계 매핑을 위해 JPA를 사용합니다.

테이블:
- `nginx_access_logs`: nginx 액세스 로그 저장
- `application_logs`: 애플리케이션 로그 저장
- `system_metrics`: 시스템 메트릭 저장

### 로그 분석 서비스

로그 분석 서비스는 다음과 같은 기능을 제공하는 Spring Boot 애플리케이션입니다:

1. Kafka에서 로그 메시지 소비
2. JPA를 사용하여 MySQL에 저장
3. 로그 쿼리 및 분석을 위한 REST API 제공
4. Elasticsearch 인덱스 및 머신러닝 작업 설정

## 머신러닝 기능

이 시스템은 이상 탐지를 위한 여러 머신러닝 기능을 포함합니다:

1. **응답 시간 이상 탐지**: nginx 응답 시간의 비정상적인 패턴 감지
2. **오류율 이상 탐지**: 애플리케이션 오류율의 비정상적인 패턴 감지
3. **시스템 메트릭 이상 탐지**: 시스템 메트릭의 비정상적인 패턴 감지

이러한 기능은 Elasticsearch의 X-Pack 머신러닝 기능을 사용합니다.

## 설치 지침

### 사전 요구 사항

- Docker 및 Docker Compose
- Java 17 이상
- Gradle

### 인프라 시작하기

1. 인프라 서비스 시작:
   ```bash
   docker-compose up -d
   ```

2. 모든 서비스가 실행 중인지 확인:
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

3. Log Analytics 애플리케이션 시작:
   ```bash
   cd log-analytics
   ./gradlew bootRun
   ```

### Kibana 대시보드 설정

1. http://localhost:5601에서 Kibana 접속
2. 다음 인덱스 패턴 생성:
   - `nginx-access-*`
   - `application-logs-*`
   - `system-metrics-*`
3. 대시보드 가져오기 또는 로그의 지침에 따라 수동으로 생성

## 사용 예제

### 로그 데이터 생성

1. nginx 액세스 로그 생성:
   ```bash
   curl -X POST "http://localhost:8080/api/messages/simple?content=GET /api/users HTTP/1.1&type=NGINX_ACCESS"
   ```

2. 애플리케이션 로그 생성:
   ```bash
   curl -X POST "http://localhost:8080/api/messages/simple?content=Application started successfully&type=APP_LOG"
   ```

3. 오류 로그 생성:
   ```bash
   curl -X POST "http://localhost:8080/api/messages/simple?content=NullPointerException in UserService&type=ERROR_LOG"
   ```

### 로그 보기

1. Kibana에서:
   - http://localhost:5601 접속
   - Discover로 이동
   - 적절한 인덱스 패턴 선택
   - 로그 검색 및 필터링

2. MySQL에서:
   - MySQL 연결: `mysql -h localhost -P 3306 -u loguser -plogpassword logs_analytics`
   - 로그 쿼리: `SELECT * FROM nginx_access_logs LIMIT 10;`

### 대시보드 보기

1. Kibana에서:
   - http://localhost:5601 접속
   - Dashboard로 이동
   - 다음 대시보드 중 하나 선택:
     - 웹 트래픽 대시보드
     - 애플리케이션 성능 대시보드
     - 시스템 모니터링 대시보드
     - 머신러닝 대시보드

### 이상 보기

1. Kibana에서:
   - http://localhost:5601 접속
   - Machine Learning으로 이동
   - Anomaly Detection 선택
   - 이상 탐지 작업 결과 보기
