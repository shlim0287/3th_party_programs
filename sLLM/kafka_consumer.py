import os
import json
import logging
import threading
import time
from typing import Dict, Any, List, Optional
from kafka import KafkaConsumer
from kafka.errors import KafkaError

logger = logging.getLogger(__name__)

class KafkaLogConsumer:
    """
    Kafka 로그 컨슈머 클래스
    Kafka 토픽에서 로그 데이터를 소비하고 처리합니다.
    """

    def __init__(self, data_processor):
        """
        Kafka 로그 컨슈머 초기화

        Args:
            data_processor: 로그 데이터를 처리할 데이터 프로세서 인스턴스
        """
        # Kafka 설정
        self.bootstrap_servers = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:29092")
        self.group_id = os.getenv("KAFKA_GROUP_ID", "sllm-consumer-group")
        self.topics = [
            os.getenv("KAFKA_APPLICATION_LOGS_TOPIC", "application-logs"),
            os.getenv("KAFKA_NGINX_LOGS_TOPIC", "nginx-access-logs"),
            os.getenv("KAFKA_SYSTEM_METRICS_TOPIC", "beats-logs")
        ]

        # 데이터 프로세서
        self.data_processor = data_processor

        # 컨슈머 인스턴스
        self.consumer = None

        # 실행 상태 플래그
        self.running = False

        # 컨슈머 스레드
        self.consumer_thread = None

        logger.info(f"Kafka 로그 컨슈머 초기화: 서버={self.bootstrap_servers}, 토픽={self.topics}")

    def _create_consumer(self) -> Optional[KafkaConsumer]:
        """
        Kafka 컨슈머 인스턴스를 생성합니다.

        Returns:
            Optional[KafkaConsumer]: 생성된 Kafka 컨슈머 인스턴스 또는 None
        """
        try:
            consumer = KafkaConsumer(
                *self.topics,
                bootstrap_servers=self.bootstrap_servers,
                group_id=self.group_id,
                auto_offset_reset='earliest',
                enable_auto_commit=True,
                value_deserializer=lambda m: json.loads(m.decode('utf-8')),
                max_poll_interval_ms=300000,  # 5분
                session_timeout_ms=30000,     # 30초
                heartbeat_interval_ms=10000   # 10초
            )
            logger.info("Kafka 컨슈머 생성 성공")
            return consumer
        except KafkaError as e:
            logger.error(f"Kafka 컨슈머 생성 실패: {str(e)}")
            return None

    def start_consuming(self):
        """
        Kafka 토픽에서 메시지 소비를 시작합니다.
        """
        if self.running:
            logger.warning("Kafka 컨슈머가 이미 실행 중입니다")
            return

        self.running = True
        self.consumer = self._create_consumer()

        if not self.consumer:
            logger.error("Kafka 컨슈머를 시작할 수 없습니다")
            self.running = False
            return

        logger.info("Kafka 메시지 소비 시작")

        try:
            # 메시지 배치 처리를 위한 변수
            messages_batch = []
            last_process_time = 0
            batch_size = int(os.getenv("KAFKA_BATCH_SIZE", "100"))
            batch_timeout = int(os.getenv("KAFKA_BATCH_TIMEOUT", "60"))  # 초 단위

            # 메시지 소비 루프
            for message in self.consumer:
                if not self.running:
                    break

                try:
                    # 메시지 추가
                    messages_batch.append(message.value)

                    # 배치 처리 조건 확인
                    current_time = int(time.time())
                    batch_full = len(messages_batch) >= batch_size
                    timeout_reached = (current_time - last_process_time) >= batch_timeout

                    if batch_full or timeout_reached:
                        if messages_batch:
                            self._process_messages_batch(messages_batch)
                            messages_batch = []
                            last_process_time = current_time

                except Exception as e:
                    logger.error(f"메시지 처리 중 오류 발생: {str(e)}")

            # 남은 메시지 처리
            if messages_batch:
                self._process_messages_batch(messages_batch)

        except Exception as e:
            logger.error(f"Kafka 메시지 소비 중 오류 발생: {str(e)}")
        finally:
            self.running = False
            if self.consumer:
                self.consumer.close()
                logger.info("Kafka 컨슈머 종료")

    def _process_messages_batch(self, messages: List[Dict[str, Any]]):
        """
        메시지 배치를 처리합니다.

        Args:
            messages (List[Dict[str, Any]]): 처리할 메시지 배치
        """
        try:
            logger.info(f"메시지 배치 처리 시작: {len(messages)}개")

            # 메시지 유형별 분류
            app_logs = []
            nginx_logs = []
            system_metrics = []

            for msg in messages:
                # 메시지 유형 확인
                msg_type = msg.get('type')

                if msg_type == 'application':
                    app_logs.append(msg)
                elif msg_type == 'nginx-access':
                    nginx_logs.append(msg)
                elif msg_type == 'beats':
                    system_metrics.append(msg)

            # 데이터 프로세서에 전달
            if app_logs:
                logger.info(f"애플리케이션 로그 {len(app_logs)}개 처리")
                self.data_processor._process_application_logs(app_logs)

            if nginx_logs:
                logger.info(f"Nginx 액세스 로그 {len(nginx_logs)}개 처리")
                self.data_processor._process_nginx_logs(nginx_logs)

            if system_metrics:
                logger.info(f"시스템 메트릭 {len(system_metrics)}개 처리")
                self.data_processor._process_system_metrics(system_metrics)

            logger.info(f"메시지 배치 처리 완료: {len(messages)}개")

        except Exception as e:
            logger.error(f"메시지 배치 처리 중 오류 발생: {str(e)}")

    def stop_consuming(self):
        """
        Kafka 메시지 소비를 중지합니다.
        """
        logger.info("Kafka 메시지 소비 중지 요청")
        self.running = False

        if self.consumer_thread and self.consumer_thread.is_alive():
            self.consumer_thread.join(timeout=5.0)
            logger.info("Kafka 컨슈머 스레드 종료")

    def start_in_thread(self):
        """
        별도의 스레드에서 Kafka 메시지 소비를 시작합니다.
        """
        if self.consumer_thread and self.consumer_thread.is_alive():
            logger.warning("Kafka 컨슈머 스레드가 이미 실행 중입니다")
            return

        self.consumer_thread = threading.Thread(target=self.start_consuming, daemon=True)
        self.consumer_thread.start()
        logger.info("Kafka 컨슈머 스레드 시작")
