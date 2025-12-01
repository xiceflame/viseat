#!/usr/bin/env python3
"""测试 VLM 识别一致性"""
import os
import sys
import json
import base64
import time

sys.path.insert(0, '/opt/RokidAI/backend')
from dotenv import load_dotenv
load_dotenv('/opt/RokidAI/backend/.env')

from openai import OpenAI

client = OpenAI(
    api_key=os.getenv("DASHSCOPE_API_KEY"),
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
)

# 读取本地图片
IMAGE_PATH = "/opt/RokidAI/backend/uploads/a681b216-e734-427e-aff5-80aefec2c603.jpg"
if not os.path.exists(IMAGE_PATH):
    # 使用任意存在的图片
    import glob
    images = glob.glob("/opt/RokidAI/backend/uploads/*.jpg")
    if images:
        IMAGE_PATH = images[0]
    else:
        print("没有找到测试图片")
        sys.exit(1)

print(f"测试图片: {IMAGE_PATH}")

# 转为 base64
with open(IMAGE_PATH, "rb") as f:
    img_base64 = base64.b64encode(f.read()).decode()

data_url = f"data:image/jpeg;base64,{img_base64}"

SYSTEM_PROMPT = """你是营养分析助手。识别图中食物并返回 JSON：
{
  "is_food": true,
  "foods": [
    {"dish_name_cn": "菜名", "total_weight_g": 100, "ingredients": [{"name_cn": "食材", "weight_g": 100}]}
  ]
}
只输出 JSON，不要解释。"""

print("\n=== 测试同一图片 5 次识别 ===")
results = []

for i in range(5):
    t0 = time.time()
    try:
        completion = client.chat.completions.create(
            model="qwen-vl-max",
            messages=[
                {"role": "system", "content": [{"type": "text", "text": SYSTEM_PROMPT}]},
                {"role": "user", "content": [
                    {"type": "image_url", "image_url": {"url": data_url}},
                    {"type": "text", "text": "识别食物"},
                ]},
            ],
            temperature=0,  # 设为0消除随机性
        )
        text = completion.choices[0].message.content
        # 解析 JSON
        try:
            if "```" in text:
                import re
                match = re.search(r'```(?:json)?\s*([\s\S]*?)\s*```', text)
                if match:
                    text = match.group(1)
            data = json.loads(text)
        except:
            data = {"error": "JSON 解析失败", "raw": text[:200]}
        
        elapsed = time.time() - t0
        results.append({"data": data, "time": elapsed})
        
        # 提取关键信息
        foods = data.get("foods", [])
        food_summary = []
        for f in foods[:5]:
            name = f.get("dish_name_cn", "?")
            weight = f.get("total_weight_g", 0)
            food_summary.append(f"{name}:{weight}g")
        
        print(f"\n第 {i+1} 次 ({elapsed:.1f}s): {', '.join(food_summary) or '无食物'}")
        
    except Exception as e:
        print(f"\n第 {i+1} 次: 错误 - {e}")
        results.append({"error": str(e)})

# 分析差异
print("\n" + "=" * 50)
print("=== 差异分析 ===")

all_foods = []
for r in results:
    if "data" in r:
        foods = r["data"].get("foods", [])
        food_set = set()
        for f in foods:
            name = f.get("dish_name_cn", "?")
            weight = f.get("total_weight_g", 0)
            food_set.add((name, weight))
        all_foods.append(food_set)

if len(all_foods) >= 2:
    # 检查一致性
    first = all_foods[0]
    consistent = all(f == first for f in all_foods)
    print(f"识别一致性: {'✅ 完全一致' if consistent else '❌ 存在差异'}")
    
    if not consistent:
        print("\n各次识别结果:")
        for i, foods in enumerate(all_foods):
            print(f"  第 {i+1} 次: {foods}")
