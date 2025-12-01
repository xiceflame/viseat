#!/usr/bin/env python3
"""测试 VLM 直接返回营养数据的一致性"""
import os, json, base64, re
from dotenv import load_dotenv
load_dotenv('/opt/RokidAI/backend/.env')
from openai import OpenAI

client = OpenAI(
    api_key=os.getenv('DASHSCOPE_API_KEY'),
    base_url='https://dashscope.aliyuncs.com/compatible-mode/v1',
)

with open('/opt/RokidAI/backend/uploads/a681b216-e734-427e-aff5-80aefec2c603.jpg', 'rb') as f:
    img_b64 = base64.b64encode(f.read()).decode()

NEW_PROMPT = """你是专业营养师。识别图中食物并直接估算营养成分。

返回 JSON 格式：
{
  "foods": [
    {
      "name_cn": "中文名",
      "name_en": "english name", 
      "weight_g": 100,
      "calories_kcal": 150,
      "protein_g": 10,
      "carbs_g": 15,
      "fat_g": 5,
      "confidence": 0.8
    }
  ],
  "total": {
    "calories_kcal": 500,
    "protein_g": 30,
    "carbs_g": 50,
    "fat_g": 20
  },
  "suggestion": "简短建议"
}

要求：
1. 只输出 JSON，无其他文字
2. 营养数据基于你的营养学知识直接估算
3. 每100g的营养参考值要准确（如鸡胸肉约165kcal/100g，米饭约130kcal/100g）
4. weight_g 根据图片估算实际份量
5. 火锅类只计算固体食材，不含汤水"""

print("=== 测试 VLM 直接返回营养数据的一致性 ===\n")

results = []
for i in range(3):
    resp = client.chat.completions.create(
        model='qwen-vl-plus',
        messages=[{'role': 'user', 'content': [
            {'type': 'image_url', 'image_url': {'url': f'data:image/jpeg;base64,{img_b64}'}},
            {'type': 'text', 'text': NEW_PROMPT},
        ]}],
        temperature=0,
    )
    
    text = resp.choices[0].message.content
    if '```' in text:
        m = re.search(r'```(?:json)?\s*([\s\S]*?)\s*```', text)
        if m: text = m.group(1)
    
    try:
        data = json.loads(text)
        total = data.get('total', {})
        cal = total.get('calories_kcal', 0)
        protein = total.get('protein_g', 0)
        foods = [f.get('name_cn', '?') for f in data.get('foods', [])]
        results.append(cal)
        print(f"第 {i+1} 次: {cal}kcal | 蛋白质:{protein}g | 食物:{foods}")
    except:
        print(f"第 {i+1} 次: 解析失败")

if results:
    avg = sum(results) / len(results)
    variance = max(results) - min(results)
    print(f"\n平均: {avg:.0f}kcal | 波动范围: {variance:.0f}kcal ({variance/avg*100:.1f}%)")
