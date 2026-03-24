# VISEAT 食智 - UI 设计规范

## 设计理念

本应用采用 **Premium Design System** 高端设计系统，参考 Apple、Dieter Rams、无印良品的设计理念。

### 核心原则
- **克制**：避免过度设计，减少渐变和特效
- **精致**：注重细节和工艺感
- **质感**：通过微妙的光影和纹理传递品质
- **呼吸感**：充足的留白和间距

## 配色方案

### Premium 主色 - 柔和高级
| 颜色名称 | 色值 | 用途 |
|---------|------|------|
| Sage | `#5B8A72` | 鼠尾草绿 - 主要操作、成功状态 |
| Sage Light | `#7BA892` | 浅鼠尾草 - 悬停状态 |
| Sage Muted | `#B8D4C8` | 柔和鼠尾草 - 背景点缀 |

### 中性暖色 - 增加温度感
| 颜色名称 | 色值 | 用途 |
|---------|------|------|
| Warmth | `#F5F1EB` | 暖白 - 主背景 |
| Linen | `#FAF8F5` | 亚麻白 - 渐变起始 |
| Cream | `#F8F6F1` | 奶油白 - 卡片背景 |
| Sand | `#E8E2D9` | 沙色 - 轨道、分割线 |

### 文字色 - 避免纯黑
| 颜色名称 | 色值 | 用途 |
|---------|------|------|
| Ink | `#1D1D1F` | 墨色 - 主要文字 |
| Ink Light | `#424245` | 浅墨色 - 次要标题 |
| Ink Muted | `#6E6E73` | 柔和墨色 - 次要文字 |
| Ink Subtle | `#86868B` | 淡墨色 - 提示文字 |

### 功能色 - 柔和版本
| 颜色名称 | 色值 | 用途 |
|---------|------|------|
| Success | `#6B9B7A` | 柔和绿 - 成功 |
| Warning | `#D4A574` | 柔和橙 - 警告 |
| Error | `#B85C5C` | 柔和红 - 错误 |
| Info | `#6B8FB8` | 柔和蓝 - 信息 |

### 语义色 (Semantic Colors)
| 颜色名称 | 色值 | 用途 |
|---------|------|------|
| Calorie Red | `#FF6B6B` | 热量显示 |
| Protein Cyan | `#5AC8FA` | 蛋白质 |
| Carbs Amber | `#FFCC00` | 碳水化合物 |
| Fat Green | `#34C759` | 脂肪 |

### 灰度系统 (Neutral Colors)
| 颜色名称 | 色值 | 用途 |
|---------|------|------|
| Apple Gray 1 | `#8E8E93` | 次要文字 |
| Apple Gray 2 | `#AEAEB2` | 占位符文字 |
| Apple Gray 3 | `#C7C7CC` | 分割线、禁用图标 |
| Apple Gray 4 | `#D1D1D6` | 边框 |
| Apple Gray 5 | `#E5E5EA` | 背景分隔 |
| Apple Gray 6 | `#F2F2F7` | 页面背景 |

## 圆角规范

| 尺寸 | 数值 | 应用场景 |
|------|------|---------|
| Extra Small | 4dp | 小标签 |
| Small | 8dp | 按钮、小卡片 |
| Medium | 12dp | 输入框、中等卡片 |
| Large | 16dp | 大卡片、对话框 |
| Extra Large | 20dp | 主要卡片、头部区域 |

## 阴影规范

采用分层阴影系统，营造丰富的层次感：

| 层级 | 数值 | 用途 |
|------|------|------|
| None | 0dp | 无阴影 |
| Extra Small | 1dp | 微小阴影 - 分割线、边框 |
| Small | 2dp | 小阴影 - 次要卡片 |
| Medium | 4dp | 中等阴影 - 普通卡片 |
| Large | 8dp | 大阴影 - 重要卡片 |
| Extra Large | 16dp | 超大阴影 - 弹窗、浮动元素 |

```kotlin
// 使用示例
.shadow(
    elevation = AppElevation.Medium,
    shape = RoundedCornerShape(AppCorners.Large),
    ambientColor = Color.Black.copy(alpha = 0.05f),
    spotColor = Color.Black.copy(alpha = 0.1f)
)
```

## 字体规范

基于 Apple SF Pro 字体风格：

| 样式 | 字重 | 字号 | 行高 | 用途 |
|------|------|------|------|------|
| Display Large | Bold | 34sp | 41sp | 大标题 |
| Display Medium | Bold | 28sp | 34sp | 页面标题 |
| Display Small | SemiBold | 22sp | 28sp | 数字显示 |
| Headline Large | SemiBold | 20sp | 25sp | 卡片标题 |
| Headline Medium | SemiBold | 17sp | 22sp | 列表标题 |
| Body Large | Normal | 17sp | 22sp | 正文 |
| Body Medium | Normal | 15sp | 20sp | 次要正文 |
| Label Medium | Medium | 12sp | 16sp | 标签 |
| Label Small | Medium | 11sp | 13sp | 小标签 |

## 组件设计

### 卡片 (Card)
- 圆角: 16-20dp
- 内边距: 20dp
- 背景: 纯白色 (Light) / 深灰色 (Dark)
- 阴影: 2-4dp elevation

### 按钮 (Button)
- 主按钮: Apple Teal 背景，白色文字
- 次按钮: 透明背景，灰色边框
- 圆角: 12dp
- 高度: 48dp

### 底部导航 (Navigation Bar)
- 背景: Surface 颜色
- 选中状态: Apple Teal 图标和文字
- 未选中状态: Apple Gray 2
- 指示器: Apple Teal 12% 透明度

### 列表项 (List Item)
- 图标容器: 32dp 圆角方形
- 图标背景: 主题色 12% 透明度
- 分割线: 从图标右侧开始，0.5dp 厚度

## 动画规范

### 动画时长系统
| 类型 | 时长 | 用途 |
|------|------|------|
| Instant | 100ms | 即时 - 微交互 |
| Fast | 200ms | 快速 - 按钮点击 |
| Normal | 300ms | 正常 - 页面切换 |
| Slow | 500ms | 慢速 - 强调动画 |
| Very Slow | 800ms | 非常慢 - 庆祝动画 |
| Count Up | 1500ms | 数值计数动画 |
| Shimmer | 1200ms | 骨架屏波纹 |
| Auto Scroll | 5000ms | 自动轮播间隔 |

### 动画组件
- **ShimmerBox**: 骨架屏加载效果
- **AnimatedCounter**: 数值计数动画
- **GradientCircularProgress**: 渐变环形进度条
- **GradientArcProgress**: 渐变弧形进度条
- **CelebrationParticles**: 庆祝粒子效果
- **BreathingGlow**: 呼吸光晕效果
- **PulsingDot**: 脉冲圆点动画
- **RippleLoader**: 波纹扩散加载动画

## 暗色模式

暗色模式采用纯黑背景 (#000000)，配合深灰色卡片 (#1C1C1E)，保持高对比度的同时减少视觉疲劳。

### 暗色模式特殊处理
- **发光边框**: 使用渐变边框替代阴影，增强卡片边界感
- **背景渐变**: 从 #1C1C1E 到 #000000 的垂直渐变
- **透明度调整**: 卡片背景使用 95% 不透明度，增加层次感

```kotlin
// 深色模式发光边框
if (isDark) {
    Modifier.border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                GlowBorderDark.copy(alpha = 0.5f),
                ApplePurple.copy(alpha = 0.3f)
            )
        ),
        shape = RoundedCornerShape(AppCorners.Large)
    )
}
```

## 个性化建议系统

### 用户健康档案
视觉分析 API 会携带用户健康档案，后端 LLM 根据用户情况生成个性化建议：

| 健康状况 | 食物示例 | 建议示例 |
|---------|---------|---------|
| 糖尿病 | 可乐 | "含糖较高，糖尿病患者建议少量" |
| 高血压 | 腌菜 | "腌制食品含盐高，需注意" |
| 减重目标 | 炸鸡 | "热量较高，建议控制份量" |
| 无特殊情况 | 鸡胸肉 | "蛋白质丰富，搭配蔬菜更均衡" |

### API 请求格式
```json
POST /api/v1/vision/analyze
{
  "image_url": "...",
  "user_profile": {
    "health_conditions": ["糖尿病", "高血压"],
    "dietary_preferences": ["低盐", "低糖"],
    "health_goal": "lose_weight"
  }
}
```

## 渐变色系统

### 预定义渐变
| 名称 | 颜色 | 用途 |
|------|------|------|
| PrimaryGradient | Teal → Mint | 主要按钮和强调元素 |
| BlueGradient | Blue → Cyan | 信息卡片 |
| PurpleGradient | Purple → Blue | AI 功能 |
| WarmGradient | Orange → Pink | 警告和热量 |
| BackgroundGradientLight | Gray6 → White | 浅色页面背景 |
| BackgroundGradientDark | #1C1C1E → Black | 深色页面背景 |

## 增强型卡片组件

### GlassCard
玻璃态卡片，带毛玻璃效果，适用于重要内容展示

### GradientBorderCard
带渐变色边框的高级卡片样式

### FloatingCard
带有高级阴影效果的浮动卡片

### EnhancedCaloriesCard
带渐变弧形进度条的热量展示卡片

### MacroNutrientsCard
宏量营养素环形进度条卡片组

### GradientButton
渐变背景的主按钮

### CustomDatePickerDialog
自定义日期选择器对话框，用于目标时间选择
- 支持年/月/日选择
- 最小日期验证（至少2周后）
- Premium 样式设计

---

## 更新记录

### 2025年1月 - UI优化版本 v2

#### 新增功能
- **自定义目标日期选择**：在减重目标设置中，除了预设的3个月/6个月/1年选项外，新增"自定时间"选项，支持用户自由选择目标完成日期

#### UI优化
- **HistoryScreen**：全面优化为 Premium 设计风格
  - 添加渐变背景
  - 卡片阴影和圆角优化
  - 使用语义化颜色（CalorieRed 热量显示）
  - 空状态设计优化

- **DeviceScreen**：全面优化为 Premium 设计风格
  - 蓝牙状态图标化展示
  - 设备信息卡片优化
  - 错误状态提示优化
  - 按钮样式统一

#### 已有页面验证
- HomeScreen、StatsScreen、ProfileScreen 已采用 Premium 设计系统

*设计更新日期: 2025年1月*
