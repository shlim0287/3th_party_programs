import os
import json
import logging
import time
from typing import List, Dict, Any, Optional
from datetime import datetime

logger = logging.getLogger(__name__)

class FineTuner:
    """
    Ollama 모델 파인튜닝 클래스
    로그 데이터를 사용하여 Ollama 모델을 파인튜닝합니다.
    """
    
    def __init__(self, ollama_client):
        """
        파인튜닝 클래스 초기화
        
        Args:
            ollama_client: Ollama API 클라이언트 인스턴스
        """
        # Ollama 클라이언트
        self.ollama_client = ollama_client
        
        # 파인튜닝 설정
        self.data_dir = os.getenv("DATA_DIR", "data")
        self.fine_tuning_history_file = os.path.join(self.data_dir, "fine_tuning_history.json")
        
        # 파인튜닝 히스토리 로드
        self.fine_tuning_history = self._load_fine_tuning_history()
        
        logger.info("파인튜닝 클래스 초기화 완료")
    
    def _load_fine_tuning_history(self) -> List[Dict[str, Any]]:
        """
        파인튜닝 히스토리를 로드합니다.
        
        Returns:
            List[Dict[str, Any]]: 파인튜닝 히스토리 목록
        """
        if os.path.exists(self.fine_tuning_history_file):
            try:
                with open(self.fine_tuning_history_file, 'r', encoding='utf-8') as f:
                    return json.load(f)
            except Exception as e:
                logger.error(f"파인튜닝 히스토리 로드 실패: {str(e)}")
        
        return []
    
    def _save_fine_tuning_history(self):
        """
        파인튜닝 히스토리를 저장합니다.
        """
        try:
            with open(self.fine_tuning_history_file, 'w', encoding='utf-8') as f:
                json.dump(self.fine_tuning_history, f, ensure_ascii=False, indent=2)
            logger.info("파인튜닝 히스토리 저장 완료")
        except Exception as e:
            logger.error(f"파인튜닝 히스토리 저장 실패: {str(e)}")
    
    def fine_tune(self, training_data: List[Dict[str, Any]]) -> bool:
        """
        로그 데이터를 사용하여 Ollama 모델을 파인튜닝합니다.
        
        Args:
            training_data (List[Dict[str, Any]]): 학습 데이터 목록
            
        Returns:
            bool: 파인튜닝 성공 여부
        """
        if not training_data:
            logger.warning("파인튜닝할 데이터가 없습니다")
            return False
        
        logger.info(f"파인튜닝 시작: {len(training_data)}개 데이터")
        
        # 작업 유형별 데이터 분류
        sentiment_data = [data for data in training_data if data.get("task_type") == "sentiment"]
        anomaly_data = [data for data in training_data if data.get("task_type") == "anomaly"]
        summary_data = [data for data in training_data if data.get("task_type") == "summary"]
        
        # 파인튜닝 결과
        results = []
        
        # 감정 분석 파인튜닝
        if sentiment_data:
            result = self._fine_tune_sentiment(sentiment_data)
            if result:
                results.append(result)
        
        # 이상 탐지 파인튜닝
        if anomaly_data:
            result = self._fine_tune_anomaly(anomaly_data)
            if result:
                results.append(result)
        
        # 요약 파인튜닝
        if summary_data:
            result = self._fine_tune_summary(summary_data)
            if result:
                results.append(result)
        
        # 파인튜닝 히스토리 업데이트
        if results:
            for result in results:
                self.fine_tuning_history.append({
                    "timestamp": datetime.now().isoformat(),
                    "task_type": result.get("task_type"),
                    "data_count": result.get("data_count"),
                    "success": result.get("success"),
                    "details": result.get("details")
                })
            
            # 히스토리 저장
            self._save_fine_tuning_history()
            
            logger.info(f"파인튜닝 완료: {len(results)}개 작업 성공")
            return True
        else:
            logger.warning("파인튜닝 실패: 모든 작업이 실패했습니다")
            return False
    
    def _fine_tune_sentiment(self, data: List[Dict[str, Any]]) -> Optional[Dict[str, Any]]:
        """
        감정 분석 모델을 파인튜닝합니다.
        
        Args:
            data (List[Dict[str, Any]]): 감정 분석 학습 데이터
            
        Returns:
            Optional[Dict[str, Any]]: 파인튜닝 결과
        """
        try:
            logger.info(f"감정 분석 파인튜닝 시작: {len(data)}개 데이터")
            
            # 파인튜닝 프롬프트 생성
            prompt = self.ollama_client.create_fine_tuning_prompt(data, "sentiment")
            
            # 파인튜닝 수행 (Ollama API를 통해)
            # 실제로는 Ollama의 파인튜닝 API를 호출하거나 프롬프트 학습을 수행
            # 여기서는 프롬프트 생성 후 모델에 전달하는 방식으로 시뮬레이션
            result = self._simulate_fine_tuning(prompt, "sentiment", len(data))
            
            logger.info(f"감정 분석 파인튜닝 완료: {result}")
            return {
                "task_type": "sentiment",
                "data_count": len(data),
                "success": True,
                "details": result
            }
            
        except Exception as e:
            logger.error(f"감정 분석 파인튜닝 실패: {str(e)}")
            return None
    
    def _fine_tune_anomaly(self, data: List[Dict[str, Any]]) -> Optional[Dict[str, Any]]:
        """
        이상 탐지 모델을 파인튜닝합니다.
        
        Args:
            data (List[Dict[str, Any]]): 이상 탐지 학습 데이터
            
        Returns:
            Optional[Dict[str, Any]]: 파인튜닝 결과
        """
        try:
            logger.info(f"이상 탐지 파인튜닝 시작: {len(data)}개 데이터")
            
            # 파인튜닝 프롬프트 생성
            prompt = self.ollama_client.create_fine_tuning_prompt(data, "anomaly")
            
            # 파인튜닝 수행
            result = self._simulate_fine_tuning(prompt, "anomaly", len(data))
            
            logger.info(f"이상 탐지 파인튜닝 완료: {result}")
            return {
                "task_type": "anomaly",
                "data_count": len(data),
                "success": True,
                "details": result
            }
            
        except Exception as e:
            logger.error(f"이상 탐지 파인튜닝 실패: {str(e)}")
            return None
    
    def _fine_tune_summary(self, data: List[Dict[str, Any]]) -> Optional[Dict[str, Any]]:
        """
        요약 모델을 파인튜닝합니다.
        
        Args:
            data (List[Dict[str, Any]]): 요약 학습 데이터
            
        Returns:
            Optional[Dict[str, Any]]: 파인튜닝 결과
        """
        try:
            logger.info(f"요약 파인튜닝 시작: {len(data)}개 데이터")
            
            # 파인튜닝 프롬프트 생성
            prompt = self.ollama_client.create_fine_tuning_prompt(data, "summary")
            
            # 파인튜닝 수행
            result = self._simulate_fine_tuning(prompt, "summary", len(data))
            
            logger.info(f"요약 파인튜닝 완료: {result}")
            return {
                "task_type": "summary",
                "data_count": len(data),
                "success": True,
                "details": result
            }
            
        except Exception as e:
            logger.error(f"요약 파인튜닝 실패: {str(e)}")
            return None
    
    def _simulate_fine_tuning(self, prompt: str, task_type: str, data_count: int) -> Dict[str, Any]:
        """
        파인튜닝을 시뮬레이션합니다.
        실제 구현에서는 Ollama API를 통해 파인튜닝을 수행합니다.
        
        Args:
            prompt (str): 파인튜닝 프롬프트
            task_type (str): 작업 유형
            data_count (int): 데이터 수
            
        Returns:
            Dict[str, Any]: 시뮬레이션 결과
        """
        # 프롬프트 길이 확인
        prompt_length = len(prompt)
        
        # 파인튜닝 시간 시뮬레이션 (데이터 크기에 비례)
        simulation_time = min(2.0, data_count * 0.1)  # 최대 2초
        time.sleep(simulation_time)
        
        # 결과 반환
        return {
            "prompt_length": prompt_length,
            "processing_time": simulation_time,
            "examples_processed": data_count,
            "model_updated": True
        }
    
    def get_fine_tuning_history(self) -> List[Dict[str, Any]]:
        """
        파인튜닝 히스토리를 반환합니다.
        
        Returns:
            List[Dict[str, Any]]: 파인튜닝 히스토리 목록
        """
        return self.fine_tuning_history
    
    def get_last_fine_tuning_time(self, task_type: Optional[str] = None) -> Optional[datetime]:
        """
        마지막 파인튜닝 시간을 반환합니다.
        
        Args:
            task_type (Optional[str]): 작업 유형 (None인 경우 모든 유형)
            
        Returns:
            Optional[datetime]: 마지막 파인튜닝 시간
        """
        if not self.fine_tuning_history:
            return None
        
        # 작업 유형별 필터링
        filtered_history = self.fine_tuning_history
        if task_type:
            filtered_history = [h for h in filtered_history if h.get("task_type") == task_type]
        
        if not filtered_history:
            return None
        
        # 가장 최근 항목 찾기
        latest = max(filtered_history, key=lambda x: x.get("timestamp", ""))
        
        try:
            return datetime.fromisoformat(latest.get("timestamp"))
        except Exception as e:
            logger.error(f"날짜 파싱 실패: {str(e)}")
            return None