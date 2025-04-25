# sLLM 모듈 (Small Language Model)

## 개요
sLLM 모듈은 경량 언어 모델(Small Language Model)을 활용하여 로그 데이터를 분석하고 인사이트를 제공하는 Python 기반 애플리케이션입니다. 이 모듈은 ELK 스택(Elasticsearch, Logstash, Kibana), Kafka, 그리고 기존 로그 분석 시스템과 통합되어 로그 데이터에 대한 고급 분석 기능을 제공합니다.

## 기술 스택
- **Python 3.12**: 기본 프로그래밍 언어
- **Ollama**: 경량 LLM 모델 서빙 플랫폼
- **Llama 3**: 실무에서 널리 사용되는 경량 LLM 모델
- **FastAPI**: REST API 제공
- **Kafka**: 로그 데이터 스트리밍
- **Elasticsearch**: 로그 데이터 저장 및 검색
- **Pandas & NumPy**: 데이터 처리 및 분석
- **scikit-learn**: 머신러닝 기능 (클러스터링 등)
- **Transformers & PyTorch**: 모델 파인튜닝

## 아키텍처

### 주요 컴포넌트
1. **Ollama 클라이언트**: Llama 3 모델과 통신하는 인터페이스
2. **데이터 프로세서**: 로그 데이터 전처리 및 분석
3. **Kafka 컨슈머**: 로그 데이터 스트림 소비
4. **파인튜닝 모듈**: 로그 데이터로 모델 학습
5. **FastAPI 서버**: REST API 제공

### 데이터 흐름
1. **데이터 수집**: log_analytics 모듈이 스케줄링을 통해 로그 데이터를 Kafka로 전송
2. **데이터 소비**: Kafka 컨슈머가 로그 데이터를 소비
3. **데이터 처리**: 데이터 프로세서가 로그 데이터를 전처리하고 분석
4. **모델 파인튜닝**: 처리된 데이터로 Llama 3 모델 파인튜닝
5. **API 제공**: 분석 결과 및 인사이트를 API로 제공

## 주요 기능

### 1. 로그 분석
- **감정 분석**: 로그 메시지의 감정(긍정/부정/중립) 분석
- **이상 탐지**: 로그 데이터에서 이상 징후 탐지
- **요약 생성**: 대량의 로그 데이터 요약

### 2. 자동 파인튜닝
- **스케줄링**: 매일 새벽 2시에 자동 파인튜닝 실행
- **데이터 기반 학습**: 새로운 로그 데이터로 모델 지속 개선
- **작업별 최적화**: 감정 분석, 이상 탐지, 요약 생성 각각에 대한 최적화

### 3. API 엔드포인트
- **POST /analyze**: 텍스트 분석 (감정, 이상, 요약)
- **POST /fine-tune**: 수동 파인튜닝 트리거
- **GET /health**: 서비스 상태 확인

## 모듈 구성

### 1. main.py
애플리케이션의 진입점으로, FastAPI 서버를 초기화하고 API 엔드포인트를 정의합니다.

```python
# FastAPI 앱 초기화
app = FastAPI(title="sLLM API", description="경량 LLM을 활용한 로그 분석 API")

# API 엔드포인트
@app.post("/analyze", response_model=AnalysisResponse)
async def analyze_text(request: AnalysisRequest):
    """텍스트를 분석하고 결과를 반환합니다."""
    # 분석 로직
```

### 2. ollama_client.py
Ollama API와 통신하여 Llama 3 모델을 사용하는 클라이언트 클래스입니다.

```python
class OllamaClient:
    """
    Ollama API 클라이언트 클래스
    Llama 3 모델과 상호작용하기 위한 인터페이스를 제공합니다.
    """
    
    def __init__(self):
        """Ollama 클라이언트 초기화"""
        self.base_url = os.getenv("OLLAMA_API_URL", "http://localhost:11434")
        self.model_name = os.getenv("OLLAMA_MODEL", "llama3")
```

### 3. data_processor.py
로그 데이터를 처리하고 분석하는 클래스입니다.

```python
class DataProcessor:
    """
    로그 데이터 처리 클래스
    Elasticsearch에서 로그 데이터를 가져와 전처리하고 분석합니다.
    """
    
    def __init__(self):
        """데이터 프로세서 초기화"""
        # Elasticsearch 연결 설정
        self.es_host = os.getenv("ELASTICSEARCH_HOST", "localhost")
        self.es_port = int(os.getenv("ELASTICSEARCH_PORT", "9200"))
```

### 4. kafka_consumer.py
Kafka 토픽에서 로그 데이터를 소비하는 클래스입니다.

```python
class KafkaLogConsumer:
    """
    Kafka 로그 컨슈머 클래스
    Kafka 토픽에서 로그 데이터를 소비하고 처리합니다.
    """
    
    def __init__(self, data_processor):
        """
        Kafka 로그 컨슈머 초기화
        """
        # Kafka 설정
        self.bootstrap_servers = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:29092")
        self.group_id = os.getenv("KAFKA_GROUP_ID", "sllm-consumer-group")
```

### 5. fine_tuning.py
로그 데이터를 사용하여 Llama 3 모델을 파인튜닝하는 클래스입니다.

```python
class FineTuner:
    """
    Ollama 모델 파인튜닝 클래스
    로그 데이터를 사용하여 Ollama 모델을 파인튜닝합니다.
    """
    
    def __init__(self, ollama_client):
        """
        파인튜닝 클래스 초기화
        """
        # Ollama 클라이언트
        self.ollama_client = ollama_client
```

## 설치 및 실행

### 필수 요구사항
- Python 3.12 이상
- Ollama 설치 및 실행
- Kafka 클러스터 접근 권한
- Elasticsearch 접근 권한

### 설치 방법
1. 가상 환경 생성 및 활성화
   ```bash
   python -m venv .venv
   source .venv/bin/activate  # Linux/Mac
   .venv\Scripts\activate     # Windows
   ```

2. 의존성 설치
   ```bash
   pip install -r requirements.txt
   ```

3. 환경 변수 설정 (.env 파일 생성)
   ```
   OLLAMA_API_URL=http://localhost:11434
   OLLAMA_MODEL=llama3
   ELASTICSEARCH_HOST=localhost
   ELASTICSEARCH_PORT=9200
   KAFKA_BOOTSTRAP_SERVERS=localhost:29092
   ```

### 실행 방법
```bash
python main.py
```

서버가 시작되면 http://localhost:8083 에서 API에 접근할 수 있습니다.

## 로그 분석 모듈과의 통합

### 스케줄링 메커니즘
log_analytics 모듈의 `LogDataScheduler` 클래스는 매일 새벽 1시에 로그 데이터를 수집하여 Kafka 토픽으로 전송합니다. 이 데이터는 sLLM 모듈의 Kafka 컨슈머에 의해 소비되어 처리됩니다.

```java
@Scheduled(cron = "0 0 1 * * *")
public void scheduledDataProcessing() {
    log.info("스케줄링된 로그 데이터 처리 시작: {}", LocalDateTime.now());
    processLogData();
}
```

### 데이터 전송
log_analytics 모듈은 다음과 같은 형식으로 데이터를 Kafka 토픽으로 전송합니다:

```json
{
  "type": "application",
  "timestamp": "2023-04-25T01:00:00",
  "count": 100,
  "data": [...]
}
```

### 파인튜닝 스케줄링
sLLM 모듈은 매일 새벽 2시에 자동으로 파인튜닝을 실행합니다:

```python
# 매일 새벽 2시에 파인튜닝 실행
schedule.every().day.at("02:00").do(scheduled_fine_tuning)
```

## Llama 3 모델 소개

Llama 3는 Meta AI에서 개발한 오픈 소스 경량 언어 모델로, 다음과 같은 특징을 가지고 있습니다:

1. **효율성**: 상대적으로 작은 크기(8B)로도 우수한 성능 제공
2. **다양한 작업 지원**: 텍스트 생성, 요약, 분류, 질의응답 등 다양한 작업 수행 가능
3. **맥락 이해**: 긴 컨텍스트를 이해하고 처리할 수 있는 능력
4. **파인튜닝 용이성**: 특정 도메인에 맞게 쉽게 파인튜닝 가능
5. **자원 효율성**: 적은 컴퓨팅 자원으로도 실행 가능

Llama 3는 로그 분석과 같은 특수 도메인에 파인튜닝하여 사용할 때 최상의 성능을 발휘합니다.

## 활용 사례

### 1. 로그 이상 탐지
시스템 로그에서 비정상적인 패턴을 감지하여 잠재적인 문제를 조기에 발견합니다.

```
2023-04-25 01:23:45 ERROR [ThreadPool-3] Connection refused: connect
2023-04-25 01:23:46 ERROR [ThreadPool-3] Retry attempt 1 failed
2023-04-25 01:23:48 ERROR [ThreadPool-3] Retry attempt 2 failed
```

분석 결과:
```json
{
  "anomaly_status": "critical",
  "confidence": 0.92,
  "detected_issues": ["연결 실패", "재시도 실패"],
  "explanation": "연속적인 연결 오류와 재시도 실패가 발생하고 있어 네트워크 또는 대상 서비스에 문제가 있을 가능성이 높습니다."
}
```

### 2. 로그 요약
대량의 로그 데이터를 간결하게 요약하여 핵심 정보를 제공합니다.

원본 로그 (수백 줄):
```
2023-04-25 01:00:01 INFO [RequestProcessor-1] Request received: GET /api/users
2023-04-25 01:00:02 INFO [RequestProcessor-1] Request processed in 150ms
...
```

요약 결과:
```
지난 1시간 동안 총 1,245개의 API 요청이 처리되었습니다. 평균 응답 시간은 180ms이며, 가장 많이 요청된 엔드포인트는 /api/users (450회)입니다. 5건의 오류가 발생했으며, 주로 인증 관련 문제였습니다.
```

## 향후 개선 계획

1. **모델 최적화**: 더 작고 빠른 모델로 대체하여 성능 향상
2. **멀티모달 지원**: 로그 데이터 외에 메트릭 차트, 시각화 등 분석 지원
3. **실시간 분석**: 배치 처리 외에 실시간 스트림 처리 지원
4. **사용자 인터페이스**: 분석 결과를 시각화하는 웹 인터페이스 개발
5. **알림 시스템**: 중요한 이상 탐지 시 알림 발송 기능

## 결론

sLLM 모듈은 경량 언어 모델의 강력한 기능을 활용하여 로그 데이터에서 유용한 인사이트를 추출합니다. 기존 ELK 스택과 Kafka 인프라를 활용하면서도, 고급 자연어 처리 기능을 통해 로그 분석의 새로운 차원을 제공합니다. 자동화된 파인튜닝 메커니즘을 통해 시간이 지남에 따라 모델이 지속적으로 개선되어 더 정확한 분석 결과를 제공합니다.