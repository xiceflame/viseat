# food_eval.py - 食品识别模型完整评测工具
import csv
import json
import time
import base64
from pathlib import Path
from dataclasses import dataclass

# ========== 统一提示词（来自后端）==========
SYSTEM_PROMPT = """你是一名专业的营养分析助手。请仔细观察图片，精确识别图中的食物，返回JSON：
{"is_food":true,"suggestion":"建议","foods":[{"dish_name":"食材名","dish_name_cn":"食材名","category":"meal","cooking_method":"raw","ingredients":[{"name_en":"食材名","name_cn":"食材名","weight_g":100,"confidence":0.9}],"total_weight_g":100,"confidence":0.9}]}

要求：
1. 只输出JSON
2. 每种食物或菜品单独一个foods元素，不要合并！如：饼干和糖果要分成2个foods
3. dish_name和ingredients.name_en/name_cn使用相同的中文标准名：
   主食: 米饭、面条、馒头、饺子、包子、炒饭、粥、面包
   肉类: 猪肉、牛肉、鸡肉、羊肉、鸭肉、排骨、五花肉、鸡腿、鸡翅、香肠、肉丸、火腿、培根
   海鲜: 鱼、虾、蟹、蛤蜊、鱿鱼、生蚝、扇贝、鱼丸、龙虾
   蔬菜: 白菜、菠菜、生菜、胡萝卜、土豆、番茄、黄瓜、西兰花、香菇、玉米、豆芽、茄子、芹菜、洋葱、辣椒、南瓜、莲藕、海带、金针菇、木耳
   豆制品: 豆腐、豆腐干、豆腐皮、豆浆、腐竹
   蛋奶: 鸡蛋、牛奶、奶酪、酸奶、黄油
   水果: 苹果、香蕉、橙子、葡萄、西瓜、草莓、芒果、桃子、梨
   零食: 饼干、薯片、巧克力、糖果、蛋糕、冰淇淋、面包、坚果
   快餐: 炒饭、炒面、汉堡、炸鸡、薯条、披萨、寿司、拉面、火锅、麻辣烫
4. category: meal/snack/beverage/dessert/fruit
5. cooking_method: raw/steam/boil/braise/stir-fry/deep-fry/bake/grill
6. weight_g估算可食用部分重量(克)"""

USER_PROMPT = "请根据图片识别所有可见的主要食材，并严格按照上述 JSON 结构输出。"

# ========== 评测结果 ==========
@dataclass
class TestResult:
    id: str
    name_correct: bool = False
    food_count_correct: bool = False
    weight_error: float = 1.0
    json_valid: bool = False
    response_time: float = 0.0
    pred_name: str = ""
    pred_weight: float = 0
    # 食材拆分指标（复合型食物）
    ingredient_recall: float = 0.0      # 召回率：识别出的真实食材比例
    ingredient_precision: float = 0.0   # 精确率：识别正确的食材比例
    ingredient_f1: float = 0.0          # F1分数
    pred_ingredients: list = None       # 预测的食材列表
    error: str = ""

# ========== 模型调用 ==========
def call_model(model_name: str, image_path: str) -> dict:
    """调用视觉模型，返回解析后的JSON"""
    with open(image_path, "rb") as f:
        img_b64 = base64.b64encode(f.read()).decode()

    # === 阿里 Qwen2.5-VL / Qwen3-VL ===
    if model_name in ["qwen2.5-vl", "qwen3-vl"]:
        import dashscope
        from dashscope import MultiModalConversation
        model_id = "qwen2.5-vl-32b-instruct" if model_name == "qwen2.5-vl" else "qwen-vl-max"
        resp = MultiModalConversation.call(
            model=model_id,
            messages=[
                {"role": "system", "content": [{"text": SYSTEM_PROMPT}]},
                {"role": "user", "content": [
                    {"image": f"data:image/jpeg;base64,{img_b64}"},
                    {"text": USER_PROMPT}
                ]}
            ]
        )
        return json.loads(resp.output.choices[0].message.content[0]["text"])

    # === OpenAI GPT-4.5 ===
    elif model_name == "gpt-4.5":
        from openai import OpenAI
        client = OpenAI()
        resp = client.chat.completions.create(
            model="gpt-4.5-preview",
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": [
                    {"type": "image_url", "image_url": {"url": f"data:image/jpeg;base64,{img_b64}"}},
                    {"type": "text", "text": USER_PROMPT}
                ]}
            ]
        )
        return json.loads(resp.choices[0].message.content)

    # === Google Gemini 2.5 Pro ===
    elif model_name == "gemini-2.5":
        import google.generativeai as genai
        model = genai.GenerativeModel("gemini-2.5-pro")
        import PIL.Image
        img = PIL.Image.open(image_path)
        resp = model.generate_content([SYSTEM_PROMPT + "\n" + USER_PROMPT, img])
        return json.loads(resp.text)

    # === Anthropic Claude Sonnet 4.5 ===
    elif model_name == "claude-4.5":
        import anthropic
        client = anthropic.Anthropic()
        resp = client.messages.create(
            model="claude-sonnet-4-5-20250514",
            max_tokens=1024,
            system=SYSTEM_PROMPT,
            messages=[{
                "role": "user",
                "content": [
                    {"type": "image", "source": {"type": "base64", "media_type": "image/jpeg", "data": img_b64}},
                    {"type": "text", "text": USER_PROMPT}
                ]
            }]
        )
        return json.loads(resp.content[0].text)

    # === 智谱 GLM-4V-Plus ===
    elif model_name == "glm-4v":
        from zhipuai import ZhipuAI
        client = ZhipuAI()
        resp = client.chat.completions.create(
            model="glm-4v-plus",
            messages=[{
                "role": "user",
                "content": [
                    {"type": "text", "text": SYSTEM_PROMPT + "\n" + USER_PROMPT},
                    {"type": "image_url", "image_url": {"url": f"data:image/jpeg;base64,{img_b64}"}}
                ]
            }]
        )
        return json.loads(resp.choices[0].message.content)

    # === 字节 Doubao-vision-pro ===
    elif model_name == "doubao":
        from volcenginesdkarkruntime import Ark
        client = Ark()
        resp = client.chat.completions.create(
            model="doubao-vision-pro-32k",
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": [
                    {"type": "image_url", "image_url": {"url": f"data:image/jpeg;base64,{img_b64}"}},
                    {"type": "text", "text": USER_PROMPT}
                ]}
            ]
        )
        return json.loads(resp.choices[0].message.content)

    raise ValueError(f"未知模型: {model_name}，支持: qwen2.5-vl, qwen3-vl, gpt-4.5, gemini-2.5, claude-4.5, glm-4v, doubao")

# ========== 评测核心 ==========
def evaluate(test_csv: str, model_name: str) -> tuple:
    results = []
    with open(test_csv, encoding="utf-8") as f:
        tests = list(csv.DictReader(f))

    for item in tests:
        r = TestResult(id=item["id"])
        start = time.time()

        try:
            pred = call_model(model_name, item["image"])
            r.response_time = time.time() - start
            r.json_valid = True

            food = pred.get("foods", [{}])[0] if pred.get("foods") else {}
            r.pred_name = food.get("dish_name_cn", "")
            r.pred_weight = food.get("total_weight_g", 0)

            r.name_correct = r.pred_name == item["food_name"]
            expected_count = int(item.get("food_count", 1))
            r.food_count_correct = len(pred.get("foods", [])) == expected_count

            actual = float(item["actual_weight"])
            if actual > 0:
                r.weight_error = abs(r.pred_weight - actual) / actual

            # 食材拆分评测（复合型食物）
            if item.get("ingredients"):
                # 支持 "Name:Weight" 格式，仅提取 Name 进行匹配
                actual_ingredients = set()
                for s in item["ingredients"].split("|"):
                    name = s.split(":")[0].strip()
                    if name:
                        actual_ingredients.add(name)
                
                pred_ingredients = set()
                for f in pred.get("foods", []):
                    for ing in f.get("ingredients", []):
                        pred_ingredients.add(ing.get("name_cn", ""))
                r.pred_ingredients = list(pred_ingredients)

                # 计算召回率、精确率、F1
                if actual_ingredients:
                    matched = actual_ingredients & pred_ingredients
                    r.ingredient_recall = len(matched) / len(actual_ingredients) if actual_ingredients else 0
                    r.ingredient_precision = len(matched) / len(pred_ingredients) if pred_ingredients else 0
                    if r.ingredient_recall + r.ingredient_precision > 0:
                        r.ingredient_f1 = 2 * r.ingredient_recall * r.ingredient_precision / (r.ingredient_recall + r.ingredient_precision)

            print(f"[{item['id']}] {item['food_name']}: 名称={'✓' if r.name_correct else '✗'}, "
                  f"重量误差={r.weight_error:.1%}, 食材F1={r.ingredient_f1:.1%}, 耗时={r.response_time:.1f}s")

        except json.JSONDecodeError:
            r.error = "JSON解析失败"
            r.response_time = time.time() - start
            print(f"[{item['id']}] JSON解析失败")
        except Exception as e:
            r.error = str(e)
            print(f"[{item['id']}] 错误: {e}")

        results.append(r)

    # 汇总指标
    valid = [r for r in results if not r.error]
    n = len(valid)
    total = len(results)

    # 食材拆分指标（仅统计有食材标注的样本）
    with_ingredients = [r for r in valid if r.pred_ingredients is not None]
    ing_n = len(with_ingredients)

    metrics = {
        "模型": model_name,
        "测试样本数": total,
        # 基础识别
        "名称准确率": f"{sum(r.name_correct for r in valid) / n:.1%}" if n else "N/A",
        "食物数量准确率": f"{sum(r.food_count_correct for r in valid) / n:.1%}" if n else "N/A",
        # 重量估算
        "重量平均误差": f"{sum(r.weight_error for r in valid) / n:.1%}" if n else "N/A",
        "重量命中率(<20%)": f"{sum(1 for r in valid if r.weight_error < 0.2) / n:.1%}" if n else "N/A",
        "重量最大偏差": f"{max(r.weight_error for r in valid):.1%}" if n else "N/A",
        # 食材拆分（复合型食物核心指标）
        "食材召回率": f"{sum(r.ingredient_recall for r in with_ingredients) / ing_n:.1%}" if ing_n else "N/A",
        "食材精确率": f"{sum(r.ingredient_precision for r in with_ingredients) / ing_n:.1%}" if ing_n else "N/A",
        "食材F1分数": f"{sum(r.ingredient_f1 for r in with_ingredients) / ing_n:.1%}" if ing_n else "N/A",
        # 实用性
        "平均响应时间": f"{sum(r.response_time for r in valid) / n:.1f}s" if n else "N/A",
        "响应<5秒占比": f"{sum(1 for r in valid if r.response_time < 5) / n:.1%}" if n else "N/A",
        "JSON格式正确率": f"{sum(r.json_valid for r in results) / total:.1%}",
        "成功率": f"{n}/{total}",
    }

    return metrics, results

# ========== 导出结果 ==========
def export_results(metrics: dict, results: list, output_file: str):
    with open(output_file, "w", encoding="utf-8", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["=== 评测汇总 ==="])
        for k, v in metrics.items():
            writer.writerow([k, v])
        writer.writerow([])
        writer.writerow(["=== 详细结果 ==="])
        writer.writerow(["编号", "名称正确", "重量误差", "响应时间", "预测名称", "预测重量", "错误"])
        for r in results:
            writer.writerow([r.id, r.name_correct, f"{r.weight_error:.1%}",
                           f"{r.response_time:.1f}s", r.pred_name, r.pred_weight, r.error])

# ========== 主程序 ==========
if __name__ == "__main__":
    import sys
    model = sys.argv[1] if len(sys.argv) > 1 else "qwen-vl"

    metrics, results = evaluate("test_data.csv", model)

    print("\n" + "="*50)
    print(f"评测结果 - {model}")
    print("="*50)
    for k, v in metrics.items():
        print(f"{k}: {v}")

    export_results(metrics, results, f"result_{model}.csv")
    print(f"\n详细结果已导出到 result_{model}.csv")
