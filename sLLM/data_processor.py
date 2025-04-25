import os
import json
import logging
import pandas as pd
import numpy as np
from typing import List, Dict, Any, Optional
from datetime import datetime, timedelta
from elasticsearch import Elasticsearch
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.cluster import KMeans

logger = logging.getLogger(__name__)

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
        self.es_user = os.getenv("ELASTICSEARCH_USER", "elastic")
        self.es_password = os.getenv("ELASTICSEARCH_PASSWORD", "changeme")
        
        # 데이터 저장 경로
        self.data_dir = os.getenv("DATA_DIR", "data")
        os.makedirs(self.data_dir, exist_ok=True)
        
        # 마지막 처리 시간 저장 파일
        self.last_processed_file = os.path.join(self.data_dir, "last_processed.json")
        
        # 처리된 데이터 저장 파일
        self.processed_data_file = os.path.join(self.data_dir, "processed_data.json")
        
        # 마지막 처리 시간 초기화
        self.last_processed = self._load_last_processed()
        
        logger.info(f"데이터 프로세서 초기화: ES={self.es_host}:{self.es_port}, 데이터 디렉토리={self.data_dir}")
        
        # Elasticsearch 클라이언트 초기화
        try:
            self.es_client = Elasticsearch(
                [f"http://{self.es_host}:{self.es_port}"],
                basic_auth=(self.es_user, self.es_password)
            )
            logger.info("Elasticsearch 연결 성공")
        except Exception as e:
            logger.error(f"Elasticsearch 연결 실패: {str(e)}")
            self.es_client = None
    
    def _load_last_processed(self) -> Dict[str, datetime]:
        """마지막 처리 시간을 로드합니다."""
        if os.path.exists(self.last_processed_file):
            try:
                with open(self.last_processed_file, 'r') as f:
                    data = json.load(f)
                    return {
                        k: datetime.fromisoformat(v) 
                        for k, v in data.items()
                    }
            except Exception as e:
                logger.error(f"마지막 처리 시간 로드 실패: {str(e)}")
        
        # 기본값: 현재 시간 - 1일
        default_time = datetime.now() - timedelta(days=1)
        return {
            "application_logs": default_time,
            "nginx_access": default_time,
            "system_metrics": default_time
        }
    
    def _save_last_processed(self):
        """마지막 처리 시간을 저장합니다."""
        try:
            with open(self.last_processed_file, 'w') as f:
                json.dump({
                    k: v.isoformat() 
                    for k, v in self.last_processed.items()
                }, f)
            logger.info("마지막 처리 시간 저장 완료")
        except Exception as e:
            logger.error(f"마지막 처리 시간 저장 실패: {str(e)}")
    
    def _save_processed_data(self, data: List[Dict[str, Any]]):
        """처리된 데이터를 저장합니다."""
        try:
            with open(self.processed_data_file, 'w', encoding='utf-8') as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
            logger.info(f"처리된 데이터 저장 완료: {len(data)}개 항목")
        except Exception as e:
            logger.error(f"처리된 데이터 저장 실패: {str(e)}")
    
    def get_latest_processed_data(self) -> List[Dict[str, Any]]:
        """가장 최근에 처리된 데이터를 반환합니다."""
        if os.path.exists(self.processed_data_file):
            try:
                with open(self.processed_data_file, 'r', encoding='utf-8') as f:
                    return json.load(f)
            except Exception as e:
                logger.error(f"처리된 데이터 로드 실패: {str(e)}")
        
        return []
    
    def process_new_logs(self) -> List[Dict[str, Any]]:
        """
        새로운 로그 데이터를 처리합니다.
        
        Returns:
            List[Dict[str, Any]]: 처리된 로그 데이터 목록
        """
        if not self.es_client:
            logger.error("Elasticsearch 클라이언트가 초기화되지 않았습니다")
            return []
        
        # 현재 시간
        now = datetime.now()
        
        # 애플리케이션 로그 처리
        app_logs = self._fetch_application_logs(self.last_processed["application_logs"], now)
        
        # Nginx 액세스 로그 처리
        nginx_logs = self._fetch_nginx_logs(self.last_processed["nginx_access"], now)
        
        # 시스템 메트릭 처리
        system_metrics = self._fetch_system_metrics(self.last_processed["system_metrics"], now)
        
        # 마지막 처리 시간 업데이트
        self.last_processed["application_logs"] = now
        self.last_processed["nginx_access"] = now
        self.last_processed["system_metrics"] = now
        self._save_last_processed()
        
        # 모든 데이터 결합
        all_data = []
        all_data.extend(self._process_application_logs(app_logs))
        all_data.extend(self._process_nginx_logs(nginx_logs))
        all_data.extend(self._process_system_metrics(system_metrics))
        
        # 처리된 데이터 저장
        if all_data:
            self._save_processed_data(all_data)
        
        return all_data
    
    def _fetch_application_logs(self, start_time: datetime, end_time: datetime) -> List[Dict[str, Any]]:
        """
        Elasticsearch에서 애플리케이션 로그를 가져옵니다.
        
        Args:
            start_time (datetime): 시작 시간
            end_time (datetime): 종료 시간
            
        Returns:
            List[Dict[str, Any]]: 애플리케이션 로그 목록
        """
        try:
            query = {
                "query": {
                    "range": {
                        "timestamp": {
                            "gte": start_time.isoformat(),
                            "lt": end_time.isoformat()
                        }
                    }
                },
                "sort": [{"timestamp": {"order": "asc"}}],
                "size": 1000
            }
            
            response = self.es_client.search(
                index="application-logs-*",
                body=query
            )
            
            logs = [hit["_source"] for hit in response["hits"]["hits"]]
            logger.info(f"애플리케이션 로그 {len(logs)}개 가져옴")
            return logs
            
        except Exception as e:
            logger.error(f"애플리케이션 로그 가져오기 실패: {str(e)}")
            return []
    
    def _fetch_nginx_logs(self, start_time: datetime, end_time: datetime) -> List[Dict[str, Any]]:
        """
        Elasticsearch에서 Nginx 액세스 로그를 가져옵니다.
        
        Args:
            start_time (datetime): 시작 시간
            end_time (datetime): 종료 시간
            
        Returns:
            List[Dict[str, Any]]: Nginx 액세스 로그 목록
        """
        try:
            query = {
                "query": {
                    "range": {
                        "timestamp": {
                            "gte": start_time.isoformat(),
                            "lt": end_time.isoformat()
                        }
                    }
                },
                "sort": [{"timestamp": {"order": "asc"}}],
                "size": 1000
            }
            
            response = self.es_client.search(
                index="nginx-access-*",
                body=query
            )
            
            logs = [hit["_source"] for hit in response["hits"]["hits"]]
            logger.info(f"Nginx 액세스 로그 {len(logs)}개 가져옴")
            return logs
            
        except Exception as e:
            logger.error(f"Nginx 액세스 로그 가져오기 실패: {str(e)}")
            return []
    
    def _fetch_system_metrics(self, start_time: datetime, end_time: datetime) -> List[Dict[str, Any]]:
        """
        Elasticsearch에서 시스템 메트릭을 가져옵니다.
        
        Args:
            start_time (datetime): 시작 시간
            end_time (datetime): 종료 시간
            
        Returns:
            List[Dict[str, Any]]: 시스템 메트릭 목록
        """
        try:
            query = {
                "query": {
                    "range": {
                        "timestamp": {
                            "gte": start_time.isoformat(),
                            "lt": end_time.isoformat()
                        }
                    }
                },
                "sort": [{"timestamp": {"order": "asc"}}],
                "size": 1000
            }
            
            response = self.es_client.search(
                index="system-metrics-*",
                body=query
            )
            
            metrics = [hit["_source"] for hit in response["hits"]["hits"]]
            logger.info(f"시스템 메트릭 {len(metrics)}개 가져옴")
            return metrics
            
        except Exception as e:
            logger.error(f"시스템 메트릭 가져오기 실패: {str(e)}")
            return []
    
    def _process_application_logs(self, logs: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        애플리케이션 로그를 처리하여 학습 데이터로 변환합니다.
        
        Args:
            logs (List[Dict[str, Any]]): 원시 애플리케이션 로그
            
        Returns:
            List[Dict[str, Any]]: 처리된 학습 데이터
        """
        if not logs:
            return []
        
        # 로그 레벨별 분류
        error_logs = [log for log in logs if log.get("log_level") == "ERROR"]
        warning_logs = [log for log in logs if log.get("log_level") == "WARNING"]
        info_logs = [log for log in logs if log.get("log_level") == "INFO"]
        
        # 결과 데이터
        result = []
        
        # 오류 로그 처리 (이상 탐지 학습 데이터)
        if error_logs:
            for log in error_logs:
                log_text = f"{log.get('timestamp')} {log.get('service')} {log.get('log_level')} {log.get('message')}"
                if log.get('stack_trace'):
                    log_text += f"\n{log.get('stack_trace')}"
                
                result.append({
                    "task_type": "anomaly",
                    "log_text": log_text,
                    "anomaly_status": "critical",
                    "detected_issues": ["오류 발생"],
                    "explanation": f"서비스 '{log.get('service')}'에서 오류가 발생했습니다: {log.get('message')}"
                })
        
        # 경고 로그 처리 (이상 탐지 학습 데이터)
        if warning_logs:
            for log in warning_logs:
                log_text = f"{log.get('timestamp')} {log.get('service')} {log.get('log_level')} {log.get('message')}"
                
                result.append({
                    "task_type": "anomaly",
                    "log_text": log_text,
                    "anomaly_status": "warning",
                    "detected_issues": ["경고 발생"],
                    "explanation": f"서비스 '{log.get('service')}'에서 경고가 발생했습니다: {log.get('message')}"
                })
        
        # 정보 로그 처리 (요약 학습 데이터)
        if len(info_logs) >= 10:
            # 로그 메시지 클러스터링
            try:
                messages = [log.get('message', '') for log in info_logs if log.get('message')]
                if len(messages) >= 10:
                    # TF-IDF 벡터화
                    vectorizer = TfidfVectorizer(max_features=100)
                    X = vectorizer.fit_transform(messages)
                    
                    # K-means 클러스터링
                    n_clusters = min(5, len(messages) // 2)
                    kmeans = KMeans(n_clusters=n_clusters, random_state=42)
                    clusters = kmeans.fit_predict(X)
                    
                    # 클러스터별 대표 로그 선택
                    for cluster_id in range(n_clusters):
                        cluster_logs = [info_logs[i] for i, c in enumerate(clusters) if c == cluster_id]
                        if cluster_logs:
                            # 클러스터 내 로그 메시지 결합
                            original_text = "\n".join([
                                f"{log.get('timestamp')} {log.get('service')} {log.get('message')}"
                                for log in cluster_logs[:10]
                            ])
                            
                            # 요약 생성
                            summary = f"서비스 '{cluster_logs[0].get('service')}'의 활동 요약: " + \
                                     f"{len(cluster_logs)}개의 유사한 로그 메시지가 발생했습니다."
                            
                            result.append({
                                "task_type": "summary",
                                "original_text": original_text,
                                "summary": summary
                            })
            except Exception as e:
                logger.error(f"정보 로그 클러스터링 실패: {str(e)}")
        
        logger.info(f"애플리케이션 로그 처리 완료: {len(result)}개 학습 데이터 생성")
        return result
    
    def _process_nginx_logs(self, logs: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        Nginx 액세스 로그를 처리하여 학습 데이터로 변환합니다.
        
        Args:
            logs (List[Dict[str, Any]]): 원시 Nginx 액세스 로그
            
        Returns:
            List[Dict[str, Any]]: 처리된 학습 데이터
        """
        if not logs:
            return []
        
        # 상태 코드별 분류
        error_logs = [log for log in logs if log.get("status_code", 0) >= 500]
        warning_logs = [log for log in logs if 400 <= log.get("status_code", 0) < 500]
        success_logs = [log for log in logs if 200 <= log.get("status_code", 0) < 400]
        
        # 결과 데이터
        result = []
        
        # 오류 로그 처리 (이상 탐지 학습 데이터)
        if error_logs:
            for log in error_logs:
                log_text = (
                    f"{log.get('timestamp')} {log.get('client_ip')} {log.get('request_method')} "
                    f"{log.get('request_path')} {log.get('status_code')} {log.get('response_time')}ms"
                )
                
                result.append({
                    "task_type": "anomaly",
                    "log_text": log_text,
                    "anomaly_status": "critical",
                    "detected_issues": [f"서버 오류 {log.get('status_code')}"],
                    "explanation": f"서버 오류 {log.get('status_code')}가 발생했습니다. 요청 경로: {log.get('request_path')}"
                })
        
        # 경고 로그 처리 (이상 탐지 학습 데이터)
        if warning_logs:
            for log in warning_logs:
                log_text = (
                    f"{log.get('timestamp')} {log.get('client_ip')} {log.get('request_method')} "
                    f"{log.get('request_path')} {log.get('status_code')} {log.get('response_time')}ms"
                )
                
                result.append({
                    "task_type": "anomaly",
                    "log_text": log_text,
                    "anomaly_status": "warning",
                    "detected_issues": [f"클라이언트 오류 {log.get('status_code')}"],
                    "explanation": f"클라이언트 오류 {log.get('status_code')}가 발생했습니다. 요청 경로: {log.get('request_path')}"
                })
        
        # 응답 시간이 느린 로그 처리 (이상 탐지 학습 데이터)
        slow_logs = [log for log in success_logs if log.get("response_time", 0) > 1000]  # 1초 이상
        if slow_logs:
            for log in slow_logs:
                log_text = (
                    f"{log.get('timestamp')} {log.get('client_ip')} {log.get('request_method')} "
                    f"{log.get('request_path')} {log.get('status_code')} {log.get('response_time')}ms"
                )
                
                result.append({
                    "task_type": "anomaly",
                    "log_text": log_text,
                    "anomaly_status": "warning",
                    "detected_issues": ["응답 시간 지연"],
                    "explanation": f"응답 시간이 {log.get('response_time')}ms로 지연되었습니다. 요청 경로: {log.get('request_path')}"
                })
        
        # 트래픽 요약 (요약 학습 데이터)
        if success_logs:
            try:
                # 경로별 요청 수 계산
                path_counts = {}
                for log in success_logs:
                    path = log.get('request_path', '')
                    if path:
                        path_counts[path] = path_counts.get(path, 0) + 1
                
                # 상위 경로 추출
                top_paths = sorted(path_counts.items(), key=lambda x: x[1], reverse=True)[:5]
                
                if top_paths:
                    # 원본 텍스트 생성
                    original_text = "웹 트래픽 로그:\n"
                    for i, log in enumerate(success_logs[:20]):
                        original_text += (
                            f"{log.get('timestamp')} {log.get('client_ip')} {log.get('request_method')} "
                            f"{log.get('request_path')} {log.get('status_code')} {log.get('response_time')}ms\n"
                        )
                    
                    # 요약 생성
                    summary = f"웹 트래픽 요약: 총 {len(success_logs)}개의 성공적인 요청이 있었습니다. "
                    summary += "가장 많이 요청된 경로: "
                    for path, count in top_paths:
                        summary += f"{path} ({count}회), "
                    summary = summary.rstrip(", ")
                    
                    result.append({
                        "task_type": "summary",
                        "original_text": original_text,
                        "summary": summary
                    })
            except Exception as e:
                logger.error(f"웹 트래픽 요약 생성 실패: {str(e)}")
        
        logger.info(f"Nginx 액세스 로그 처리 완료: {len(result)}개 학습 데이터 생성")
        return result
    
    def _process_system_metrics(self, metrics: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        시스템 메트릭을 처리하여 학습 데이터로 변환합니다.
        
        Args:
            metrics (List[Dict[str, Any]]): 원시 시스템 메트릭
            
        Returns:
            List[Dict[str, Any]]: 처리된 학습 데이터
        """
        if not metrics:
            return []
        
        # 결과 데이터
        result = []
        
        # 호스트별 메트릭 그룹화
        host_metrics = {}
        for metric in metrics:
            host = metric.get('host', 'unknown')
            if host not in host_metrics:
                host_metrics[host] = []
            host_metrics[host].append(metric)
        
        # 호스트별 처리
        for host, host_data in host_metrics.items():
            # CPU 사용량 높은 경우 (이상 탐지 학습 데이터)
            high_cpu_metrics = [m for m in host_data if m.get('cpu_usage', 0) > 80]
            if high_cpu_metrics:
                for metric in high_cpu_metrics:
                    log_text = (
                        f"{metric.get('timestamp')} {host} CPU: {metric.get('cpu_usage')}% "
                        f"Memory: {metric.get('memory_usage')}% Disk: {metric.get('disk_usage')}%"
                    )
                    
                    result.append({
                        "task_type": "anomaly",
                        "log_text": log_text,
                        "anomaly_status": "warning",
                        "detected_issues": ["CPU 사용량 높음"],
                        "explanation": f"호스트 '{host}'의 CPU 사용량이 {metric.get('cpu_usage')}%로 높습니다."
                    })
            
            # 메모리 사용량 높은 경우 (이상 탐지 학습 데이터)
            high_mem_metrics = [m for m in host_data if m.get('memory_usage', 0) > 90]
            if high_mem_metrics:
                for metric in high_mem_metrics:
                    log_text = (
                        f"{metric.get('timestamp')} {host} CPU: {metric.get('cpu_usage')}% "
                        f"Memory: {metric.get('memory_usage')}% Disk: {metric.get('disk_usage')}%"
                    )
                    
                    result.append({
                        "task_type": "anomaly",
                        "log_text": log_text,
                        "anomaly_status": "critical",
                        "detected_issues": ["메모리 사용량 높음"],
                        "explanation": f"호스트 '{host}'의 메모리 사용량이 {metric.get('memory_usage')}%로 매우 높습니다."
                    })
            
            # 디스크 사용량 높은 경우 (이상 탐지 학습 데이터)
            high_disk_metrics = [m for m in host_data if m.get('disk_usage', 0) > 85]
            if high_disk_metrics:
                for metric in high_disk_metrics:
                    log_text = (
                        f"{metric.get('timestamp')} {host} CPU: {metric.get('cpu_usage')}% "
                        f"Memory: {metric.get('memory_usage')}% Disk: {metric.get('disk_usage')}%"
                    )
                    
                    result.append({
                        "task_type": "anomaly",
                        "log_text": log_text,
                        "anomaly_status": "warning",
                        "detected_issues": ["디스크 사용량 높음"],
                        "explanation": f"호스트 '{host}'의 디스크 사용량이 {metric.get('disk_usage')}%로 높습니다."
                    })
            
            # 시스템 성능 요약 (요약 학습 데이터)
            if len(host_data) >= 10:
                try:
                    # 시간별 평균 계산
                    df = pd.DataFrame(host_data)
                    if 'timestamp' in df.columns:
                        df['timestamp'] = pd.to_datetime(df['timestamp'])
                        df.set_index('timestamp', inplace=True)
                        hourly_avg = df.resample('H').mean()
                        
                        if not hourly_avg.empty:
                            # 원본 텍스트 생성
                            original_text = f"호스트 '{host}' 시스템 메트릭:\n"
                            for i, metric in enumerate(host_data[:20]):
                                original_text += (
                                    f"{metric.get('timestamp')} CPU: {metric.get('cpu_usage')}% "
                                    f"Memory: {metric.get('memory_usage')}% Disk: {metric.get('disk_usage')}%\n"
                                )
                            
                            # 요약 생성
                            avg_cpu = hourly_avg['cpu_usage'].mean() if 'cpu_usage' in hourly_avg else 0
                            avg_mem = hourly_avg['memory_usage'].mean() if 'memory_usage' in hourly_avg else 0
                            avg_disk = hourly_avg['disk_usage'].mean() if 'disk_usage' in hourly_avg else 0
                            
                            summary = (
                                f"호스트 '{host}' 시스템 성능 요약: "
                                f"평균 CPU 사용량 {avg_cpu:.1f}%, "
                                f"평균 메모리 사용량 {avg_mem:.1f}%, "
                                f"평균 디스크 사용량 {avg_disk:.1f}%. "
                                f"총 {len(host_data)}개의 메트릭이 수집되었습니다."
                            )
                            
                            result.append({
                                "task_type": "summary",
                                "original_text": original_text,
                                "summary": summary
                            })
                except Exception as e:
                    logger.error(f"시스템 성능 요약 생성 실패: {str(e)}")
        
        logger.info(f"시스템 메트릭 처리 완료: {len(result)}개 학습 데이터 생성")
        return result