import unittest
from unittest.mock import patch, MagicMock
import json
import os
import sys

# Add the parent directory to the path so we can import the modules
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from ollama_client import OllamaClient

class TestOllamaClient(unittest.TestCase):
    
    def setUp(self):
        # Set environment variables for testing
        os.environ["OLLAMA_API_URL"] = "http://test-ollama:11434"
        os.environ["OLLAMA_MODEL"] = "test-model"
        os.environ["OLLAMA_TEMPERATURE"] = "0.5"
        
        self.client = OllamaClient()
    
    def tearDown(self):
        # Clean up environment variables
        for var in ["OLLAMA_API_URL", "OLLAMA_MODEL", "OLLAMA_TEMPERATURE"]:
            if var in os.environ:
                del os.environ[var]
    
    def test_init(self):
        """Test initialization of OllamaClient"""
        self.assertEqual(self.client.base_url, "http://test-ollama:11434")
        self.assertEqual(self.client.model_name, "test-model")
        self.assertEqual(self.client.default_params["temperature"], 0.5)
    
    @patch('requests.get')
    def test_check_status_online(self, mock_get):
        """Test check_status when Ollama is online"""
        # Mock response
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {"models": [{"name": "model1"}, {"name": "model2"}]}
        mock_get.return_value = mock_response
        
        # Call the method
        status = self.client.check_status()
        
        # Assertions
        self.assertEqual(status["status"], "online")
        self.assertEqual(status["models"], 2)
        mock_get.assert_called_once_with("http://test-ollama:11434/api/tags")
    
    @patch('requests.get')
    def test_check_status_error(self, mock_get):
        """Test check_status when Ollama returns an error"""
        # Mock response
        mock_response = MagicMock()
        mock_response.status_code = 500
        mock_response.text = "Internal Server Error"
        mock_get.return_value = mock_response
        
        # Call the method
        status = self.client.check_status()
        
        # Assertions
        self.assertEqual(status["status"], "error")
        self.assertEqual(status["message"], "Internal Server Error")
    
    @patch('requests.get')
    def test_check_status_offline(self, mock_get):
        """Test check_status when Ollama is offline"""
        # Mock exception
        mock_get.side_effect = Exception("Connection refused")
        
        # Call the method
        status = self.client.check_status()
        
        # Assertions
        self.assertEqual(status["status"], "offline")
        self.assertEqual(status["error"], "Connection refused")
    
    @patch('requests.get')
    @patch('requests.post')
    def test_ensure_model_loaded_already_exists(self, mock_post, mock_get):
        """Test ensure_model_loaded when model already exists"""
        # Mock response for GET
        mock_get_response = MagicMock()
        mock_get_response.status_code = 200
        mock_get_response.json.return_value = {"models": [{"name": "test-model"}, {"name": "other-model"}]}
        mock_get.return_value = mock_get_response
        
        # Call the method
        result = self.client.ensure_model_loaded()
        
        # Assertions
        self.assertTrue(result)
        mock_get.assert_called_once()
        mock_post.assert_not_called()
    
    @patch('requests.get')
    @patch('requests.post')
    def test_ensure_model_loaded_download_success(self, mock_post, mock_get):
        """Test ensure_model_loaded when model needs to be downloaded"""
        # Mock response for GET
        mock_get_response = MagicMock()
        mock_get_response.status_code = 200
        mock_get_response.json.return_value = {"models": [{"name": "other-model"}]}
        mock_get.return_value = mock_get_response
        
        # Mock response for POST
        mock_post_response = MagicMock()
        mock_post_response.status_code = 200
        mock_post.return_value = mock_post_response
        
        # Call the method
        result = self.client.ensure_model_loaded()
        
        # Assertions
        self.assertTrue(result)
        mock_get.assert_called_once()
        mock_post.assert_called_once_with(
            "http://test-ollama:11434/api/pull",
            json={"name": "test-model"}
        )
    
    @patch('requests.post')
    def test_generate_text_success(self, mock_post):
        """Test generate_text with successful response"""
        # Mock response
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.text = '{"response":"partial"}\n{"response":" text","eval_count":100,"eval_duration":1.5}'
        mock_post.return_value = mock_response
        
        # Call the method
        result = self.client.generate_text("Test prompt")
        
        # Assertions
        self.assertEqual(result["result"], " text")
        self.assertEqual(result["eval_count"], 100)
        self.assertEqual(result["eval_duration"], 1.5)
        mock_post.assert_called_once()
    
    @patch('requests.post')
    def test_generate_text_error(self, mock_post):
        """Test generate_text with error response"""
        # Mock response
        mock_response = MagicMock()
        mock_response.status_code = 500
        mock_response.text = "Internal Server Error"
        mock_post.return_value = mock_response
        
        # Call the method
        result = self.client.generate_text("Test prompt")
        
        # Assertions
        self.assertEqual(result["result"], "")
        self.assertEqual(result["error"], "Internal Server Error")
    
    @patch.object(OllamaClient, 'generate_text')
    def test_analyze_sentiment(self, mock_generate_text):
        """Test analyze_sentiment with valid JSON response"""
        # Mock generate_text response
        mock_generate_text.return_value = {
            "result": 'Analysis: {"sentiment": "positive", "confidence": 0.85, "explanation": "The text contains positive words."}'
        }
        
        # Call the method
        result = self.client.analyze_sentiment("This is a great day!")
        
        # Assertions
        self.assertEqual(result["result"], "positive")
        self.assertEqual(result["confidence"], 0.85)
        self.assertEqual(result["details"]["explanation"], "The text contains positive words.")
    
    @patch.object(OllamaClient, 'generate_text')
    def test_detect_anomaly(self, mock_generate_text):
        """Test detect_anomaly with valid JSON response"""
        # Mock generate_text response
        mock_generate_text.return_value = {
            "result": 'Analysis: {"anomaly_status": "warning", "confidence": 0.75, "detected_issues": ["High CPU usage"], "explanation": "The log shows high CPU usage."}'
        }
        
        # Call the method
        result = self.client.detect_anomaly("CPU usage at 95%")
        
        # Assertions
        self.assertEqual(result["result"], "warning")
        self.assertEqual(result["confidence"], 0.75)
        self.assertEqual(result["details"]["detected_issues"], ["High CPU usage"])
        self.assertEqual(result["details"]["explanation"], "The log shows high CPU usage.")
    
    @patch.object(OllamaClient, 'generate_text')
    def test_generate_summary(self, mock_generate_text):
        """Test generate_summary"""
        # Mock generate_text response
        mock_generate_text.return_value = {
            "result": "This is a summary of the text.",
            "eval_count": 50,
            "eval_duration": 0.8
        }
        
        # Call the method
        result = self.client.generate_summary("This is a long text that needs to be summarized.")
        
        # Assertions
        self.assertEqual(result["result"], "This is a summary of the text.")
        self.assertEqual(result["details"]["eval_count"], 50)
        self.assertEqual(result["details"]["eval_duration"], 0.8)
    
    def test_create_fine_tuning_prompt_sentiment(self):
        """Test create_fine_tuning_prompt for sentiment analysis"""
        examples = [
            {
                "text": "I love this product!",
                "sentiment": "positive",
                "explanation": "Contains positive words like 'love'"
            },
            {
                "text": "This is terrible.",
                "sentiment": "negative",
                "explanation": "Contains negative words like 'terrible'"
            }
        ]
        
        prompt = self.client.create_fine_tuning_prompt(examples, "sentiment")
        
        # Basic assertions
        self.assertIn("감정 분석 학습", prompt)
        self.assertIn("I love this product!", prompt)
        self.assertIn("This is terrible.", prompt)
        self.assertIn("positive", prompt)
        self.assertIn("negative", prompt)
    
    def test_create_fine_tuning_prompt_anomaly(self):
        """Test create_fine_tuning_prompt for anomaly detection"""
        examples = [
            {
                "log_text": "CPU usage at 95%",
                "anomaly_status": "warning",
                "detected_issues": ["High CPU usage"],
                "explanation": "CPU usage is very high"
            }
        ]
        
        prompt = self.client.create_fine_tuning_prompt(examples, "anomaly")
        
        # Basic assertions
        self.assertIn("이상 탐지 학습", prompt)
        self.assertIn("CPU usage at 95%", prompt)
        self.assertIn("warning", prompt)
        self.assertIn("High CPU usage", prompt)
    
    def test_create_fine_tuning_prompt_summary(self):
        """Test create_fine_tuning_prompt for text summarization"""
        examples = [
            {
                "original_text": "This is a long text that needs to be summarized.",
                "summary": "This is a summary."
            }
        ]
        
        prompt = self.client.create_fine_tuning_prompt(examples, "summary")
        
        # Basic assertions
        self.assertIn("텍스트 요약 학습", prompt)
        self.assertIn("This is a long text that needs to be summarized.", prompt)
        self.assertIn("This is a summary.", prompt)
    
    def test_create_fine_tuning_prompt_unsupported(self):
        """Test create_fine_tuning_prompt with unsupported task type"""
        prompt = self.client.create_fine_tuning_prompt([], "unsupported_task")
        self.assertEqual(prompt, "")

if __name__ == '__main__':
    unittest.main()