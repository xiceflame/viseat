#!/usr/bin/env python3
"""性能分析脚本 - 测试流式响应"""
import time
import os
import sys
sys.path.insert(0, '/opt/RokidAI/backend')

from dotenv import load_dotenv
load_dotenv('/opt/RokidAI/backend/.env')

from openai import OpenAI

client = OpenAI(
    api_key=os.getenv("DASHSCOPE_API_KEY"),
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
)

IMAGE_URL = "https://viseat.cn/uploads/c9d74aa1-69ae-4363-899b-8170d2f900fb.jpg"

print("=== 测试流式响应 ===")
t0 = time.time()
first_chunk_time = None
content = ""

stream = client.chat.completions.create(
    model="qwen-vl-max",
    messages=[
        {"role": "user", "content": [
            {"type": "image_url", "image_url": {"url": IMAGE_URL}},
            {"type": "text", "text": "简单描述图中食物"},
        ]},
    ],
    stream=True,
)

for chunk in stream:
    if first_chunk_time is None:
        first_chunk_time = time.time() - t0
        print(f"首个响应到达: {first_chunk_time:.2f}s")
    delta = chunk.choices[0].delta.content or ""
    content += delta
    print(delta, end="", flush=True)

total = time.time() - t0
print(f"\n\n总耗时: {total:.2f}s")
print(f"用户可提前 {total - first_chunk_time:.2f}s 看到反馈")
