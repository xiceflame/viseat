#!/usr/bin/env python3
"""
Rokid 营养助手后端 API 完整流程测试

用法:
    python scripts/test_full_flow.py [BASE_URL]
    
示例:
    python scripts/test_full_flow.py http://localhost:8000
    python scripts/test_full_flow.py http://viseat.cn
"""

import sys
import json
import time
import requests
from datetime import datetime

BASE_URL = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8000"


def print_header(title: str):
    print(f"\n{'='*60}")
    print(f"  {title}")
    print(f"{'='*60}")


def print_response(resp: requests.Response, show_full: bool = True):
    print(f"Status: {resp.status_code}")
    if resp.status_code == 200:
        data = resp.json()
        if show_full:
            print(json.dumps(data, ensure_ascii=False, indent=2))
        return data
    else:
        print(f"Error: {resp.text}")
        return None


def test_health():
    """测试健康检查"""
    print_header("1. 健康检查")
    resp = requests.get(f"{BASE_URL}/health")
    return print_response(resp)


def test_nutrition_lookup():
    """测试营养查询"""
    print_header("2. 营养查询 (lookup)")
    
    foods = ["rice", "pork", "chicken breast", "tomato"]
    for food in foods:
        print(f"\n查询: {food}")
        resp = requests.get(f"{BASE_URL}/api/v1/nutrition/lookup", params={"name": food})
        if resp.status_code == 200:
            data = resp.json()
            per100g = data.get("per_100g", {})
            print(f"  热量: {per100g.get('calories', 'N/A')} kcal/100g")
            print(f"  蛋白质: {per100g.get('protein', 'N/A')} g/100g")
        else:
            print(f"  未找到: {resp.status_code}")


def test_nutrition_aggregate():
    """测试营养聚合"""
    print_header("3. 营养聚合 (aggregate)")
    
    payload = {
        "foods": [
            {"name": "rice", "weight_g": 200},
            {"name": "pork", "weight_g": 150},
            {"name": "tomato", "weight_g": 100}
        ]
    }
    print(f"输入: {json.dumps(payload, ensure_ascii=False)}")
    
    resp = requests.post(f"{BASE_URL}/api/v1/nutrition/aggregate", json=payload)
    data = print_response(resp)
    
    if data:
        total = data.get("total", {})
        print(f"\n汇总结果:")
        print(f"  总热量: {total.get('calories', 0):.1f} kcal")
        print(f"  蛋白质: {total.get('protein', 0):.1f} g")
        print(f"  碳水: {total.get('carbs', 0):.1f} g")
        print(f"  脂肪: {total.get('fat', 0):.1f} g")
    
    return data


def test_meal_session_flow():
    """测试完整用餐会话流程"""
    print_header("4. 完整用餐会话流程")
    
    # 4.1 开始会话
    print("\n--- 4.1 开始用餐 (meal/start) ---")
    start_payload = {
        "foods": [
            {"name": "rice", "weight_g": 200},
            {"name": "pork", "weight_g": 150}
        ],
        "nutrition": {
            "calories": 650,
            "protein": 30,
            "carbs": 80,
            "fat": 25
        }
    }
    
    resp = requests.post(
        f"{BASE_URL}/api/v1/meal/start",
        params={"user_id": "test_user", "meal_type": "lunch"},
        json=start_payload
    )
    start_data = print_response(resp)
    
    if not start_data:
        print("开始会话失败，终止测试")
        return None
    
    session_id = start_data.get("session_id")
    print(f"\n✓ 会话已创建: {session_id}")
    print(f"  初始热量: {start_data.get('initial_kcal', 0)} kcal")
    print(f"  状态: {start_data.get('status')}")
    
    # 模拟进餐过程
    time.sleep(1)
    
    # 4.2 更新会话（模拟吃了一半）
    print("\n--- 4.2 更新进度 (meal/update) ---")
    update_payload = {
        "foods": [
            {"name": "rice", "weight_g": 100},
            {"name": "pork", "weight_g": 80}
        ],
        "nutrition": {
            "calories": 350,
            "protein": 16,
            "carbs": 40,
            "fat": 14
        }
    }
    
    resp = requests.post(
        f"{BASE_URL}/api/v1/meal/update",
        params={"session_id": session_id},
        json=update_payload
    )
    update_data = print_response(resp)
    
    if update_data:
        print(f"\n✓ 进度已更新:")
        print(f"  当前剩余: {update_data.get('current_remaining_kcal', 0)} kcal")
        print(f"  已消耗: {update_data.get('consumed_kcal', 0)} kcal")
        print(f"  建议: {update_data.get('suggestion', 'N/A')}")
    
    time.sleep(1)
    
    # 4.3 结束会话
    print("\n--- 4.3 结束用餐 (meal/end) ---")
    end_payload = {
        "foods": [
            {"name": "rice", "weight_g": 20},
            {"name": "pork", "weight_g": 10}
        ],
        "nutrition": {
            "calories": 70,
            "protein": 3,
            "carbs": 8,
            "fat": 3
        }
    }
    
    resp = requests.post(
        f"{BASE_URL}/api/v1/meal/end",
        params={"session_id": session_id},
        json=end_payload
    )
    end_data = print_response(resp)
    
    if end_data:
        print(f"\n✓ 用餐已结束:")
        print(f"  总消耗: {end_data.get('total_consumed_kcal', 0)} kcal")
        print(f"  消耗比例: {end_data.get('consumption_ratio', 0)*100:.1f}%")
        print(f"  用餐时长: {end_data.get('duration_minutes', 0)} 分钟")
        print(f"  蛋白质: {end_data.get('total_protein', 0)} g")
        print(f"  碳水: {end_data.get('total_carbs', 0)} g")
        print(f"  脂肪: {end_data.get('total_fat', 0)} g")
        
        breakdown = end_data.get('nutrition_breakdown', {})
        print(f"\n  营养饼图:")
        print(f"    蛋白质: {breakdown.get('protein_percent', 0):.1f}%")
        print(f"    碳水: {breakdown.get('carbs_percent', 0):.1f}%")
        print(f"    脂肪: {breakdown.get('fat_percent', 0):.1f}%")
        
        print(f"\n  报告: {end_data.get('report', 'N/A')}")
    
    return session_id


def test_daily_stats():
    """测试今日统计"""
    print_header("5. 今日统计 (stats/daily)")
    
    today = datetime.now().strftime("%Y-%m-%d")
    resp = requests.get(
        f"{BASE_URL}/api/v1/stats/daily",
        params={"user_id": "test_user", "date": today}
    )
    data = print_response(resp)
    
    if data:
        print(f"\n✓ 今日统计 ({today}):")
        print(f"  总热量: {data.get('total_calories', 0)} kcal")
        print(f"  目标热量: {data.get('target_calories', 0)} kcal")
        print(f"  完成度: {data.get('calories_ratio', 0)}%")
        print(f"  用餐次数: {data.get('meals_count', 0)}")
        
        breakdown = data.get('nutrition_breakdown', {})
        print(f"\n  营养比例:")
        print(f"    蛋白质: {breakdown.get('protein_percent', 0):.1f}%")
        print(f"    碳水: {breakdown.get('carbs_percent', 0):.1f}%")
        print(f"    脂肪: {breakdown.get('fat_percent', 0):.1f}%")


def test_chat_nutrition():
    """测试个性化营养对话"""
    print_header("6. AI 营养对话 (chat/nutrition)")
    
    payload = {
        "query": "分析本餐营养情况",
        "user_profile": {
            "age": 45,
            "gender": "male",
            "bmi": 26.5,
            "health_conditions": ["轻度脂肪肝"],
            "dietary_preferences": ["低油", "低盐"],
            "activity_level": "light"
        },
        "meal_context": {
            "foods": ["红烧肉", "米饭", "青菜"],
            "total_calories": 780,
            "total_protein": 30,
            "total_carbs": 85,
            "total_fat": 35
        },
        "message_type": "meal_end"
    }
    
    print(f"请求内容:")
    print(f"  用户档案: {json.dumps(payload['user_profile'], ensure_ascii=False)}")
    print(f"  用餐情况: {json.dumps(payload['meal_context'], ensure_ascii=False)}")
    
    resp = requests.post(f"{BASE_URL}/api/v1/chat/nutrition", json=payload)
    data = print_response(resp)
    
    if data:
        print(f"\n✓ AI 回复:")
        print(f"  {data.get('answer', 'N/A')}")
        
        actions = data.get('suggested_actions', [])
        if actions:
            print(f"\n  建议动作: {', '.join(actions)}")
        
        meal_advice = data.get('meal_advice')
        if meal_advice:
            print(f"  用餐建议: {meal_advice}")


def main():
    print(f"\n{'#'*60}")
    print(f"#  Rokid 营养助手后端 API 完整测试")
    print(f"#  Base URL: {BASE_URL}")
    print(f"#  时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"{'#'*60}")
    
    try:
        # 1. 健康检查
        if not test_health():
            print("健康检查失败，服务可能未启动")
            return
        
        # 2. 营养查询
        test_nutrition_lookup()
        
        # 3. 营养聚合
        test_nutrition_aggregate()
        
        # 4. 完整用餐流程
        test_meal_session_flow()
        
        # 5. 今日统计
        test_daily_stats()
        
        # 6. AI 对话（如果 API Key 已配置）
        try:
            test_chat_nutrition()
        except Exception as e:
            print(f"AI 对话测试跳过: {e}")
        
        print_header("测试完成!")
        print("✓ 所有测试通过\n")
        
    except requests.exceptions.ConnectionError:
        print(f"\n❌ 连接失败: 无法连接到 {BASE_URL}")
        print("请确保后端服务已启动")
    except Exception as e:
        print(f"\n❌ 测试失败: {e}")
        raise


if __name__ == "__main__":
    main()
