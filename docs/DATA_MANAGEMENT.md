# 数据管理与安全方案

## 📊 数据分类

### 1. 用户数据
| 数据类型 | 敏感级别 | 保留策略 | 存储位置 |
|----------|----------|----------|----------|
| 设备ID | 低 | 永久 | users 表 |
| 健康目标 | 中 | 用户删除时清除 | users 表 |
| 身高体重 | 高 | 加密存储 | users 表 |
| 饮食偏好 | 低 | 用户删除时清除 | users 表 |

### 2. 业务数据
| 数据类型 | 敏感级别 | 保留策略 | 存储位置 |
|----------|----------|----------|----------|
| 用餐会话 | 低 | 90天 | meal_sessions 表 |
| 营养快照 | 低 | 90天 | meal_snapshots 表 |
| 食物识别结果 | 低 | 90天 | snapshot_foods 表 |
| API调用日志 | 低 | 30天 | api_logs 表 |

### 3. 图片数据
| 数据类型 | 敏感级别 | 保留策略 | 存储位置 |
|----------|----------|----------|----------|
| 食物照片 | 中 | 30天 | uploads/ 目录 |
| 图片元数据 | 低 | 与图片同步 | image_records 表 |

---

## 🔐 安全措施

### 1. 数据加密
```python
# 敏感字段加密（建议使用）
from cryptography.fernet import Fernet

# 生成密钥（存储在环境变量）
ENCRYPTION_KEY = os.getenv("ENCRYPTION_KEY")

# 加密用户敏感信息
def encrypt_field(value: str) -> str:
    f = Fernet(ENCRYPTION_KEY)
    return f.encrypt(value.encode()).decode()
```

### 2. 日志脱敏
```python
# 自动过滤敏感字段
SENSITIVE_FIELDS = ["password", "token", "phone", "email", "birth_date"]

def sanitize_log(data: dict) -> dict:
    return {k: "[REDACTED]" if any(s in k.lower() for s in SENSITIVE_FIELDS) else v 
            for k, v in data.items()}
```

### 3. API 安全
- **限流**：60次/分钟，1000次/天
- **认证**：设备ID + 签名验证
- **HTTPS**：强制使用 TLS 1.3

---

## 🗄️ 数据库模型

### 新增表结构

```sql
-- 用户档案
CREATE TABLE users (
    id VARCHAR PRIMARY KEY,
    device_id VARCHAR UNIQUE,
    nickname VARCHAR,
    gender VARCHAR,
    birth_year INTEGER,
    height_cm FLOAT,
    weight_kg FLOAT,
    target_calories FLOAT DEFAULT 2000,
    dietary_preference VARCHAR,
    allergies JSON,
    total_meals INTEGER DEFAULT 0,
    total_calories FLOAT DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- API 调用日志
CREATE TABLE api_logs (
    id VARCHAR PRIMARY KEY,
    user_id VARCHAR,
    device_id VARCHAR,
    endpoint VARCHAR,
    method VARCHAR,
    status_code INTEGER,
    response_time_ms INTEGER,
    food_count INTEGER,
    total_calories FLOAT,
    category VARCHAR,
    model_used VARCHAR,
    error_message TEXT,
    created_at TIMESTAMP
);
CREATE INDEX idx_api_logs_user_date ON api_logs(user_id, created_at);

-- 图片记录
CREATE TABLE image_records (
    id VARCHAR PRIMARY KEY,
    user_id VARCHAR,
    session_id VARCHAR,
    filename VARCHAR,
    storage_path VARCHAR,
    file_size INTEGER,
    status VARCHAR DEFAULT 'active',
    retention_days INTEGER DEFAULT 30,
    expires_at TIMESTAMP,
    created_at TIMESTAMP
);
CREATE INDEX idx_images_status_expires ON image_records(status, expires_at);

-- 每日统计
CREATE TABLE daily_stats (
    id VARCHAR PRIMARY KEY,
    user_id VARCHAR,
    date VARCHAR,
    api_calls INTEGER,
    meals_recorded INTEGER,
    total_calories FLOAT,
    avg_calories_per_meal FLOAT,
    category_distribution JSON,
    avg_response_time_ms FLOAT,
    error_rate FLOAT,
    created_at TIMESTAMP
);
```

---

## 🔧 管理 API

### 存储统计
```bash
GET /api/v1/admin/storage/stats

# 响应
{
  "total_files": 82,
  "total_size_mb": 7.14,
  "by_date": {
    "2025-11-26": {"count": 76, "size": 6100022}
  }
}
```

### 清理过期图片
```bash
# 预览模式
POST /api/v1/admin/storage/cleanup?days=30&dry_run=true

# 实际删除
POST /api/v1/admin/storage/cleanup?days=30&dry_run=false

# 响应
{
  "dry_run": false,
  "files_to_delete": 10,
  "freed_mb": 1.5,
  "message": "已删除 10 个文件"
}
```

### API 统计
```bash
GET /api/v1/admin/api-stats
```

---

## ⏰ 定时任务

### 图片清理（每天凌晨3点）
```python
# 在 main.py 中启用
@app.on_event("startup")
async def start_cleanup_task():
    asyncio.create_task(scheduled_cleanup())
```

### 统计聚合（每天凌晨4点）
```python
# 聚合每日统计
data_manager.aggregate_daily_stats()
```

---

## 📈 数据积累与分析

### 1. 用户行为分析
```python
# 从 api_logs 分析
- 使用频率（每日/每周活跃）
- 高峰时段（早/中/晚餐分布）
- 常用功能（识别/查询/会话）
```

### 2. 食物偏好分析
```python
# 从 snapshot_foods 聚合
- Top 10 常吃食物
- 营养摄入趋势
- 食物分类偏好（正餐/零食比例）
```

### 3. 模型优化数据
```python
# 用于模型迭代
- VLM 识别准确率（通过用户反馈）
- 常见识别错误（收集修正数据）
- 新食材发现（补充营养库）
```

### 4. 数据导出（GDPR 合规）
```python
# 用户数据导出
data_manager.export_user_data(user_id)

# 用户数据删除
data_manager.delete_user_data(user_id, delete_images=True)
```

---

## 🚀 部署建议

### 短期（当前阶段）
1. ✅ 使用 SQLite 本地存储
2. ✅ 图片存储在本地 uploads/ 目录
3. ✅ 30天自动清理

### 中期（用户量增长后）
1. 迁移到 PostgreSQL
2. 图片上传到 OSS（阿里云/腾讯云）
3. 添加 Redis 缓存热点数据

### 长期（规模化）
1. 数据库分库分表
2. CDN 加速图片访问
3. 数据仓库（用于分析）

---

## 📋 检查清单

### 安全
- [ ] 配置 ENCRYPTION_KEY 环境变量
- [ ] 启用 HTTPS
- [ ] 配置 API 限流
- [ ] 定期安全审计

### 合规
- [ ] 隐私政策更新
- [ ] 用户数据导出功能
- [ ] 用户数据删除功能
- [ ] 数据保留策略说明

### 运维
- [ ] 启用定时清理任务
- [ ] 配置存储告警（>10GB）
- [ ] 配置数据库备份
- [ ] 监控 API 响应时间
