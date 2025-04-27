import unittest
from unittest.mock import patch, MagicMock, mock_open
import json
import os
import sys
import pandas as pd
from datetime import datetime, timedelta

# Add the parent directory to the path so we can import the modules
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from data_processor import DataProcessor

class TestDataProcessor(unittest.TestCase):
    
    def setUp(self):
        # Set environment variables for testing
        os.environ["ELASTICSEARCH_HOST"] = "test-elasticsearch"
        os.environ["ELASTICSEARCH_PORT"] = "9200"
        os.environ["ELASTICSEARCH_USER"] = "test-user"
        os.environ["ELASTICSEARCH_PASSWORD"] = "test-password"
        os.environ["DATA_DIR"] = "test_data"
        
        # Mock the Elasticsearch client
        self.es_client_patcher = patch('elasticsearch.Elasticsearch')
        self.mock_es_client = self.es_client_patcher.start()
        
        # Mock os.makedirs
        self.makedirs_patcher = patch('os.makedirs')
        self.mock_makedirs = self.makedirs_patcher.start()
        
        # Mock os.path.exists
        self.exists_patcher = patch('os.path.exists')
        self.mock_exists = self.exists_patcher.start()
        self.mock_exists.return_value = False  # Default to file not existing
        
        # Mock open
        self.open_patcher = patch('builtins.open', mock_open())
        self.mock_open = self.open_patcher.start()
        
        # Create the data processor
        self.data_processor = DataProcessor()
        
        # Set the Elasticsearch client mock
        self.data_processor.es_client = MagicMock()
    
    def tearDown(self):
        # Clean up environment variables
        for var in ["ELASTICSEARCH_HOST", "ELASTICSEARCH_PORT", "ELASTICSEARCH_USER", "ELASTICSEARCH_PASSWORD", "DATA_DIR"]:
            if var in os.environ:
                del os.environ[var]
        
        # Stop all patches
        self.es_client_patcher.stop()
        self.makedirs_patcher.stop()
        self.exists_patcher.stop()
        self.open_patcher.stop()
    
    def test_init(self):
        """Test initialization of DataProcessor"""
        self.assertEqual(self.data_processor.es_host, "test-elasticsearch")
        self.assertEqual(self.data_processor.es_port, 9200)
        self.assertEqual(self.data_processor.es_user, "test-user")
        self.assertEqual(self.data_processor.es_password, "test-password")
        self.assertEqual(self.data_processor.data_dir, "test_data")
        self.assertEqual(self.data_processor.last_processed_file, os.path.join("test_data", "last_processed.json"))
        self.assertEqual(self.data_processor.processed_data_file, os.path.join("test_data", "processed_data.json"))
        self.mock_makedirs.assert_called_once_with("test_data", exist_ok=True)
    
    def test_load_last_processed_default(self):
        """Test _load_last_processed when file doesn't exist"""
        # Mock os.path.exists to return False
        self.mock_exists.return_value = False
        
        # Call the method
        result = self.data_processor._load_last_processed()
        
        # Assertions
        self.assertIn("application_logs", result)
        self.assertIn("nginx_access", result)
        self.assertIn("system_metrics", result)
        
        # Check that all values are datetime objects
        for key, value in result.items():
            self.assertIsInstance(value, datetime)
    
    def test_load_last_processed_from_file(self):
        """Test _load_last_processed when file exists"""
        # Mock os.path.exists to return True
        self.mock_exists.return_value = True
        
        # Mock json.load to return test data
        test_data = {
            "application_logs": "2023-01-01T12:00:00",
            "nginx_access": "2023-01-01T12:00:00",
            "system_metrics": "2023-01-01T12:00:00"
        }
        
        # Mock open to return file with test data
        self.mock_open.return_value.__enter__.return_value.read.return_value = json.dumps(test_data)
        
        # Mock json.load
        with patch('json.load', return_value=test_data):
            # Call the method
            result = self.data_processor._load_last_processed()
        
        # Assertions
        self.assertIn("application_logs", result)
        self.assertIn("nginx_access", result)
        self.assertIn("system_metrics", result)
        
        # Check that all values are datetime objects
        for key, value in result.items():
            self.assertIsInstance(value, datetime)
            self.assertEqual(value, datetime.fromisoformat("2023-01-01T12:00:00"))
    
    def test_save_last_processed(self):
        """Test _save_last_processed"""
        # Set up test data
        self.data_processor.last_processed = {
            "application_logs": datetime(2023, 1, 1, 12, 0, 0),
            "nginx_access": datetime(2023, 1, 1, 12, 0, 0),
            "system_metrics": datetime(2023, 1, 1, 12, 0, 0)
        }
        
        # Call the method
        self.data_processor._save_last_processed()
        
        # Assertions
        self.mock_open.assert_called_once_with(self.data_processor.last_processed_file, 'w')
        handle = self.mock_open()
        handle.write.assert_called_once()
        
        # Check that the written data contains the expected timestamps
        written_data = handle.write.call_args[0][0]
        self.assertIn("2023-01-01T12:00:00", written_data)
    
    def test_get_latest_processed_data_empty(self):
        """Test get_latest_processed_data when file doesn't exist"""
        # Mock os.path.exists to return False
        self.mock_exists.return_value = False
        
        # Call the method
        result = self.data_processor.get_latest_processed_data()
        
        # Assertions
        self.assertEqual(result, [])
    
    def test_get_latest_processed_data_with_data(self):
        """Test get_latest_processed_data when file exists with data"""
        # Mock os.path.exists to return True
        self.mock_exists.return_value = True
        
        # Mock json.load to return test data
        test_data = [{"task_type": "anomaly", "log_text": "test log"}]
        
        # Mock open to return file with test data
        self.mock_open.return_value.__enter__.return_value.read.return_value = json.dumps(test_data)
        
        # Mock json.load
        with patch('json.load', return_value=test_data):
            # Call the method
            result = self.data_processor.get_latest_processed_data()
        
        # Assertions
        self.assertEqual(result, test_data)
    
    def test_fetch_application_logs(self):
        """Test _fetch_application_logs"""
        # Mock Elasticsearch search response
        mock_response = {
            "hits": {
                "hits": [
                    {"_source": {"timestamp": "2023-01-01T12:00:00", "service": "test-service", "log_level": "INFO", "message": "Test message"}},
                    {"_source": {"timestamp": "2023-01-01T12:01:00", "service": "test-service", "log_level": "ERROR", "message": "Error message"}}
                ]
            }
        }
        self.data_processor.es_client.search.return_value = mock_response
        
        # Call the method
        start_time = datetime(2023, 1, 1, 12, 0, 0)
        end_time = datetime(2023, 1, 1, 13, 0, 0)
        result = self.data_processor._fetch_application_logs(start_time, end_time)
        
        # Assertions
        self.assertEqual(len(result), 2)
        self.data_processor.es_client.search.assert_called_once()
        
        # Check that the query contains the correct time range
        query = self.data_processor.es_client.search.call_args[1]['body']
        self.assertEqual(query['query']['range']['timestamp']['gte'], start_time.isoformat())
        self.assertEqual(query['query']['range']['timestamp']['lt'], end_time.isoformat())
    
    def test_process_application_logs(self):
        """Test _process_application_logs"""
        # Test data
        logs = [
            {"timestamp": "2023-01-01T12:00:00", "service": "test-service", "log_level": "INFO", "message": "Info message 1"},
            {"timestamp": "2023-01-01T12:01:00", "service": "test-service", "log_level": "INFO", "message": "Info message 2"},
            {"timestamp": "2023-01-01T12:02:00", "service": "test-service", "log_level": "WARNING", "message": "Warning message"},
            {"timestamp": "2023-01-01T12:03:00", "service": "test-service", "log_level": "ERROR", "message": "Error message", "stack_trace": "Stack trace"}
        ]
        
        # Call the method
        result = self.data_processor._process_application_logs(logs)
        
        # Assertions
        self.assertGreaterEqual(len(result), 2)  # At least warning and error logs
        
        # Check that we have the expected task types
        task_types = [item["task_type"] for item in result]
        self.assertIn("anomaly", task_types)
        
        # Check anomaly statuses
        anomaly_items = [item for item in result if item["task_type"] == "anomaly"]
        anomaly_statuses = [item["anomaly_status"] for item in anomaly_items]
        self.assertIn("warning", anomaly_statuses)
        self.assertIn("critical", anomaly_statuses)
    
    def test_process_new_logs(self):
        """Test process_new_logs"""
        # Mock the fetch and process methods
        with patch.object(self.data_processor, '_fetch_application_logs', return_value=[{"log": "app log"}]) as mock_fetch_app, \
             patch.object(self.data_processor, '_fetch_nginx_logs', return_value=[{"log": "nginx log"}]) as mock_fetch_nginx, \
             patch.object(self.data_processor, '_fetch_system_metrics', return_value=[{"metric": "system metric"}]) as mock_fetch_sys, \
             patch.object(self.data_processor, '_process_application_logs', return_value=[{"processed": "app log"}]) as mock_process_app, \
             patch.object(self.data_processor, '_process_nginx_logs', return_value=[{"processed": "nginx log"}]) as mock_process_nginx, \
             patch.object(self.data_processor, '_process_system_metrics', return_value=[{"processed": "system metric"}]) as mock_process_sys, \
             patch.object(self.data_processor, '_save_last_processed') as mock_save_last, \
             patch.object(self.data_processor, '_save_processed_data') as mock_save_data:
            
            # Call the method
            result = self.data_processor.process_new_logs()
            
            # Assertions
            self.assertEqual(len(result), 3)  # One from each source
            mock_fetch_app.assert_called_once()
            mock_fetch_nginx.assert_called_once()
            mock_fetch_sys.assert_called_once()
            mock_process_app.assert_called_once_with([{"log": "app log"}])
            mock_process_nginx.assert_called_once_with([{"log": "nginx log"}])
            mock_process_sys.assert_called_once_with([{"metric": "system metric"}])
            mock_save_last.assert_called_once()
            mock_save_data.assert_called_once_with(result)

if __name__ == '__main__':
    unittest.main()