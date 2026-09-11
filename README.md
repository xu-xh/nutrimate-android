# NutriMate

[![CI](https://github.com/xu-xh/nutrimate-android/actions/workflows/ci.yml/badge.svg)](https://github.com/xu-xh/nutrimate-android/actions/workflows/ci.yml)

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
| M1 | 工程骨架：分层架构 / Room / Hilt / AI Provider 抽象 | ✅ |
| M2 | Onboarding + 首页热量环 + 拍照/手动记录 + 历史日报 | ✅ |
| M3 | 今日吃什么（限次/预算区间/过敏）+ 菜谱详情 + 购物清单 + 设置(BYOK/身体数据) | ✅ |
| M4 | 单测补全（23 个全绿）/ 降级演练 / lint 工具链限制文档化 | ✅ |

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

## 构建与质量门禁

```bash
# 使用 Gradle Wrapper（本地与 CI 一致）
./gradlew :app:assembleDebug --no-daemon --console=plain
./gradlew :app:testDebugUnitTest --no-daemon --console=plain
```

CI（GitHub Actions，`.github/workflows/ci.yml`）：
- **push/PR 自动**：`assembleDebug + testDebugUnitTest`（质量门禁），上传单测报告与 debug APK 工件；
- **手动触发（workflow_dispatch）**：`instrumented` job —— API 26 模拟器运行时冒烟测试（app 真机安装 + 首页渲染断言）。注意：GitHub hosted runner 无 KVM，模拟器以纯软件（TCG）模式运行且 boot 极慢，仅建议在带 KVM 的自托管 runner（可修改 `runs-on`）或采用本地真机/模拟器时使用本 job；当前 run 已就绪，若在支持硬件加速的环境可直接运行连测。

质量门禁 = `assembleDebug` + `testDebugUnitTest`（23 个单测：金标准算法 / 食材合并去重 / 餐次推断 / 菜谱限次与预算过滤 / 菜谱 JSON 序列化）。

> **已知工具链限制**：AGP 8.7 的 `NonNullableMutableLiveData` lint detector 在 Kotlin 2.2 字节码上抛 IncompatibleClassChangeError（AGP/Kotlin 组合问题，非项目代码），已在 `app/build.gradle.kts` 中 `abortOnError = false` + disable 该 detector；CI 以构建+测试为门禁。升级 AGP 后可恢复 `lintDebug`。

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