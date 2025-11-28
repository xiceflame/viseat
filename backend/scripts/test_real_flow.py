#!/usr/bin/env python3
"""
Rokid 营养助手后端 - 真实数据流测试脚本

使用真实图片调用 Qwen-VL API，进行完整的用餐会话测试。

用法:
    python scripts/test_real_flow.py [IMAGE_PATH] [BASE_URL]
    
示例:
    python scripts/test_real_flow.py pics/牛肉饭-翻-converted.jpg http://localhost:8000
    python scripts/test_real_flow.py pics/牛肉饭.jpg http://viseat.cn
"""

import sys
import os
import json
import time
import base64
import requests
from datetime import datetime
from pathlib import Path

# 默认参数
DEFAULT_IMAGE = "pics/牛肉饭-翻-converted.jpg"
DEFAULT_URL = "http://localhost:8000"

# 解析命令行参数
SCRIPT_DIR = Path(__file__).parent.parent
IMAGE_PATH = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_IMAGE
BASE_URL = sys.argv[2] if len(sys.argv) > 2 else DEFAULT_URL

# 处理相对路径
if not os.path.isabs(IMAGE_PATH):
    IMAGE_PATH = SCRIPT_DIR / IMAGE_PATH


def print_header(title: str):
    print(f"\n{'='*60}")
    print(f"  {title}")
    print(f"{'='*60}")


def print_json(data: dict, title: str = None):
    if title:
        print(f"\n{title}:")
    print(json.dumps(data, ensure_ascii=False, indent=2))


def load_image_as_base64(image_path: str) -> str:
    """将本地图片转为 base64 data URI"""
    with open(image_path, "rb") as f:
        img_data = f.read()
    
    # 检测图片类型
    ext = os.path.splitext(image_path)[1].lower()
    mime_types = {
        ".jpg": "image/jpeg",
        ".jpeg": "image/jpeg",
        ".png": "image/png",
        ".gif": "image/gif",
        ".webp": "image/webp"
    }
    mime_type = mime_types.get(ext, "image/jpeg")
    
    b64_str = base64.b64encode(img_data).decode("utf-8")
    return f"data:{mime_type};base64,{b64_str}"


def test_vision_analyze(image_url: str) -> dict:
    """测试视觉分析 API - 使用真实 Qwen-VL"""
    print_header("1. 图像识别 (Qwen-VL)")
    
    print(f"图片: {IMAGE_PATH}")
    print(f"API: {BASE_URL}/api/v1/vision/analyze")
    print("正在调用 Qwen-VL API...")
    
    start_time = time.time()
    resp = requests.post(
        f"{BASE_URL}/api/v1/vision/analyze",
        json={"image_url": image_url}
    )
    elapsed = time.time() - start_time
    
    print(f"响应时间: {elapsed:.2f}s")
    print(f"状态码: {resp.status_code}")
    
    if resp.status_code != 200:
        print(f"❌ 错误: {resp.text}")
        return None
    
    data = resp.json()
    
    # 显示 LLM 原始识别结果
    raw_llm = data.get("raw_llm", {})
    print(f"\n✓ Qwen-VL 识别成功")
    print(f"  是否食物: {raw_llm.get('is_food', 'N/A')}")
    
    foods = raw_llm.get("foods", [])
    print(f"  识别到 {len(foods)} 个菜品:")
    for i, food in enumerate(foods, 1):
        dish_name = food.get("dish_name", "未知")
        cooking = food.get("cooking_method", "未知")
        ingredients = food.get("ingredients", [])
        print(f"\n  [{i}] {dish_name} (烹饪: {cooking})")
        for ing in ingredients:
            name = ing.get("name_en", "?")
            weight = ing.get("weight_g", 0)
            conf = ing.get("confidence", 0)
            print(f"      - {name}: {weight}g (置信度: {conf:.0%})")
    
    # 显示营养计算结果
    snapshot = data.get("snapshot", {})
    nutrition = snapshot.get("nutrition", {})
    print(f"\n  📊 营养计算结果:")
    print(f"      热量: {nutrition.get('calories', 0):.1f} kcal")
    print(f"      蛋白质: {nutrition.get('protein', 0):.1f} g")
    print(f"      碳水: {nutrition.get('carbs', 0):.1f} g")
    print(f"      脂肪: {nutrition.get('fat', 0):.1f} g")
    
    return data


def test_meal_start(snapshot_data: dict) -> str:
    """测试开始用餐 - 使用真实识别数据"""
    print_header("2. 开始用餐会话")
    
    snapshot = snapshot_data.get("snapshot", {})
    foods = snapshot.get("foods", [])
    nutrition = snapshot.get("nutrition", {})
    
    payload = {
        "foods": foods,
        "nutrition": nutrition
    }
    
    print(f"输入食物: {[f.get('name', '?') for f in foods]}")
    print(f"输入热量: {nutrition.get('calories', 0)} kcal")
    
    resp = requests.post(
        f"{BASE_URL}/api/v1/meal/start",
        params={"user_id": "real_test_user", "meal_type": "lunch"},
        json=payload
    )
    
    print(f"状态码: {resp.status_code}")
    
    if resp.status_code != 200:
        print(f"❌ 错误: {resp.text}")
        return None
    
    data = resp.json()
    session_id = data.get("session_id")
    
    print(f"\n✓ 会话已创建")
    print(f"  Session ID: {session_id}")
    print(f"  状态: {data.get('status')}")
    print(f"  初始热量: {data.get('initial_kcal')} kcal")
    
    return session_id


def test_meal_update(session_id: str, original_foods: list, consumption_ratio: float = 0.5) -> dict:
    """测试更新用餐 - 模拟吃掉一部分"""
    print_header(f"3. 更新进度 (模拟消耗 {consumption_ratio:.0%})")
    
    # 模拟吃掉一部分：减少食物重量
    remaining_foods = []
    for f in original_foods:
        remaining = f.copy()
        remaining["weight_g"] = round(f.get("weight_g", 0) * (1 - consumption_ratio), 1)
        remaining_foods.append(remaining)
    
    payload = {
        "foods": remaining_foods
    }
    
    print(f"剩余食物重量: {[(f['name'], f['weight_g']) for f in remaining_foods]}")
    
    resp = requests.post(
        f"{BASE_URL}/api/v1/meal/update",
        params={"session_id": session_id},
        json=payload
    )
    
    print(f"状态码: {resp.status_code}")
    
    if resp.status_code != 200:
        print(f"❌ 错误: {resp.text}")
        return None
    
    data = resp.json()
    
    print(f"\n✓ 进度已更新")
    print(f"  当前剩余: {data.get('current_remaining_kcal', 0):.1f} kcal")
    print(f"  总上菜: {data.get('total_served_kcal', 0):.1f} kcal")
    print(f"  已消耗: {data.get('consumed_kcal', 0):.1f} kcal")
    print(f"  消耗比例: {data.get('consumption_ratio', 0)*100:.1f}%")
    print(f"  建议: {data.get('suggestion', 'N/A')}")
    
    if data.get('consumed_protein'):
        print(f"\n  📊 已消耗营养:")
        print(f"      蛋白质: {data.get('consumed_protein', 0):.1f} g")
        print(f"      碳水: {data.get('consumed_carbs', 0):.1f} g")
        print(f"      脂肪: {data.get('consumed_fat', 0):.1f} g")
    
    return data


def test_meal_end(session_id: str, original_foods: list, remaining_ratio: float = 0.1) -> dict:
    """测试结束用餐 - 模拟剩余少量"""
    print_header(f"4. 结束用餐 (剩余 {remaining_ratio:.0%})")
    
    # 模拟剩余少量
    remaining_foods = []
    for f in original_foods:
        remaining = f.copy()
        remaining["weight_g"] = round(f.get("weight_g", 0) * remaining_ratio, 1)
        remaining_foods.append(remaining)
    
    payload = {
        "foods": remaining_foods
    }
    
    print(f"最终剩余: {[(f['name'], f['weight_g']) for f in remaining_foods]}")
    
    resp = requests.post(
        f"{BASE_URL}/api/v1/meal/end",
        params={"session_id": session_id},
        json=payload
    )
    
    print(f"状态码: {resp.status_code}")
    
    if resp.status_code != 200:
        print(f"❌ 错误: {resp.text}")
        return None
    
    data = resp.json()
    
    print(f"\n✓ 用餐已结束")
    print(f"  状态: {data.get('status')}")
    print(f"  总消耗: {data.get('total_consumed_kcal', 0):.1f} kcal")
    print(f"  消耗比例: {data.get('consumption_ratio', 0)*100:.1f}%")
    print(f"  用餐时长: {data.get('duration_minutes', 0)} 分钟")
    
    print(f"\n  📊 营养成分总量:")
    print(f"      蛋白质: {data.get('total_protein', 0):.1f} g")
    print(f"      碳水: {data.get('total_carbs', 0):.1f} g")
    print(f"      脂肪: {data.get('total_fat', 0):.1f} g")
    
    breakdown = data.get('nutrition_breakdown', {})
    if breakdown:
        print(f"\n  🥧 营养饼图:")
        print(f"      蛋白质: {breakdown.get('protein_percent', 0):.1f}%")
        print(f"      碳水: {breakdown.get('carbs_percent', 0):.1f}%")
        print(f"      脂肪: {breakdown.get('fat_percent', 0):.1f}%")
    
    report = data.get('report')
    if report:
        print(f"\n  📝 用餐报告:")
        print(f"      {report}")
    
    return data


def test_chat_nutrition(meal_data: dict, foods_names: list) -> dict:
    """测试 AI 营养对话 - 使用真实用餐数据"""
    print_header("5. AI 营养建议 (Qwen-Plus)")
    
    # 构造真实用餐上下文
    payload = {
        "query": "请分析我这顿饭的营养情况，给出改进建议",
        "user_profile": {
            "age": 35,
            "gender": "male",
            "bmi": 24.5,
            "health_conditions": ["轻度高血压"],
            "dietary_preferences": ["低盐"],
            "activity_level": "moderate"
        },
        "meal_context": {
            "foods": foods_names,
            "total_calories": meal_data.get("total_consumed_kcal", 0),
            "total_protein": meal_data.get("total_protein", 0),
            "total_carbs": meal_data.get("total_carbs", 0),
            "total_fat": meal_data.get("total_fat", 0)
        },
        "message_type": "meal_end"
    }
    
    print(f"用户问题: {payload['query']}")
    print(f"用餐食物: {foods_names}")
    print(f"健康状况: {payload['user_profile']['health_conditions']}")
    print("正在调用 Qwen-Plus API...")
    
    start_time = time.time()
    resp = requests.post(
        f"{BASE_URL}/api/v1/chat/nutrition",
        json=payload
    )
    elapsed = time.time() - start_time
    
    print(f"响应时间: {elapsed:.2f}s")
    print(f"状态码: {resp.status_code}")
    
    if resp.status_code != 200:
        print(f"❌ 错误: {resp.text}")
        return None
    
    data = resp.json()
    
    print(f"\n✓ AI 回复:")
    print(f"  {data.get('answer', 'N/A')}")
    
    actions = data.get('suggested_actions', [])
    if actions:
        print(f"\n  💡 建议动作: {', '.join(actions)}")
    
    advice = data.get('meal_advice')
    if advice:
        print(f"  🍽️ 用餐建议: {advice}")
    
    return data


def test_daily_stats() -> dict:
    """测试今日统计"""
    print_header("6. 今日统计")
    
    today = datetime.now().strftime("%Y-%m-%d")
    resp = requests.get(
        f"{BASE_URL}/api/v1/stats/daily",
        params={"user_id": "real_test_user", "date": today}
    )
    
    print(f"状态码: {resp.status_code}")
    
    if resp.status_code != 200:
        print(f"❌ 错误: {resp.text}")
        return None
    
    data = resp.json()
    
    print(f"\n✓ 今日统计 ({today}):")
    print(f"  总热量: {data.get('total_calories', 0):.1f} kcal")
    print(f"  目标热量: {data.get('target_calories', 0)} kcal")
    print(f"  完成度: {data.get('calories_ratio', 0):.1f}%")
    print(f"  用餐次数: {data.get('meals_count', 0)}")
    
    breakdown = data.get('nutrition_breakdown', {})
    if breakdown:
        print(f"\n  🥧 营养比例:")
        print(f"      蛋白质: {breakdown.get('protein_percent', 0):.1f}%")
        print(f"      碳水: {breakdown.get('carbs_percent', 0):.1f}%")
        print(f"      脂肪: {breakdown.get('fat_percent', 0):.1f}%")
    
    return data


def main():
    print(f"\n{'#'*60}")
    print(f"#  Rokid 营养助手 - 真实数据流测试")
    print(f"#  Base URL: {BASE_URL}")
    print(f"#  图片: {IMAGE_PATH}")
    print(f"#  时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"{'#'*60}")
    
    # 检查图片是否存在
    if not os.path.exists(IMAGE_PATH):
        print(f"\n❌ 图片不存在: {IMAGE_PATH}")
        return
    
    print(f"\n图片大小: {os.path.getsize(IMAGE_PATH) / 1024:.1f} KB")
    
    try:
        # 1. 加载图片并调用视觉分析
        print("\n正在加载图片并转换为 base64...")
        image_url = load_image_as_base64(IMAGE_PATH)
        print(f"Base64 长度: {len(image_url)} 字符")
        
        # 1. 视觉分析
        vision_result = test_vision_analyze(image_url)
        if not vision_result:
            print("\n❌ 视觉分析失败，终止测试")
            return
        
        snapshot = vision_result.get("snapshot", {})
        foods = snapshot.get("foods", [])
        raw_llm = vision_result.get("raw_llm", {})
        
        # 获取菜品名称（用于 AI 对话）
        foods_names = [f.get("dish_name", f.get("name", "未知")) for f in raw_llm.get("foods", [])]
        if not foods_names:
            foods_names = [f.get("name", "未知") for f in foods]
        
        time.sleep(1)
        
        # 2. 开始用餐
        session_id = test_meal_start(vision_result)
        if not session_id:
            print("\n❌ 开始用餐失败，终止测试")
            return
        
        time.sleep(1)
        
        # 3. 更新进度（模拟吃了 50%）
        test_meal_update(session_id, foods, consumption_ratio=0.5)
        
        time.sleep(1)
        
        # 4. 结束用餐（模拟剩余 10%）
        meal_result = test_meal_end(session_id, foods, remaining_ratio=0.1)
        if not meal_result:
            print("\n❌ 结束用餐失败，终止测试")
            return
        
        time.sleep(1)
        
        # 5. AI 营养建议
        test_chat_nutrition(meal_result, foods_names)
        
        # 6. 今日统计
        test_daily_stats()
        
        print_header("✅ 所有测试完成!")
        print(f"\n使用的真实图片: {IMAGE_PATH}")
        print(f"识别到的食物: {foods_names}")
        print(f"Session ID: {session_id}")
        print()
        
    except requests.exceptions.ConnectionError:
        print(f"\n❌ 连接失败: 无法连接到 {BASE_URL}")
        print("请确保后端服务已启动: uvicorn app.main:app --reload")
    except Exception as e:
        print(f"\n❌ 测试失败: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    main()
