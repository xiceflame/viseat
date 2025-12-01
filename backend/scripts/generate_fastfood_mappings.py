#!/usr/bin/env python3
"""
快餐/外卖食品热量映射生成器

策略：
1. 先定义 500 种常见快餐/外卖名称
2. 优先从数据库查询已有数据
3. 数据库没有的，用 LLM 估算营养值
4. 生成完整的映射表
"""

import os
import sys
import json
import time
import logging
from pathlib import Path
from typing import Dict, List, Tuple, Optional
from concurrent.futures import ThreadPoolExecutor, as_completed
import threading

sys.path.insert(0, str(Path(__file__).parent.parent))

from dotenv import load_dotenv
load_dotenv(Path(__file__).parent.parent / '.env')

from openai import OpenAI

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(threadName)s] %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# ==================== 500 种快餐/外卖名称 ====================
FASTFOOD_NAMES = [
    # ===== 中式快餐 - 米饭类 (50) =====
    "蛋炒饭", "扬州炒饭", "酱油炒饭", "咖喱炒饭", "虾仁炒饭",
    "牛肉炒饭", "鸡肉炒饭", "腊肠炒饭", "火腿炒饭", "培根炒饭",
    "黄金蛋炒饭", "菠萝炒饭", "泡菜炒饭", "海鲜炒饭", "什锦炒饭",
    "盖浇饭", "红烧肉盖饭", "鱼香肉丝盖饭", "宫保鸡丁盖饭", "麻婆豆腐盖饭",
    "番茄炒蛋盖饭", "青椒肉丝盖饭", "回锅肉盖饭", "糖醋里脊盖饭", "咖喱鸡肉饭",
    "卤肉饭", "排骨饭", "鸡腿饭", "烧鸭饭", "叉烧饭",
    "煲仔饭", "腊味煲仔饭", "滑鸡煲仔饭", "排骨煲仔饭", "牛肉煲仔饭",
    "黄焖鸡米饭", "黄焖排骨饭", "红烧牛肉饭", "照烧鸡腿饭", "日式咖喱饭",
    "石锅拌饭", "韩式拌饭", "牛肉拌饭", "三鲜拌饭", "肥牛饭",
    "猪扒饭", "鸡扒饭", "牛扒饭", "黑椒牛柳饭", "蒜香排骨饭",
    
    # ===== 中式快餐 - 面食类 (60) =====
    "兰州拉面", "牛肉拉面", "红烧牛肉面", "清汤牛肉面", "酸菜牛肉面",
    "担担面", "炸酱面", "热干面", "刀削面", "油泼面",
    "阳春面", "葱油拌面", "麻酱面", "凉面", "鸡丝凉面",
    "炒面", "肉丝炒面", "海鲜炒面", "什锦炒面", "干炒牛河",
    "炒米粉", "肉丝炒米粉", "星洲炒米粉", "桂林米粉", "螺蛳粉",
    "过桥米线", "砂锅米线", "酸辣粉", "重庆小面", "武汉热干面",
    "刀削面", "臊子面", "岐山臊子面", "biangbiang面", "裤带面",
    "烩面", "羊肉烩面", "河南烩面", "板面", "安徽板面",
    "肠粉", "牛肉肠粉", "虾仁肠粉", "叉烧肠粉", "鸡蛋肠粉",
    "云吞面", "鲜虾云吞面", "馄饨面", "饺子", "水饺",
    "蒸饺", "煎饺", "锅贴", "小笼包", "生煎包",
    "灌汤包", "肉包子", "菜包子", "豆沙包", "馒头",
    
    # ===== 中式快餐 - 小吃类 (50) =====
    "煎饼果子", "手抓饼", "鸡蛋灌饼", "葱油饼", "肉夹馍",
    "凉皮", "肉夹馍", "羊肉泡馍", "胡辣汤", "豆腐脑",
    "油条", "豆浆", "烧饼", "麻团", "糯米鸡",
    "粽子", "肉粽", "蛋黄肉粽", "豆沙粽", "八宝粥",
    "皮蛋瘦肉粥", "鸡肉粥", "海鲜粥", "白粥", "小米粥",
    "春卷", "炸春卷", "蛋卷", "虾饺", "烧麦",
    "臭豆腐", "炸臭豆腐", "麻辣烫", "关东煮", "串串香",
    "烤冷面", "炒年糕", "韩式炒年糕", "糍粑", "驴打滚",
    "炸鸡柳", "炸鸡排", "炸鸡腿", "炸鸡翅", "盐酥鸡",
    "烤肠", "烤面筋", "烤鱿鱼", "烤串", "羊肉串",
    
    # ===== 西式快餐 - 汉堡类 (40) =====
    "汉堡", "牛肉汉堡", "鸡肉汉堡", "鱼肉汉堡", "猪肉汉堡",
    "芝士汉堡", "双层芝士汉堡", "培根芝士汉堡", "巨无霸", "麦辣鸡腿堡",
    "香辣鸡腿堡", "奥尔良鸡腿堡", "新奥尔良烤鸡堡", "劲脆鸡腿堡", "田园鸡腿堡",
    "板烧鸡腿堡", "麦香鸡", "麦香鱼", "吉士汉堡", "双层吉士汉堡",
    "安格斯牛堡", "川辣双鸡堡", "金拱门汉堡", "原味鸡腿堡", "香脆鸡腿堡",
    "辣堡", "鳕鱼堡", "虾堡", "鸡肉卷", "老北京鸡肉卷",
    "墨西哥鸡肉卷", "嫩牛五方", "川香双鸡堡", "培根蛋堡", "早餐汉堡",
    "猪柳蛋堡", "猪柳麦满分", "烟肉蛋麦满分", "吉士蛋麦满分", "大脆鸡扒堡",
    
    # ===== 西式快餐 - 炸鸡类 (30) =====
    "炸鸡", "原味炸鸡", "香辣炸鸡", "脆皮炸鸡", "韩式炸鸡",
    "蜂蜜炸鸡", "甜辣炸鸡", "蒜香炸鸡", "酱油炸鸡", "炸鸡块",
    "鸡米花", "上校鸡块", "黄金鸡块", "麦乐鸡", "劲爆鸡米花",
    "炸鸡翅", "香辣鸡翅", "奥尔良烤翅", "新奥尔良烤翅", "蜜汁烤翅",
    "烤鸡腿", "炸鸡腿", "脆皮鸡腿", "鸡柳", "香酥鸡柳",
    "鸡排", "大鸡排", "台湾大鸡排", "炸鸡排", "香脆鸡排",
    
    # ===== 西式快餐 - 薯条披萨类 (30) =====
    "薯条", "大薯条", "中薯条", "小薯条", "卷卷薯条",
    "薯饼", "薯角", "芝士薯条", "培根薯条", "薯泥",
    "披萨", "意式披萨", "美式披萨", "芝士披萨", "培根披萨",
    "夏威夷披萨", "海鲜披萨", "牛肉披萨", "蔬菜披萨", "榴莲披萨",
    "烤肠披萨", "鸡肉披萨", "金枪鱼披萨", "玛格丽特披萨", "意大利香肠披萨",
    "洋葱圈", "芝士条", "鸡肉沙拉", "蔬菜沙拉", "凯撒沙拉",
    
    # ===== 日韩料理 (50) =====
    "寿司", "三文鱼寿司", "金枪鱼寿司", "鳗鱼寿司", "虾寿司",
    "手卷", "加州卷", "天妇罗", "炸虾天妇罗", "蔬菜天妇罗",
    "拉面", "日式拉面", "豚骨拉面", "味噌拉面", "酱油拉面",
    "乌冬面", "牛肉乌冬面", "咖喱乌冬面", "荞麦面", "冷荞麦面",
    "日式咖喱饭", "牛肉咖喱饭", "猪排咖喱饭", "鸡肉咖喱饭", "炸猪排饭",
    "亲子丼", "牛丼", "猪排丼", "天丼", "鳗鱼饭",
    "章鱼小丸子", "大阪烧", "日式煎饺", "味噌汤", "日式便当",
    "韩式炸鸡", "韩式烤肉", "石锅拌饭", "部队锅", "泡菜汤",
    "韩式冷面", "韩式炒年糕", "紫菜包饭", "韩式拌饭", "参鸡汤",
    "芝士年糕", "辣炒年糕", "韩式炸酱面", "韩式海鲜面", "韩式牛肉汤",
    
    # ===== 东南亚料理 (30) =====
    "泰式炒河粉", "冬阴功汤", "绿咖喱鸡", "红咖喱牛肉", "菠萝炒饭",
    "芒果糯米饭", "泰式炒饭", "泰式凉拌", "越南河粉", "越南春卷",
    "越南法棍", "新加坡炒米粉", "海南鸡饭", "叻沙", "肉骨茶",
    "沙爹", "印尼炒饭", "马来炒面", "咖喱角", "印度飞饼",
    "印度咖喱", "黄油鸡", "烤饼", "印度炒饭", "咖喱羊肉",
    "椰浆饭", "娘惹糕", "班兰蛋糕", "榴莲班戟", "芋圆",
    
    # ===== 烧烤烤肉类 (40) =====
    "烤肉", "韩式烤肉", "日式烤肉", "烤五花肉", "烤牛肉",
    "烤羊肉", "烤鸡肉", "烤鱼", "烤虾", "烤生蚝",
    "烤串", "羊肉串", "牛肉串", "鸡肉串", "烤鸡翅",
    "烤鸡腿", "烤排骨", "烤猪蹄", "烤肠", "烤玉米",
    "烤茄子", "烤金针菇", "烤韭菜", "烤豆腐", "烤土豆",
    "铁板烧", "铁板牛肉", "铁板鱿鱼", "铁板豆腐", "铁板炒饭",
    "自助烤肉", "烤肉拌饭", "烤肉饭", "烤鸭", "北京烤鸭",
    "脆皮烤鸭", "烧鹅", "烧鸡", "叉烧", "蜜汁叉烧",
    
    # ===== 火锅/麻辣烫类 (30) =====
    "火锅", "麻辣火锅", "清汤火锅", "番茄火锅", "菌汤火锅",
    "牛油火锅", "鸳鸯火锅", "羊肉火锅", "牛肉火锅", "海鲜火锅",
    "麻辣烫", "冒菜", "串串香", "钵钵鸡", "冷锅串串",
    "小火锅", "自助火锅", "旋转小火锅", "一人食火锅", "酸菜鱼",
    "水煮鱼", "水煮肉片", "毛血旺", "干锅", "干锅牛蛙",
    "干锅鸡", "干锅虾", "干锅花菜", "干锅土豆片", "干锅肥肠",
    
    # ===== 盖饭/套餐类 (40) =====
    "商务套餐", "双拼饭", "三拼饭", "自选套餐", "工作餐",
    "便当", "日式便当", "台式便当", "港式便当", "中式便当",
    "鸡腿套餐", "排骨套餐", "牛肉套餐", "鱼排套餐", "猪排套餐",
    "咖喱套餐", "照烧套餐", "红烧套餐", "糖醋套餐", "宫保套餐",
    "麻辣香锅", "干锅套餐", "铁板套餐", "石锅套餐", "砂锅套餐",
    "鸡公煲", "黄焖鸡套餐", "红烧肉套餐", "梅菜扣肉饭", "东坡肉饭",
    "酸菜鱼饭", "剁椒鱼头饭", "清蒸鱼饭", "红烧鱼饭", "糖醋鱼饭",
    "蒜蓉虾饭", "油焖虾饭", "香辣虾饭", "小龙虾饭", "蟹黄饭",
    
    # ===== 饮品甜点类 (50) =====
    "奶茶", "珍珠奶茶", "波霸奶茶", "芋圆奶茶", "红豆奶茶",
    "抹茶奶茶", "巧克力奶茶", "焦糖奶茶", "椰果奶茶", "布丁奶茶",
    "果茶", "柠檬茶", "百香果茶", "芒果茶", "葡萄茶",
    "水果茶", "杨枝甘露", "芒果西米露", "椰汁西米露", "红豆沙冰",
    "绿豆沙冰", "芒果沙冰", "草莓沙冰", "奥利奥沙冰", "抹茶星冰乐",
    "咖啡", "拿铁", "美式咖啡", "卡布奇诺", "摩卡",
    "冰淇淋", "圣代", "麦旋风", "甜筒", "雪糕",
    "蛋挞", "葡式蛋挞", "芝士蛋糕", "提拉米苏", "慕斯蛋糕",
    "泡芙", "马卡龙", "曲奇", "布朗尼", "华夫饼",
    "可丽饼", "铜锣烧", "蛋糕卷", "千层蛋糕", "班戟",
]

# 线程锁
_lock = threading.Lock()


def get_qwen_client():
    """获取 Qwen 客户端"""
    api_key = os.getenv('DASHSCOPE_API_KEY')
    if not api_key:
        raise RuntimeError("未设置 DASHSCOPE_API_KEY")
    return OpenAI(
        api_key=api_key,
        base_url='https://dashscope.aliyuncs.com/compatible-mode/v1',
    )


def lookup_from_db(name: str) -> Optional[Dict]:
    """从数据库查询营养数据"""
    try:
        from app.food_mapper import get_food_mapper
        mapper = get_food_mapper()
        result = mapper.lookup(name, "meal")
        
        # 如果置信度高，说明找到了
        if result.confidence >= 0.7 and result.db_key != "unknown" and not result.db_key.startswith("default_"):
            return {
                "name": name,
                "db_key": result.db_key,
                "calories": result.calories_per_100g,
                "protein": result.protein_per_100g,
                "carbs": result.carbs_per_100g,
                "fat": result.fat_per_100g,
                "source": "database",
                "confidence": result.confidence,
            }
    except Exception as e:
        logger.warning(f"数据库查询失败 ({name}): {e}")
    
    return None


def estimate_with_llm(client: OpenAI, foods: List[str], model: str = "qwen-plus") -> List[Dict]:
    """用 LLM 估算营养数据"""
    
    food_list = "\n".join([f"{i+1}. {f}" for i, f in enumerate(foods)])
    
    prompt = f"""你是营养学专家。请估算以下快餐/外卖食品的营养成分（每份/每100克）。

食品列表：
{food_list}

请返回 JSON 数组，每个元素包含：
- name: 食品名称
- portion_g: 典型一份的重量（克）
- calories: 每100克热量（千卡）
- protein: 每100克蛋白质（克）
- carbs: 每100克碳水化合物（克）
- fat: 每100克脂肪（克）

要求：
1. 只输出 JSON 数组，无其他文字
2. 数值要合理，参考常见快餐热量
3. 考虑中国快餐的实际配料和烹饪方式

参考热量范围：
- 米饭类盖浇饭: 150-200 kcal/100g
- 炒饭炒面: 180-250 kcal/100g
- 汉堡: 220-280 kcal/100g
- 炸鸡: 250-320 kcal/100g
- 披萨: 250-300 kcal/100g
- 奶茶: 40-80 kcal/100ml"""

    try:
        response = client.chat.completions.create(
            model=model,
            messages=[{"role": "user", "content": prompt}],
            temperature=0.1,
            max_tokens=4000,
        )
        
        text = response.choices[0].message.content.strip()
        
        # 提取 JSON
        if '```' in text:
            import re
            match = re.search(r'```(?:json)?\s*([\s\S]*?)\s*```', text)
            if match:
                text = match.group(1)
        
        results = json.loads(text)
        
        # 添加来源标记
        for r in results:
            r['source'] = 'llm_estimate'
            r['confidence'] = 0.7
        
        return results
        
    except Exception as e:
        logger.error(f"LLM 估算失败: {e}")
        return []


def generate_fastfood_mappings(workers: int = 5, batch_size: int = 20):
    """生成快餐映射表"""
    
    output_path = Path(__file__).parent.parent / "data" / "fastfood_mappings.json"
    
    # 加载已有数据
    existing = {}
    if output_path.exists():
        with open(output_path, 'r', encoding='utf-8') as f:
            existing = json.load(f)
        logger.info(f"已加载 {len(existing)} 条映射")
    
    # 过滤未处理的
    pending = [name for name in FASTFOOD_NAMES if name not in existing]
    logger.info(f"待处理: {len(pending)} 种快餐")
    
    if not pending:
        logger.info("所有快餐已处理完成")
        return existing
    
    # 第一步：从数据库查询
    logger.info("=== 阶段1: 数据库查询 ===")
    db_found = []
    db_missing = []
    
    for name in pending:
        result = lookup_from_db(name)
        if result:
            existing[name] = result
            db_found.append(name)
        else:
            db_missing.append(name)
    
    logger.info(f"数据库找到: {len(db_found)} 种")
    logger.info(f"需要 LLM 估算: {len(db_missing)} 种")
    
    # 保存进度
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(existing, f, ensure_ascii=False, indent=2)
    
    if not db_missing:
        return existing
    
    # 第二步：LLM 估算
    logger.info("=== 阶段2: LLM 估算 ===")
    client = get_qwen_client()
    
    # 分批处理
    batches = [db_missing[i:i+batch_size] for i in range(0, len(db_missing), batch_size)]
    total_batches = len(batches)
    
    def process_batch(batch_id: int, batch: List[str]) -> List[Dict]:
        results = estimate_with_llm(client, batch)
        logger.info(f"批次 {batch_id}/{total_batches} 完成，估算 {len(results)} 种")
        return results
    
    # 多线程处理
    with ThreadPoolExecutor(max_workers=workers, thread_name_prefix="LLM") as executor:
        futures = {
            executor.submit(process_batch, i + 1, batch): batch
            for i, batch in enumerate(batches)
        }
        
        for future in as_completed(futures):
            try:
                results = future.result()
                with _lock:
                    for r in results:
                        name = r.get('name', '')
                        if name:
                            existing[name] = r
                    
                    # 保存进度
                    with open(output_path, 'w', encoding='utf-8') as f:
                        json.dump(existing, f, ensure_ascii=False, indent=2)
                        
            except Exception as e:
                logger.error(f"批次处理异常: {e}")
            
            time.sleep(0.5)  # 避免限流
    
    logger.info(f"完成！共 {len(existing)} 种快餐映射")
    
    # 生成 Python 代码
    generate_python_code(existing, output_path.with_suffix('.py'))
    
    return existing


def generate_python_code(mappings: Dict, output_path: Path):
    """生成 Python 代码"""
    
    lines = [
        "#!/usr/bin/env python3",
        '"""快餐/外卖热量映射表 - 自动生成"""',
        "",
        "# 格式: \"食品名\": (热量, 蛋白质, 碳水, 脂肪, 典型份量g)",
        "FASTFOOD_NUTRITION = {",
    ]
    
    for name, info in sorted(mappings.items()):
        cal = info.get('calories', 150)
        pro = info.get('protein', 5)
        carb = info.get('carbs', 20)
        fat = info.get('fat', 5)
        portion = info.get('portion_g', 300)
        source = info.get('source', 'unknown')
        
        lines.append(f'    "{name}": ({cal}, {pro}, {carb}, {fat}, {portion}),  # {source}')
    
    lines.append("}")
    
    with open(output_path, 'w', encoding='utf-8') as f:
        f.write("\n".join(lines))
    
    logger.info(f"Python 代码已保存: {output_path}")


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser(description='生成快餐热量映射')
    parser.add_argument('--workers', type=int, default=5, help='并行线程数')
    parser.add_argument('--batch-size', type=int, default=20, help='每批处理数量')
    args = parser.parse_args()
    
    logger.info(f"开始生成快餐映射，共 {len(FASTFOOD_NAMES)} 种")
    generate_fastfood_mappings(workers=args.workers, batch_size=args.batch_size)
