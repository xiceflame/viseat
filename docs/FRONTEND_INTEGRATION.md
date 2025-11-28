# 前端集成指南 - 数据关联

## 📋 前端需要传递的信息

### 核心标识（必须）

| 字段 | 说明 | 来源 | 示例 |
|------|------|------|------|
| `device_id` | 设备唯一标识 | 眼镜硬件ID或手机UUID | `"rokid_max_001"` |
| `user_id` | 用户ID | 首次注册后由后端返回 | `"usr_abc123"` |
| `session_id` | 用餐会话ID | 开始用餐时由后端返回 | `"sess_xyz789"` |

### 数据流程

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  首次启动   │────►│  用户注册   │────►│ 获取user_id │
└─────────────┘     └─────────────┘     └─────────────┘
                           │
                           ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  开始用餐   │────►│ 创建会话   │────►│获取session_id│
└─────────────┘     └─────────────┘     └─────────────┘
                           │
                           ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   拍照识别  │────►│ 上传+识别  │────►│ 关联到会话  │
└─────────────┘     └─────────────┘     └─────────────┘
```

---

## 🔌 API 调用示例

### 1. 用户注册/登录（首次启动）

```bash
POST /api/v1/user/register
Content-Type: application/json

{
  "device_id": "rokid_max_001",      # 必须：眼镜设备ID
  "device_type": "glasses",           # 必须：glasses/phone
  "device_model": "Rokid Max Pro",    # 可选：设备型号
  "app_version": "1.0.0"              # 可选：APP版本
}
```

**响应：**
```json
{
  "user_id": "usr_abc123",
  "device_id": "rokid_max_001",
  "is_new_user": true,
  "token": "eyJ..."  // 后续请求需携带
}
```

### 2. 开始用餐会话

```bash
POST /api/v1/meal/start
Authorization: Bearer {token}
Content-Type: application/json

{
  "user_id": "usr_abc123",           # 必须
  "device_id": "rokid_max_001",       # 必须
  "meal_type": "lunch",               # 可选：breakfast/lunch/dinner/snack
  "auto_capture_interval": 300        # 可选：自动拍照间隔（秒）
}
```

**响应：**
```json
{
  "session_id": "sess_xyz789",
  "status": "active",
  "start_time": "2025-11-26T15:30:00Z"
}
```

### 3. 图片上传 + 识别（带关联）

```bash
POST /api/v1/vision/analyze
Authorization: Bearer {token}
Content-Type: application/json

{
  "image_url": "https://viseat.cn/uploads/xxx.jpg",
  
  # === 关联信息（必须传递）===
  "user_id": "usr_abc123",
  "device_id": "rokid_max_001",
  "session_id": "sess_xyz789",
  
  # === 可选参数 ===
  "mode": "start",                    # start/update
  "snapshot_type": "manual",          # manual/auto
  "baseline_foods": [...]             # update模式需要
}
```

**响应：**
```json
{
  "snapshot_id": "snap_123",          # 快照ID
  "session_id": "sess_xyz789",
  "raw_llm": {...},
  "snapshot": {
    "foods": [...],
    "nutrition": {
      "calories": 650,
      "protein": 25,
      "carbs": 80,
      "fat": 20
    }
  }
}
```

### 4. 结束用餐会话

```bash
POST /api/v1/meal/{session_id}/end
Authorization: Bearer {token}
Content-Type: application/json

{
  "user_id": "usr_abc123"
}
```

**响应：**
```json
{
  "session_id": "sess_xyz789",
  "status": "completed",
  "duration_minutes": 25,
  "summary": {
    "total_calories": 850,
    "total_protein": 35,
    "total_carbs": 100,
    "total_fat": 28,
    "food_count": 6
  },
  "advice": {
    "summary": "本餐营养均衡，热量适中",
    "suggestions": ["晚餐可适当增加蔬菜摄入"]
  }
}
```

---

## 📱 请求头规范

所有 API 请求建议携带：

```http
Authorization: Bearer {token}
X-Device-ID: rokid_max_001
X-User-ID: usr_abc123
X-Session-ID: sess_xyz789          # 用餐期间
X-App-Version: 1.0.0
X-Request-ID: req_unique_id        # 用于日志追踪
```

---

## 💾 本地存储建议

### 必须持久化存储

```javascript
// 用户信息（永久存储）
localStorage.setItem('user_id', 'usr_abc123');
localStorage.setItem('device_id', 'rokid_max_001');
localStorage.setItem('auth_token', 'eyJ...');

// 当前会话（用餐期间）
sessionStorage.setItem('current_session_id', 'sess_xyz789');
```

### 初始化流程

```javascript
async function initApp() {
  // 1. 获取设备ID
  const deviceId = await getDeviceId();  // 从硬件获取或生成UUID
  
  // 2. 检查是否已注册
  let userId = localStorage.getItem('user_id');
  
  if (!userId) {
    // 3. 首次注册
    const result = await api.post('/user/register', {
      device_id: deviceId,
      device_type: 'glasses'
    });
    userId = result.user_id;
    localStorage.setItem('user_id', userId);
    localStorage.setItem('auth_token', result.token);
  }
  
  return { userId, deviceId };
}
```

---

## 🔄 完整用餐流程

```javascript
class MealTracker {
  constructor(userId, deviceId) {
    this.userId = userId;
    this.deviceId = deviceId;
    this.sessionId = null;
  }
  
  // 开始用餐
  async startMeal(mealType = 'meal') {
    const result = await api.post('/meal/start', {
      user_id: this.userId,
      device_id: this.deviceId,
      meal_type: mealType
    });
    this.sessionId = result.session_id;
    return result;
  }
  
  // 拍照识别
  async captureAndAnalyze(imageUrl, mode = 'start', baselineFoods = null) {
    const payload = {
      image_url: imageUrl,
      user_id: this.userId,
      device_id: this.deviceId,
      session_id: this.sessionId,
      mode: mode
    };
    
    if (mode === 'update' && baselineFoods) {
      payload.baseline_foods = baselineFoods;
    }
    
    return await api.post('/vision/analyze', payload);
  }
  
  // 结束用餐
  async endMeal() {
    const result = await api.post(`/meal/${this.sessionId}/end`, {
      user_id: this.userId
    });
    this.sessionId = null;
    return result;
  }
}

// 使用示例
const tracker = new MealTracker('usr_abc123', 'rokid_max_001');
await tracker.startMeal('lunch');
const result1 = await tracker.captureAndAnalyze(imageUrl1, 'start');
const result2 = await tracker.captureAndAnalyze(imageUrl2, 'update', result1.raw_llm.foods);
const summary = await tracker.endMeal();
```

---

## 📊 数据关联示例

### 后端存储结构

```
User (usr_abc123)
├── device_id: rokid_max_001
├── total_meals: 156
│
├── MealSession (sess_xyz789)
│   ├── meal_type: lunch
│   ├── start_time: 2025-11-26 15:30
│   ├── total_calories: 850
│   │
│   ├── MealSnapshot (snap_001) - 开始时
│   │   ├── image_url: https://...
│   │   ├── snapshot_type: baseline
│   │   ├── total_calories: 850 (盘中)
│   │   └── foods: [rice, pork, vegetables]
│   │
│   ├── MealSnapshot (snap_002) - 中间
│   │   ├── image_url: https://...
│   │   ├── snapshot_type: auto
│   │   ├── delta_calories: 320 (已消耗)
│   │   └── foods: [rice, vegetables]
│   │
│   └── NutritionAdvice
│       ├── summary: "本餐营养均衡"
│       └── suggestions: [...]
│
└── DailyNutrition (2025-11-26)
    ├── lunch_count: 1
    ├── total_calories: 1850
    └── calories_pct: 92.5%
```

---

## ⚠️ 注意事项

### 1. 必传字段检查

```javascript
function validateRequest(payload, requiredFields) {
  for (const field of requiredFields) {
    if (!payload[field]) {
      throw new Error(`Missing required field: ${field}`);
    }
  }
}

// 图片识别必传
validateRequest(payload, ['image_url', 'user_id', 'device_id', 'session_id']);
```

### 2. 网络断线处理

```javascript
// 离线队列
const offlineQueue = [];

async function analyzeWithRetry(payload) {
  try {
    return await api.post('/vision/analyze', payload);
  } catch (e) {
    if (isNetworkError(e)) {
      offlineQueue.push({ type: 'analyze', payload, timestamp: Date.now() });
      return { offline: true, queued: true };
    }
    throw e;
  }
}

// 恢复网络后同步
async function syncOfflineData() {
  for (const item of offlineQueue) {
    await api.post('/vision/analyze', item.payload);
  }
  offlineQueue.length = 0;
}
```

### 3. 会话超时处理

```javascript
// 会话超时（默认2小时）
const SESSION_TIMEOUT = 2 * 60 * 60 * 1000;

function checkSessionTimeout(startTime) {
  if (Date.now() - startTime > SESSION_TIMEOUT) {
    // 自动结束会话
    endMeal();
    alert('用餐会话已超时自动结束');
  }
}
```

---

## 🔑 获取 device_id 的方式

### Rokid 眼镜

```javascript
// 从 Rokid SDK 获取
const deviceId = RokidSDK.getDeviceId();
```

### Android 手机

```javascript
// 使用 Android ID
const deviceId = Settings.Secure.getString(
  contentResolver, 
  Settings.Secure.ANDROID_ID
);
```

### iOS 手机

```javascript
// 使用 identifierForVendor
const deviceId = UIDevice.current.identifierForVendor?.uuidString;
```

### Web 端（开发测试）

```javascript
// 生成并持久化
let deviceId = localStorage.getItem('device_id');
if (!deviceId) {
  deviceId = 'web_' + crypto.randomUUID();
  localStorage.setItem('device_id', deviceId);
}
```
