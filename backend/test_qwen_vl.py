#!/usr/bin/env python3
"""测试 Qwen-VL 模型调用"""
import os
from dotenv import load_dotenv
load_dotenv('/opt/RokidAI/backend/.env')

from openai import OpenAI

api_key = os.getenv('DASHSCOPE_API_KEY')
print(f"API Key: {api_key[:10]}...{api_key[-5:]}" if api_key else "API Key 未设置!")

client = OpenAI(
    api_key=api_key,
    base_url='https://dashscope.aliyuncs.com/compatible-mode/v1',
)

test_image = "https://dashscope.oss-cn-beijing.aliyuncs.com/images/dog_and_girl.jpeg"

# 测试 qwen-vl-plus
print("\n=== 测试 qwen-vl-plus ===")
try:
    response = client.chat.completions.create(
        model="qwen-vl-plus",
        messages=[{
            "role": "user",
            "content": [
                {"type": "text", "text": "简单描述图片"},
                {"type": "image_url", "image_url": {"url": test_image}}
            ]
        }],
        max_tokens=50,
    )
    print(f"✅ 成功: {response.choices[0].message.content[:50]}")
except Exception as e:
    print(f"❌ 失败: {e}")

# 测试 qwen-vl-max
print("\n=== 测试 qwen-vl-max ===")
try:
    response = client.chat.completions.create(
        model="qwen-vl-max",
        messages=[{
            "role": "user",
            "content": [
                {"type": "text", "text": "简单描述图片"},
                {"type": "image_url", "image_url": {"url": test_image}}
            ]
        }],
        max_tokens=50,
    )
    print(f"✅ 成功: {response.choices[0].message.content[:50]}")
except Exception as e:
    print(f"❌ 失败: {e}")
