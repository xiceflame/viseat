# 眼镜端 ↔ 手机端 蓝牙通信协议

> **版本**: 2.0.0 | **更新**: 2025-11-26

## ✅ 当前状态

眼镜端代码已启用**真机模式**（`USE_REAL_SDK = true`），可在 Rokid 设备上运行。

## 概述

眼镜端和手机端通过 Rokid CXR SDK 的蓝牙通道通信，使用 `Caps` 数据结构传输消息。

### SDK 对应关系
- **眼镜端**: CXR-S SDK (`cxr-service-bridge`) - 通过 `CXRServiceBridge.sendMessage()` 发送
- **手机端**: CXR-M SDK (`client-m`) - 通过 `CxrApi.setCustomCmdListener()` 接收

### ⚠️ 重要：CXR-M SDK 的 CustomCmdListener 不接收二进制数据参数
因此所有数据（包括图片）必须写入 Caps 中。

## 常量定义

```kotlin
// 蓝牙服务 UUID（双端必须一致）
const val BLUETOOTH_SERVICE_UUID = "00009100-0000-1000-8000-00805f9b34fb"

// 消息名称
object MsgName {
    const val IMAGE = "nutrition_image"           // 眼镜→手机: 图片
    const val COMMAND = "nutrition_command"       // 眼镜→手机: 指令
    const val RESULT = "nutrition_result"         // 手机→眼镜: 结果
    const val SESSION_STATUS = "session_status"   // 手机→眼镜: 会话状态
}

// 指令类型
object CommandType {
    const val START_MEAL = "start_meal"
    const val END_MEAL = "end_meal"
}
```

## 消息格式

### 1. 眼镜→手机：图片消息 (`nutrition_image`)

```
Caps 结构:
[0] String: 图片格式 = "jpeg"
[1] Int32:  数据大小 (bytes)
[2] Int64:  时间戳 (毫秒)
[3] Int32:  是否主动拍照 (1=是, 0=否)
[4] Binary: JPEG 图片二进制数据
```

**注意**：图片数据必须写入 Caps[4]，不能使用 sendMessage 的额外二进制参数。

### 2. 眼镜→手机：指令消息 (`nutrition_command`)

```
Caps 结构:
[0] String: 指令类型 = "start_meal" | "end_meal"
[1] Int64:  时间戳 (毫秒)
```

### 3. 手机→眼镜：营养结果 (`nutrition_result`)

| 索引 | 类型   | 说明                |
|------|--------|---------------------|
| 0    | String | 菜品描述（见下方说明）|
| 1    | Float  | 总热量 (kcal)       |
| 2    | Float  | 蛋白质 (g)          |
| 3    | Float  | 碳水化合物 (g)      |
| 4    | Float  | 脂肪 (g)            |
| 5    | String | LLM 建议文本        |

#### 菜品描述格式说明

后端 VLM (`/api/v1/vision/analyze`) 返回的是多菜品结构：
```json
{
  "raw_llm": {
    "is_food": true,
    "foods": [
      {"dish_name": "红烧肉", "cooking_method": "braise", "ingredients": [{"name_en": "pork", "weight_g": 150}]},
      {"dish_name": "米饭", "cooking_method": "steam", "ingredients": [{"name_en": "rice", "weight_g": 200}]}
    ]
  },
  "snapshot": {
    "foods": [...],
    "nutrition": {"calories": 650, "protein": 25, "carbs": 80, "fat": 28}
  }
}
```

**字段说明**：
- `cooking_method`: 英文烹饪方式（`raw`/`steam`/`boil`/`braise`/`stir-fry`/`deep-fry`）
- `name_en`: 英文食材名（用于数据库查询，如 `pork`, `rice`, `chicken breast`）

**手机端需要将 `raw_llm.foods[].dish_name` 组合成简洁的菜品描述**：
- 单道菜: `"红烧肉"`
- 两道菜: `"红烧肉 · 米饭"`
- 三道及以上: `"红烧肉等3道菜"` 或 `"红烧肉 · 米饭 · 青菜"`

### 4. 手机→眼镜：会话状态 (`session_status`)

```
Caps 结构:
[0] String: 消息类型 = "session_status"
[1] String: 会话ID
[2] String: 状态 = "active" | "ended"
[3] Float:  总摄入热量 (kcal)
[4] String: 消息文本 (如 "用餐结束，共摄入 650 千卡")
```

## 交互流程

### 开始用餐
```
眼镜: 用户长按侧键
眼镜 → 手机: nutrition_command (start_meal)
眼镜: 自动拍照
眼镜 → 手机: nutrition_image (isManualCapture=true)
手机: 调用后端 API
手机 → 眼镜: nutrition_result
手机 → 眼镜: session_status (active)
```

### 持续监测
```
眼镜: 5分钟定时器触发
眼镜 → 手机: nutrition_image (isManualCapture=false)
手机: 调用后端 API
手机 → 眼镜: nutrition_result
```

### 用户主动拍照
```
眼镜: 用户短按侧键
眼镜: 重置5分钟定时器
眼镜 → 手机: nutrition_image (isManualCapture=true)
手机: 调用后端 API（更仔细评估）
手机 → 眼镜: nutrition_result
```

### 结束用餐
```
眼镜: 用户长按侧键
眼镜: 自动拍照
眼镜 → 手机: nutrition_image (isManualCapture=true)
眼镜 → 手机: nutrition_command (end_meal)
手机: 调用后端 API
手机 → 眼镜: nutrition_result
手机 → 眼镜: session_status (ended, 总摄入热量, 评价文本)
```

## 眼镜端实现位置

- `BluetoothSender.kt` - 发送图片和指令
- `BluetoothReceiver.kt` - 接收结果和状态
- `Config.kt` - 常量定义

## 手机端实现要求

手机端需要实现：
1. 蓝牙服务端（CXR-M SDK）
2. 订阅 `nutrition_image` 和 `nutrition_command` 消息
3. 发送 `nutrition_result` 和 `session_status` 消息
4. 调用后端 API 获取识别结果
