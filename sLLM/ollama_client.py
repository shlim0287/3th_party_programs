import os
import json
import logging
import requests
from typing import Dict, Any, Optional, List

logger = logging.getLogger(__name__)

class OllamaClient:
    """
    Ollama API 클라이언트 클래스
    Llama 3 모델과 상호작용하기 위한 인터페이스를 제공합니다.
    """
    
    def __init__(self):
        """Ollama 클라이언트 초기화"""
        # Ollama API 엔드포인트 설정
        self.base_url = os.getenv("OLLAMA_API_URL", "http://localhost:11434")
        self.model_name = os.getenv("OLLAMA_MODEL", "llama3")
        
        # 모델 파라미터 설정
        self.default_params = {
            "temperature": float(os.getenv("OLLAMA_TEMPERATURE", "0.7")),
            "top_p": float(os.getenv("OLLAMA_TOP_P", "0.9")),
            "top_k": int(os.getenv("OLLAMA_TOP_K", "40")),
            "num_predict": int(os.getenv("OLLAMA_NUM_PREDICT", "256")),
        }
        
        logger.info(f"Ollama 클라이언트 초기화: 모델={self.model_name}, URL={self.base_url}")
    
    def ensure_model_loaded(self) -> bool:
        """
        모델이 로드되어 있는지 확인하고, 없으면 다운로드합니다.
        
        Returns:
            bool: 모델 로드 성공 여부
        """
        try:
            # 모델 목록 확인
            response = requests.get(f"{self.base_url}/api/tags")
            models = response.json().get("models", [])
            
            # 모델 이름으로 필터링
            model_exists = any(model.get("name") == self.model_name for model in models)
            
            if not model_exists:
                logger.info(f"모델 '{self.model_name}'을 다운로드합니다...")
                response = requests.post(
                    f"{self.base_url}/api/pull",
                    json={"name": self.model_name}
                )
                if response.status_code == 200:
                    logger.info(f"모델 '{self.model_name}' 다운로드 완료")
                    return True
                else:
                    logger.error(f"모델 다운로드 실패: {response.text}")
                    return False
            
            logger.info(f"모델 '{self.model_name}'이 이미 로드되어 있습니다")
            return True
            
        except Exception as e:
            logger.error(f"모델 로드 확인 중 오류 발생: {str(e)}")
            return False
    
    def check_status(self) -> Dict[str, Any]:
        """
        Ollama 서비스 상태를 확인합니다.
        
        Returns:
            Dict[str, Any]: 상태 정보
        """
        try:
            response = requests.get(f"{self.base_url}/api/tags")
            if response.status_code == 200:
                return {"status": "online", "models": len(response.json().get("models", []))}
            else:
                return {"status": "error", "message": response.text}
        except Exception as e:
            return {"status": "offline", "error": str(e)}
    
    def generate_text(self, prompt: str, params: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """
        주어진 프롬프트에 대한 텍스트를 생성합니다.
        
        Args:
            prompt (str): 입력 프롬프트
            params (Optional[Dict[str, Any]]): 생성 파라미터
            
        Returns:
            Dict[str, Any]: 생성된 텍스트와 메타데이터
        """
        # 기본 파라미터와 사용자 지정 파라미터 병합
        request_params = self.default_params.copy()
        if params:
            request_params.update(params)
        
        try:
            # API 요청
            response = requests.post(
                f"{self.base_url}/api/generate",
                json={
                    "model": self.model_name,
                    "prompt": prompt,
                    **request_params
                }
            )
            
            if response.status_code != 200:
                logger.error(f"텍스트 생성 실패: {response.text}")
                return {"result": "", "error": response.text}
            
            # 응답 파싱
            response_text = response.text
            response_lines = response_text.strip().split('\n')
            
            # 마지막 줄에서 최종 응답 추출
            final_response = json.loads(response_lines[-1])
            
            return {
                "result": final_response.get("response", ""),
                "eval_count": final_response.get("eval_count"),
                "eval_duration": final_response.get("eval_duration")
            }
            
        except Exception as e:
            logger.error(f"텍스트 생성 중 오류 발생: {str(e)}")
            return {"result": "", "error": str(e)}
    
    def analyze_sentiment(self, text: str) -> Dict[str, Any]:
        """
        텍스트의 감정을 분석합니다.
        
        Args:
            text (str): 분석할 텍스트
            
        Returns:
            Dict[str, Any]: 감정 분석 결과
        """
        prompt = f"""
        다음 텍스트의 감정을 분석하고 결과를 JSON 형식으로 반환해주세요.
        감정은 'positive', 'neutral', 'negative' 중 하나여야 합니다.
        신뢰도는 0부터 1 사이의 값이어야 합니다.
        
        텍스트: {text}
        
        JSON 형식:
        {{
            "sentiment": "감정",
            "confidence": 신뢰도,
            "explanation": "분석 설명"
        }}
        """
        
        result = self.generate_text(prompt)
        
        try:
            # JSON 응답 파싱
            response_text = result.get("result", "")
            json_start = response_text.find("{")
            json_end = response_text.rfind("}") + 1
            
            if json_start >= 0 and json_end > json_start:
                json_str = response_text[json_start:json_end]
                sentiment_data = json.loads(json_str)
                
                return {
                    "result": sentiment_data.get("sentiment", "neutral"),
                    "confidence": sentiment_data.get("confidence", 0.0),
                    "details": {
                        "explanation": sentiment_data.get("explanation", "")
                    }
                }
            else:
                logger.warning(f"JSON 파싱 실패: {response_text}")
                return {"result": "neutral", "confidence": 0.0, "details": {"raw_response": response_text}}
                
        except Exception as e:
            logger.error(f"감정 분석 결과 파싱 중 오류 발생: {str(e)}")
            return {"result": "neutral", "confidence": 0.0, "error": str(e)}
    
    def detect_anomaly(self, log_text: str) -> Dict[str, Any]:
        """
        로그 텍스트에서 이상 징후를 탐지합니다.
        
        Args:
            log_text (str): 분석할 로그 텍스트
            
        Returns:
            Dict[str, Any]: 이상 탐지 결과
        """
        prompt = f"""
        다음 로그 텍스트에서 이상 징후가 있는지 분석하고 결과를 JSON 형식으로 반환해주세요.
        이상 여부는 'normal', 'warning', 'critical' 중 하나여야 합니다.
        신뢰도는 0부터 1 사이의 값이어야 합니다.
        
        로그 텍스트:
        {log_text}
        
        JSON 형식:
        {{
            "anomaly_status": "이상 여부",
            "confidence": 신뢰도,
            "detected_issues": ["문제1", "문제2", ...],
            "explanation": "분석 설명"
        }}
        """
        
        result = self.generate_text(prompt)
        
        try:
            # JSON 응답 파싱
            response_text = result.get("result", "")
            json_start = response_text.find("{")
            json_end = response_text.rfind("}") + 1
            
            if json_start >= 0 and json_end > json_start:
                json_str = response_text[json_start:json_end]
                anomaly_data = json.loads(json_str)
                
                return {
                    "result": anomaly_data.get("anomaly_status", "normal"),
                    "confidence": anomaly_data.get("confidence", 0.0),
                    "details": {
                        "detected_issues": anomaly_data.get("detected_issues", []),
                        "explanation": anomaly_data.get("explanation", "")
                    }
                }
            else:
                logger.warning(f"JSON 파싱 실패: {response_text}")
                return {"result": "normal", "confidence": 0.0, "details": {"raw_response": response_text}}
                
        except Exception as e:
            logger.error(f"이상 탐지 결과 파싱 중 오류 발생: {str(e)}")
            return {"result": "normal", "confidence": 0.0, "error": str(e)}
    
    def generate_summary(self, text: str) -> Dict[str, Any]:
        """
        텍스트 요약을 생성합니다.
        
        Args:
            text (str): 요약할 텍스트
            
        Returns:
            Dict[str, Any]: 요약 결과
        """
        prompt = f"""
        다음 텍스트를 간결하게 요약해주세요. 요약은 3-5문장으로 작성하고, 
        가장 중요한 정보를 포함해야 합니다.
        
        텍스트:
        {text}
        
        요약:
        """
        
        result = self.generate_text(prompt)
        
        return {
            "result": result.get("result", "").strip(),
            "details": {
                "eval_count": result.get("eval_count"),
                "eval_duration": result.get("eval_duration")
            }
        }
    
    def create_fine_tuning_prompt(self, examples: List[Dict[str, Any]], task_type: str) -> str:
        """
        파인튜닝을 위한 프롬프트를 생성합니다.
        
        Args:
            examples (List[Dict[str, Any]]): 학습 예제 목록
            task_type (str): 작업 유형 (sentiment, anomaly, summary)
            
        Returns:
            str: 파인튜닝 프롬프트
        """
        if task_type == "sentiment":
            prompt_template = """
            # 감정 분석 학습
            
            다음은 텍스트의 감정을 분석하는 예제입니다. 각 예제를 학습하여 감정 분석 능력을 향상시키세요.
            
            {examples}
            
            이제 당신은 텍스트의 감정을 더 정확하게 분석할 수 있습니다.
            """
            
            examples_text = ""
            for i, example in enumerate(examples):
                examples_text += f"""
                ## 예제 {i+1}
                텍스트: {example.get('text', '')}
                감정: {example.get('sentiment', '')}
                이유: {example.get('explanation', '')}
                """
            
            return prompt_template.format(examples=examples_text)
            
        elif task_type == "anomaly":
            prompt_template = """
            # 이상 탐지 학습
            
            다음은 로그 텍스트에서 이상 징후를 탐지하는 예제입니다. 각 예제를 학습하여 이상 탐지 능력을 향상시키세요.
            
            {examples}
            
            이제 당신은 로그에서 이상 징후를 더 정확하게 탐지할 수 있습니다.
            """
            
            examples_text = ""
            for i, example in enumerate(examples):
                examples_text += f"""
                ## 예제 {i+1}
                로그: {example.get('log_text', '')}
                이상 여부: {example.get('anomaly_status', '')}
                탐지된 문제: {', '.join(example.get('detected_issues', []))}
                설명: {example.get('explanation', '')}
                """
            
            return prompt_template.format(examples=examples_text)
            
        elif task_type == "summary":
            prompt_template = """
            # 텍스트 요약 학습
            
            다음은 텍스트를 요약하는 예제입니다. 각 예제를 학습하여 요약 능력을 향상시키세요.
            
            {examples}
            
            이제 당신은 텍스트를 더 효과적으로 요약할 수 있습니다.
            """
            
            examples_text = ""
            for i, example in enumerate(examples):
                examples_text += f"""
                ## 예제 {i+1}
                원본 텍스트: {example.get('original_text', '')}
                요약: {example.get('summary', '')}
                """
            
            return prompt_template.format(examples=examples_text)
            
        else:
            logger.warning(f"지원되지 않는 작업 유형: {task_type}")
            return ""