#!/usr/bin/env python3
"""
使用 LLM 多线程批量生成食材映射表

功能：
1. 从 nutrition_db 提取所有 6000+ 食材
2. 多线程并行调用 LLM 生成映射
3. 实时保存 JSON 和 Python 代码

运行方式：
    python scripts/generate_food_mappings.py --workers 5 --batch-size 30
"""

import os
import sys
import json
import time
import argparse
import logging
import threading
from pathlib import Path
from typing import Dict, List, Any
from dataclasses import dataclass, asdict
from concurrent.futures import ThreadPoolExecutor, as_completed
from queue import Queue

# 添加项目路径
sys.path.insert(0, str(Path(__file__).parent.parent))

from dotenv import load_dotenv
load_dotenv(Path(__file__).parent.parent / '.env')

from openai import OpenAI

logging.basicConfig(
    level=logging.INFO, 
    format='%(asctime)s [%(threadName)s] %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# 线程安全锁
_lock = threading.Lock()
_save_lock = threading.Lock()


@dataclass
class FoodMapping:
    """食材映射条目"""
    db_key: str           # 数据库原始键名
    name_cn: str          # 标准中文名
    name_en: str          # 标准英文名
    aliases_cn: List[str] # 中文别名
    aliases_en: List[str] # 英文别名
    category: str         # 分类
    calories: float       # 热量 per 100g
    protein: float        # 蛋白质
    carbs: float          # 碳水
    fat: float            # 脂肪


def get_qwen_client():
    """获取 Qwen 客户端"""
    api_key = os.getenv('DASHSCOPE_API_KEY')
    if not api_key:
        raise RuntimeError("未设置 DASHSCOPE_API_KEY")
    return OpenAI(
        api_key=api_key,
        base_url='https://dashscope.aliyuncs.com/compatible-mode/v1',
    )


def extract_all_foods() -> List[Dict[str, Any]]:
    """从 nutrition_db 提取所有食材"""
    from app.nutrition_db import get_nutrition_db
    
    db = get_nutrition_db()
    foods = []
    
    for key, nutrition in db._db.items():
        if not key or len(key) < 2:
            continue
        
        # 跳过无效数据
        if nutrition.energy_kcal <= 0 and nutrition.protein <= 0:
            continue
        
        foods.append({
            "db_key": key,
            "calories": round(nutrition.energy_kcal, 1),
            "protein": round(nutrition.protein, 1),
            "carbs": round(nutrition.carbohydrate, 1),
            "fat": round(nutrition.fat, 1),
            "source": nutrition.source.value,
        })
    
    logger.info(f"提取了 {len(foods)} 种食材")
    return foods


def generate_mappings_batch(client: OpenAI, foods: List[Dict], model: str = "qwen-plus") -> List[Dict]:
    """用 LLM 批量生成映射"""
    
    # 构建 prompt
    food_list = "\n".join([
        f"{i+1}. {f['db_key']} ({f['calories']}kcal, {f['protein']}g蛋白)"
        for i, f in enumerate(foods)
    ])
    
    prompt = f"""你是食品营养专家。请为以下食材生成标准化映射。

食材列表：
{food_list}

请为每个食材返回 JSON 数组，每个元素包含：
- db_key: 原始名称（保持不变）
- name_cn: 简化中文名（去掉括号内容，如"猪肉（代表值）"→"猪肉"）
- name_en: 标准英文名（VLM 常用的，如 pork, beef, rice）
- aliases_cn: 中文别名数组（常见叫法，如["五花肉", "猪五花"]）
- aliases_en: 英文别名数组（如["pork belly", "streaky pork"]）
- category: 分类（meat/seafood/vegetable/fruit/grain/dairy/snack/beverage/other）

要求：
1. 只输出 JSON 数组，无其他文字
2. name_en 用小写，常见简单词汇
3. 别名要实用，是用户或 VLM 可能使用的名称
4. 如果是英文食材，name_cn 可以是音译或意译

示例输出：
[
  {{"db_key": "猪肉（代表值）", "name_cn": "猪肉", "name_en": "pork", "aliases_cn": ["猪肉"], "aliases_en": ["pork meat"], "category": "meat"}},
  {{"db_key": "Chicken breast", "name_cn": "鸡胸肉", "name_en": "chicken breast", "aliases_cn": ["鸡胸", "鸡脯肉"], "aliases_en": ["chicken breast meat"], "category": "meat"}}
]"""

    try:
        response = client.chat.completions.create(
            model=model,
            messages=[{"role": "user", "content": prompt}],
            temperature=0,
            max_tokens=4000,
        )
        
        text = response.choices[0].message.content.strip()
        
        # 提取 JSON
        if '```' in text:
            import re
            match = re.search(r'```(?:json)?\s*([\s\S]*?)\s*```', text)
            if match:
                text = match.group(1)
        
        # 解析 JSON
        mappings = json.loads(text)
        return mappings
        
    except Exception as e:
        logger.error(f"LLM 调用失败: {e}")
        return []


def merge_mappings(existing: Dict, new_mappings: List[Dict]) -> Dict:
    """合并映射结果"""
    for m in new_mappings:
        db_key = m.get('db_key', '')
        if not db_key:
            continue
        
        # 添加到映射表
        name_en = m.get('name_en', '').lower().strip()
        name_cn = m.get('name_cn', '').strip()
        
        if name_en and name_en not in existing:
            existing[name_en] = {
                "db_key": db_key,
                "name_cn": name_cn,
                "category": m.get('category', 'other'),
            }
        
        if name_cn and name_cn not in existing:
            existing[name_cn] = {
                "db_key": db_key,
                "name_en": name_en,
                "category": m.get('category', 'other'),
            }
        
        # 添加别名
        for alias in m.get('aliases_en', []):
            alias = alias.lower().strip()
            if alias and alias not in existing:
                existing[alias] = {
                    "db_key": db_key,
                    "name_cn": name_cn,
                    "category": m.get('category', 'other'),
                }
        
        for alias in m.get('aliases_cn', []):
            alias = alias.strip()
            if alias and alias not in existing:
                existing[alias] = {
                    "db_key": db_key,
                    "name_en": name_en,
                    "category": m.get('category', 'other'),
                }
    
    return existing


def generate_food_mapper_code(mappings: Dict, foods_data: Dict[str, Dict]) -> str:
    """生成 food_mapper.py 的 FOOD_ALIASES 代码"""
    
    lines = ["# 自动生成的食材映射表", "# 生成时间: " + time.strftime("%Y-%m-%d %H:%M:%S"), ""]
    lines.append("FOOD_ALIASES_GENERATED = {")
    
    for alias, info in sorted(mappings.items()):
        db_key = info.get('db_key', '')
        if db_key in foods_data:
            food = foods_data[db_key]
            cal = food.get('calories', 100)
            pro = food.get('protein', 5)
            carb = food.get('carbs', 15)
            fat = food.get('fat', 3)
            
            # 转义引号
            alias_escaped = alias.replace('"', '\\"')
            db_key_escaped = db_key.replace('"', '\\"')
            
            lines.append(f'    "{alias_escaped}": ("{db_key_escaped}", {cal}, {pro}, {carb}, {fat}),')
    
    lines.append("}")
    return "\n".join(lines)


class MappingGenerator:
    """多线程映射生成器"""
    
    def __init__(self, output_path: Path, foods_data: Dict, model: str = "qwen-plus"):
        self.output_path = output_path
        self.foods_data = foods_data
        self.model = model
        self.mappings: Dict = {}
        self.processed_keys: set = set()
        self.total_batches = 0
        self.completed_batches = 0
        self.start_time = time.time()
    
    def load_progress(self):
        """加载已有进度"""
        if self.output_path.exists():
            with open(self.output_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
                self.mappings = data.get('mappings', {})
                self.processed_keys = set(data.get('processed_keys', []))
            logger.info(f"已加载 {len(self.mappings)} 条映射，{len(self.processed_keys)} 个已处理")
    
    def save_progress(self):
        """保存进度（线程安全）"""
        with _save_lock:
            self.output_path.parent.mkdir(parents=True, exist_ok=True)
            
            # 保存 JSON
            with open(self.output_path, 'w', encoding='utf-8') as f:
                json.dump({
                    'mappings': self.mappings,
                    'processed_keys': list(self.processed_keys),
                    'total_foods': len(self.foods_data),
                    'generated_at': time.strftime("%Y-%m-%d %H:%M:%S"),
                }, f, ensure_ascii=False, indent=2)
            
            # 同步生成 Python 代码
            code = generate_food_mapper_code(self.mappings, self.foods_data)
            code_path = self.output_path.with_suffix('.py')
            with open(code_path, 'w', encoding='utf-8') as f:
                f.write(code)
    
    def process_batch(self, batch_id: int, batch: List[Dict]) -> bool:
        """处理单个批次（在线程中运行）"""
        try:
            client = get_qwen_client()
            new_mappings = generate_mappings_batch(client, batch, self.model)
            
            if new_mappings:
                with _lock:
                    self.mappings = merge_mappings(self.mappings, new_mappings)
                    for f in batch:
                        self.processed_keys.add(f['db_key'])
                    self.completed_batches += 1
                    
                    # 计算进度
                    elapsed = time.time() - self.start_time
                    speed = self.completed_batches / elapsed * 60  # 批次/分钟
                    remaining = (self.total_batches - self.completed_batches) / max(speed, 0.1)
                    
                    logger.info(
                        f"批次 {batch_id}/{self.total_batches} 完成 | "
                        f"映射: {len(self.mappings)} | "
                        f"进度: {self.completed_batches}/{self.total_batches} | "
                        f"速度: {speed:.1f}批/分 | "
                        f"剩余: {remaining:.1f}分钟"
                    )
                
                # 每完成一批就保存
                self.save_progress()
                return True
            else:
                logger.warning(f"批次 {batch_id} 处理失败")
                return False
                
        except Exception as e:
            logger.error(f"批次 {batch_id} 异常: {e}")
            return False


def main():
    parser = argparse.ArgumentParser(description='多线程生成食材映射表')
    parser.add_argument('--batch-size', type=int, default=30, help='每批处理的食材数量')
    parser.add_argument('--workers', type=int, default=5, help='并行线程数')
    parser.add_argument('--max-foods', type=int, default=None, help='最大处理食材数（测试用）')
    parser.add_argument('--output', type=str, default='data/food_mappings_full.json', help='输出文件')
    parser.add_argument('--model', type=str, default='qwen-plus', help='LLM 模型')
    parser.add_argument('--resume', action='store_true', default=True, help='从上次中断处继续')
    args = parser.parse_args()
    
    logger.info(f"配置: workers={args.workers}, batch_size={args.batch_size}, model={args.model}")
    
    # 提取所有食材
    logger.info("正在提取食材...")
    all_foods = extract_all_foods()
    
    if args.max_foods:
        all_foods = all_foods[:args.max_foods]
    
    # 构建食材数据字典
    foods_data = {f['db_key']: f for f in all_foods}
    
    # 初始化生成器
    output_path = Path(__file__).parent.parent / args.output
    generator = MappingGenerator(output_path, foods_data, args.model)
    
    # 加载已有进度
    if args.resume:
        generator.load_progress()
    
    # 过滤未处理的食材
    pending_foods = [f for f in all_foods if f['db_key'] not in generator.processed_keys]
    logger.info(f"待处理: {len(pending_foods)} 种食材")
    
    if not pending_foods:
        logger.info("所有食材已处理完成！")
        return
    
    # 分批
    batches = []
    for i in range(0, len(pending_foods), args.batch_size):
        batches.append(pending_foods[i:i + args.batch_size])
    
    generator.total_batches = len(batches)
    logger.info(f"共 {len(batches)} 个批次，使用 {args.workers} 个线程并行处理")
    
    # 多线程处理
    with ThreadPoolExecutor(max_workers=args.workers, thread_name_prefix="Worker") as executor:
        futures = {
            executor.submit(generator.process_batch, i + 1, batch): i
            for i, batch in enumerate(batches)
        }
        
        for future in as_completed(futures):
            batch_id = futures[future] + 1
            try:
                future.result()
            except Exception as e:
                logger.error(f"批次 {batch_id} 执行异常: {e}")
    
    # 最终保存
    generator.save_progress()
    
    elapsed = time.time() - generator.start_time
    logger.info(f"完成！总耗时: {elapsed/60:.1f} 分钟")
    logger.info(f"  - 映射数: {len(generator.mappings)}")
    logger.info(f"  - JSON: {output_path}")
    logger.info(f"  - Python: {output_path.with_suffix('.py')}")


if __name__ == '__main__':
    main()
