# NutriMate

每天 30 秒拍一餐，AI 记录营养；打开就知道「今天吃什么」，菜谱一键变购物清单。

> 面向减脂 / 增肌用户的健康饮食管理 Android App（MVP）。

## 产品定位

**Fitia 的功能闭环 × Cal AI 的拍照极简 × Lifesum 的习惯养成 × fud-ai 的本地优先架构**

- 拍照记录（AI 识别 → 修正 → 保存）
- 今日吃什么（按剩余预算 × 偏好 × 目标餐次生成菜谱）
- 一键购物清单（菜谱食材自动合并去重）
- BYOK：自带 AI Key（Gemini / OpenAI / 自定义 OpenAI 兼容端点），数据本地存储、无账号、无系统自动备份

## 里程碑

| 阶段 | 内容 | 状态 |
|------|------|------|
| 文档 | IA + PRD v0.2 + 交叉评审 #1 | ✅ 完成（docs/） |
| M1 | 工程骨架：分层架构 / Room / Hilt / AI Provider 抽象 / 导航 4 Tab | 🔨 进行中 |
| M2 | Onboarding + 首页 + 拍照记录 + 手动记录 + 历史日报 | 待做 |
| M3 | 今日吃什么 + 菜谱详情 + 购物清单 + 设置(AI Key/身体数据) | 待做 |
| M4 | 单测补全 / Lint / 降级演练 / 隐私断言 | 待做 |

## 架构

单模块 Clean Architecture（预留多模块拆分），单向数据流：

```
presentation (Compose UI + ViewModel + StateFlow)
   ↓
domain (纯 Kotlin: 模型 / 用例 / 仓库接口)   ← 100% 可单测，包含金标准算法
   ↓
data (Room / DataStore / KeyStore / AI Provider 适配)
   ↑
di (Hilt)
```

- AI Provider 可插拔：`NutritionAiService` 接口 + Mock/OpenAI 兼容实现
- API Key：Android Keystore AES-GCM 加密，明文不落盘
- 隐私：`android:allowBackup="false"`，无账号、无埋点

## 构建

```bash
# 依赖 SDK 于 local.properties
/root/gradle-8.9/bin/gradle :app:assembleDebug --no-daemon --console=plain
/root/gradle-8.9/bin/gradle :app:testDebugUnitTest --no-daemon --console=plain
```

## 目录

```
docs/          需求与评审文档（IA / PRD / review）
app/src/main/java/com/nutrimate/app/
  domain/      模型、用例（含金标准算法）、仓库接口
  data/        Room / DataStore / KeyStore / AI
  presentation/Compose UI + 导航
  di/          Hilt 模块
app/src/test/  单元测试（golden test / 购物清单合并 / 餐次推断）
```