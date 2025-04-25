import os
import logging
import uvicorn
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from dotenv import load_dotenv
import schedule
import time
import threading

from ollama_client import OllamaClient
from data_processor import DataProcessor
from kafka_consumer import KafkaLogConsumer
from fine_tuning import FineTuner

# 환경 변수 로드
load_dotenv()

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler("sllm.log"),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

# FastAPI 앱 초기화
app = FastAPI(title="sLLM API", description="경량 LLM을 활용한 로그 분석 API")

# 컴포넌트 초기화
ollama_client = OllamaClient()
data_processor = DataProcessor()
kafka_consumer = KafkaLogConsumer(data_processor)
fine_tuner = FineTuner(ollama_client)

# 스케줄러 실행 플래그
scheduler_running = False

# 요청/응답 모델
class AnalysisRequest(BaseModel):
    text: str
    analysis_type: str = "sentiment"  # sentiment, anomaly, summary

class AnalysisResponse(BaseModel):
    result: str
    confidence: float = None
    details: dict = None

# 스케줄러 작업 정의
def scheduled_fine_tuning():
    """주기적으로 새로운 로그 데이터로 모델을 파인튜닝합니다."""
    logger.info("스케줄링된 파인튜닝 작업 시작")
    try:
        # 최신 로그 데이터 가져오기
        log_data = data_processor.get_latest_processed_data()
        if log_data and len(log_data) > 0:
            # 파인튜닝 수행
            fine_tuner.fine_tune(log_data)
            logger.info(f"파인튜닝 완료: {len(log_data)}개 데이터 처리됨")
        else:
            logger.info("파인튜닝할 새 데이터가 없습니다")
    except Exception as e:
        logger.error(f"파인튜닝 중 오류 발생: {str(e)}")

# 스케줄러 스레드 함수
def run_scheduler():
    """스케줄러를 별도 스레드에서 실행합니다."""
    global scheduler_running
    scheduler_running = True
    
    # 매일 새벽 2시에 파인튜닝 실행
    schedule.every().day.at("02:00").do(scheduled_fine_tuning)
    
    logger.info("스케줄러 시작됨")
    while scheduler_running:
        schedule.run_pending()
        time.sleep(60)  # 1분마다 스케줄 확인

# API 엔드포인트
@app.post("/analyze", response_model=AnalysisResponse)
async def analyze_text(request: AnalysisRequest):
    """텍스트를 분석하고 결과를 반환합니다."""
    try:
        if request.analysis_type == "sentiment":
            result = ollama_client.analyze_sentiment(request.text)
        elif request.analysis_type == "anomaly":
            result = ollama_client.detect_anomaly(request.text)
        elif request.analysis_type == "summary":
            result = ollama_client.generate_summary(request.text)
        else:
            raise HTTPException(status_code=400, detail="지원되지 않는 분석 유형입니다")
        
        return AnalysisResponse(
            result=result["result"],
            confidence=result.get("confidence"),
            details=result.get("details")
        )
    except Exception as e:
        logger.error(f"분석 중 오류 발생: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/fine-tune")
async def trigger_fine_tuning():
    """수동으로 파인튜닝을 트리거합니다."""
    try:
        scheduled_fine_tuning()
        return {"status": "success", "message": "파인튜닝이 시작되었습니다"}
    except Exception as e:
        logger.error(f"수동 파인튜닝 트리거 중 오류 발생: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/health")
async def health_check():
    """서비스 상태를 확인합니다."""
    return {
        "status": "healthy",
        "ollama_status": ollama_client.check_status(),
        "scheduler_running": scheduler_running
    }

# 애플리케이션 시작 이벤트
@app.on_event("startup")
async def startup_event():
    """애플리케이션 시작 시 실행되는 이벤트 핸들러"""
    # Kafka 컨슈머 시작
    threading.Thread(target=kafka_consumer.start_consuming, daemon=True).start()
    logger.info("Kafka 컨슈머가 시작되었습니다")
    
    # 스케줄러 시작
    threading.Thread(target=run_scheduler, daemon=True).start()
    logger.info("스케줄러가 시작되었습니다")
    
    # Ollama 모델 로드 확인
    ollama_client.ensure_model_loaded()
    logger.info("Ollama 모델이 로드되었습니다")

# 애플리케이션 종료 이벤트
@app.on_event("shutdown")
async def shutdown_event():
    """애플리케이션 종료 시 실행되는 이벤트 핸들러"""
    global scheduler_running
    scheduler_running = False
    kafka_consumer.stop_consuming()
    logger.info("애플리케이션이 정상적으로 종료되었습니다")

# 직접 실행 시 서버 시작
if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8083, reload=True)