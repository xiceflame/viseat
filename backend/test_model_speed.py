#!/usr/bin/env python3
"""测试不同 VLM 模型速度"""
import os
import sys
import time
import base64
import json
import re

sys.path.insert(0, '/opt/RokidAI/backend')
from dotenv import load_dotenv
load_dotenv('/opt/RokidAI/backend/.env')

from openai import OpenAI

client = OpenAI(
    api_key=os.getenv('DASHSCOPE_API_KEY'),
    base_url='https://dashscope.aliyuncs.com/compatible-mode/v1',
)

# 使用火锅图片测试
with open('/opt/RokidAI/backend/uploads/a681b216-e734-427e-aff5-80aefec2c603.jpg', 'rb') as f:
    img_b64 = base64.b64encode(f.read()).decode()
data_url = f'data:image/jpeg;base64,{img_b64}'

PROMPT = '识别图中食物，返回JSON：{"foods": [{"dish_name_cn": "菜名", "total_weight_g": 100}]}'

models = ['qwen-vl-plus', 'qwen-vl-max']
print('=== 完整食物识别任务速度对比 ===')

for model in models:
    t0 = time.time()
    try:
        resp = client.chat.completions.create(
            model=model,
            messages=[{'role': 'user', 'content': [
                {'type': 'image_url', 'image_url': {'url': data_url}},
                {'type': 'text', 'text': PROMPT},
            ]}],
            temperature=0,
        )
        elapsed = time.time() - t0
        text = resp.choices[0].message.content
        # 提取食物名
        try:
            if '```' in text:
                m = re.search(r'```(?:json)?\s*([\s\S]*?)\s*```', text)
                if m:
                    text = m.group(1)
            data = json.loads(text)
            foods = [f.get('dish_name_cn', '?') for f in data.get('foods', [])]
        except:
            foods = ['解析失败']
        print(f'{model:20s}: {elapsed:5.2f}s | {foods}')
    except Exception as e:
        print(f'{model:20s}: 错误 - {str(e)[:60]}')
