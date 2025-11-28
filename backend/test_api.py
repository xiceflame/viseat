from fastapi.testclient import TestClient
from app.main import app
import os
from pathlib import Path

client = TestClient(app)

def test_read_main():
    response = client.get("/")
    assert response.status_code == 200
    assert response.json()["status"] == "running"

def test_health_check():
    response = client.get("/health")
    assert response.status_code == 200
    assert "nutrition_db_loaded" in response.json()

def test_upload_image():
    # 创建一个临时的测试文件
    test_content = b"fake image content"
    files = {"file": ("test.jpg", test_content, "image/jpeg")}
    
    response = client.post("/api/v1/upload", files=files)
    assert response.status_code == 200
    data = response.json()
    assert "url" in data
    assert data["url"].startswith("/uploads/")
    
    # 清理：实际文件会被创建在 uploads 目录，测试后可以清理，或者留给 cleanup 脚本
    # 这里简单检查文件是否存在
    filename = data["filename"]
    assert (Path("uploads") / filename).exists()
    
    # 删除测试文件
    (Path("uploads") / filename).unlink()

def test_lookup_nutrition():
    # 测试查表功能
    response = client.get("/api/v1/nutrition/lookup?name=米饭")
    assert response.status_code == 200
    data = response.json()
    assert data["name"] == "米饭"
    assert data["per_100g"]["calories"] == 116

def test_calculate_nutrition_aggregate():
    payload = {
        "foods": [
            {"name": "米饭", "weight_g": 200},
            {"name": "鸡胸肉", "weight_g": 100}
        ]
    }
    response = client.post("/api/v1/nutrition/aggregate", json=payload)
    assert response.status_code == 200
    data = response.json()
    
    # 米饭 116*2 = 232, 鸡胸肉 165*1 = 165. 总热量 397
    total_cals = data["total"]["calories"]
    assert 390 < total_cals < 410  # 允许一点浮动
