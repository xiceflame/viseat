from fastapi.testclient import TestClient
import os

# 强制使用内存数据库进行测试，防止产生本地文件且保证隔离
os.environ["DATABASE_URL"] = "sqlite:///:memory:"

from app.main import app
from app.db import init_db
import uuid
import pytest

client = TestClient(app)

@pytest.fixture(autouse=True)
def setup_database():
    # 每次测试前初始化数据库
    init_db()
    yield
    # 内存数据库会在进程结束时销毁，或者可以显式 drop_all (如果需要重置状态)

def test_meal_session_lifecycle():
    # 1. 开始会话
    start_payload = {
        "foods": [
            {"name": "米饭", "weight_g": 150}
        ]
    }
    response = client.post("/api/v1/meal/start?user_id=test_user", json=start_payload)
    assert response.status_code == 200
    session_data = response.json()
    session_id = session_data["session_id"]
    assert session_id is not None
    assert session_data["status"] == "started"
    
    # 2. 更新会话（比如吃了一半）
    update_payload = {
        "foods": [
            {"name": "米饭", "weight_g": 75}
        ]
    }
    response = client.post(f"/api/v1/meal/update?session_id={session_id}", json=update_payload)
    assert response.status_code == 200
    update_data = response.json()
    # 初始150g(174kcal) -> 剩余75g(87kcal) -> 消耗87kcal
    assert update_data["current_status"]["consumed_total"] > 0
    
    # 3. 结束会话
    end_payload = {
        "foods": [
            {"name": "米饭", "weight_g": 0} # 吃完了
        ]
    }
    response = client.post(f"/api/v1/meal/end?session_id={session_id}", json=end_payload)
    assert response.status_code == 200
    end_data = response.json()
    
    report = end_data["report"]
    assert isinstance(report, str)
    assert len(report) > 0
    
    # 4. 验证会话详情
    response = client.get(f"/api/v1/meal/session/{session_id}")
    assert response.status_code == 200
    detail = response.json()
    assert detail["session"]["status"] == "completed"
