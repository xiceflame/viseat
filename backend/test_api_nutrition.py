#!/usr/bin/env python3
"""测试 API 营养计算"""
import requests
import base64
import json
import time

IMAGE_PATH = "/opt/RokidAI/backend/uploads/a681b216-e734-427e-aff5-80aefec2c603.jpg"

with open(IMAGE_PATH, "rb") as f:
    img_b64 = base64.b64encode(f.read()).decode()

t0 = time.time()
resp = requests.post(
    "http://localhost:8000/api/v1/vision/analyze",
    json={"image_base64": img_b64},
    timeout=60,
)
elapsed = time.time() - t0

if resp.status_code != 200:
    print(f"错误: {resp.status_code}")
    print(resp.text[:500])
else:
    data = resp.json()
    print(f"=== 响应时间: {elapsed:.2f}s ===\n")
    
    print("=== VLM 识别结果 ===")
    for f in data.get("raw_llm", {}).get("foods", [])[:8]:
        # 新格式：直接返回中文 name
        name = f.get("name") or f.get("name_cn") or "?"
        weight = f.get("weight_g") or f.get("total_weight_g", 0)
        print(f"  {name}: {weight}g")
    
    # VLM 返回的 total
    vlm_total = data.get("raw_llm", {}).get("total", {})
    if vlm_total:
        print(f"\nVLM 总计: {vlm_total.get('calories_kcal', 0)}kcal")
    
    print("\n=== 最终营养（数据库校验后）===")
    n = data.get("snapshot", {}).get("nutrition", {})
    print(f"热量: {n.get('calories', 0):.0f} kcal")
    print(f"蛋白质: {n.get('protein', 0):.1f} g")
    print(f"碳水: {n.get('carbs', 0):.1f} g")
    print(f"脂肪: {n.get('fat', 0):.1f} g")
    
    print("\n=== 食物详情 ===")
    for f in data.get("snapshot", {}).get("foods", [])[:8]:
        name = f.get('name') or f.get('name_en', '?')
        cal = f.get('calories_kcal', '?')
        print(f"  {name}: {f.get('weight_g')}g | {cal}kcal")
