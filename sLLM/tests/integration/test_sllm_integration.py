import unittest
import os
import sys
import json
from unittest.mock import patch, MagicMock
import requests

# Add the parent directory to the path so we can import the modules
sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from ollama_client import OllamaClient
from data_processor import DataProcessor

class TestSLLMIntegration(unittest.TestCase):
    
    def setUp(self):
        # Set environment variables for testing
        os.environ["ELASTICSEARCH_HOST"] = "localhost"
        os.environ["ELASTICSEARCH_PORT"] = "9200"
        os.environ["OLLAMA_API_URL"] = "http://localhost:11434"
        os.environ["OLLAMA_MODEL"] = "llama3"
        
        # Initialize components
        self.ollama_client = OllamaClient()
        self.data_processor = DataProcessor()
    
    def tearDown(self):
        # Clean up environment variables
        for var in ["ELASTICSEARCH_HOST", "ELASTICSEARCH_PORT", "OLLAMA_API_URL", "OLLAMA_MODEL"]:
            if var in os.environ:
                del os.environ[var]
    
    @patch('elasticsearch.Elasticsearch')
    @patch('requests.get')
    def test_elasticsearch_connection(self, mock_requests_get, mock_es):
        """Test that the data processor can connect to Elasticsearch"""
        # Mock Elasticsearch client
        mock_es_instance = MagicMock()
        mock_es.return_value = mock_es_instance
        
        # Mock successful connection
        mock_es_instance.info.return_value = {"version": {"number": "7.10.0"}}
        
        # Create a new data processor to test the connection
        data_processor = DataProcessor()
        
        # Assert that the Elasticsearch client was created
        self.assertIsNotNone(data_processor.es_client)
        
        # Mock Ollama status check
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {"models": [{"name": "llama3"}]}
        mock_requests_get.return_value = mock_response
        
        # Check Ollama status
        status = self.ollama_client.check_status()
        
        # Assert that Ollama is online
        self.assertEqual(status["status"], "online")
    
    @patch('requests.post')
    def test_ollama_text_generation(self, mock_post):
        """Test that the Ollama client can generate text"""
        # Mock successful response
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.text = '{"response":"This is a test response","eval_count":100,"eval_duration":1.5}'
        mock_post.return_value = mock_response
        
        # Generate text
        result = self.ollama_client.generate_text("Test prompt")
        
        # Assert that text was generated
        self.assertEqual(result["result"], "This is a test response")
        self.assertEqual(result["eval_count"], 100)
        self.assertEqual(result["eval_duration"], 1.5)
        
        # Verify the request
        mock_post.assert_called_once()
        args, kwargs = mock_post.call_args
        self.assertEqual(args[0], "http://localhost:11434/api/generate")
        self.assertEqual(kwargs["json"]["model"], "llama3")
        self.assertEqual(kwargs["json"]["prompt"], "Test prompt")
    
    @patch('requests.post')
    def test_sentiment_analysis(self, mock_post):
        """Test sentiment analysis integration"""
        # Mock successful response
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.text = '{"response":"{\\"sentiment\\": \\"positive\\", \\"confidence\\": 0.85, \\"explanation\\": \\"The text contains positive words.\\"}","eval_count":100,"eval_duration":1.5}'
        mock_post.return_value = mock_response
        
        # Analyze sentiment
        result = self.ollama_client.analyze_sentiment("This is a great day!")
        
        # Assert that sentiment was analyzed
        self.assertEqual(result["result"], "positive")
        self.assertEqual(result["confidence"], 0.85)
        self.assertEqual(result["details"]["explanation"], "The text contains positive words.")
    
    @patch('elasticsearch.Elasticsearch')
    def test_data_processing_flow(self, mock_es):
        """Test the data processing flow"""
        # Mock Elasticsearch client
        mock_es_instance = MagicMock()
        mock_es.return_value = mock_es_instance
        
        # Mock search results for application logs
        app_logs = [
            {
                "timestamp": "2023-01-01T12:00:00",
                "service": "test-service",
                "log_level": "ERROR",
                "message": "Test error message",
                "stack_trace": "Test stack trace"
            }
        ]
        
        # Mock search results for Nginx logs
        nginx_logs = [
            {
                "timestamp": "2023-01-01T12:00:00",
                "client_ip": "127.0.0.1",
                "request_method": "GET",
                "request_path": "/api/test",
                "status_code": 500,
                "response_time": 100
            }
        ]
        
        # Mock search results for system metrics
        system_metrics = [
            {
                "timestamp": "2023-01-01T12:00:00",
                "host": "test-host",
                "cpu_usage": 90,
                "memory_usage": 85,
                "disk_usage": 70
            }
        ]
        
        # Mock search responses
        mock_es_instance.search.side_effect = [
            {"hits": {"hits": [{"_source": log} for log in app_logs]}},
            {"hits": {"hits": [{"_source": log} for log in nginx_logs]}},
            {"hits": {"hits": [{"_source": log} for log in system_metrics]}}
        ]
        
        # Create a new data processor with the mocked Elasticsearch client
        data_processor = DataProcessor()
        data_processor.es_client = mock_es_instance
        
        # Process logs
        with patch.object(data_processor, '_save_processed_data'):
            with patch.object(data_processor, '_save_last_processed'):
                processed_data = data_processor.process_new_logs()
        
        # Assert that logs were processed
        self.assertGreater(len(processed_data), 0)
        
        # Check that we have anomaly detection data
        anomaly_data = [item for item in processed_data if item.get("task_type") == "anomaly"]
        self.assertGreater(len(anomaly_data), 0)
        
        # Verify that Elasticsearch was queried
        self.assertEqual(mock_es_instance.search.call_count, 3)

if __name__ == '__main__':
    unittest.main()