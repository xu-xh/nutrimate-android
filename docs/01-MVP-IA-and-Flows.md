# NutriMate MVP 信息架构与核心页面流程

> 代号：**NutriMate**（暂定，可改）
> 定位：面向减脂/增肌用户的「拍照记录 + 今日吃什么 + 一键购物清单」健康饮食管理 App
> 参考对象：Fitia（功能闭环/食谱/购物清单）、Cal AI（拍照极简）、fud-ai（开源架构/BYOK 多 AI 提供商）
> 版本：v0.1（MVP 范围）

---

## 1. 产品愿景与差异化

- **一句话**：每天 30 秒拍一餐，AI 记录营养；想吃了打开 App 就知道「今天吃什么」，菜谱一键变购物清单。
- **差异化**（vs Fitia / Cal AI / Lifesum）：
  1. **拍照路径极短**（Cal AI 式：拍 → 确认 → 完成，≤3 步）
  2. **「吃什么」是核心回答**（Fitia 式：剩余热量预算 + 偏好 → 菜谱推荐，而非只给列表）
  3. **购物清单一键生成**（Fitia 式：菜谱 → 食材合并 → 勾选）
  4. **BYOK 本地优先**（fud-ai 式：自带 AI Key，数据本地存储，无账号无云依赖）

---

## 2. MVP 范围（P0 必须 / P1 后续 / 明确不做）

### P0（MVP 必须）
| # | 功能 | 说明 |
|---|------|------|
| F1 | Onboarding 引导 | 性别/年龄/身高/体重/目标（减脂/增肌/维持）/活动水平 → 计算 BMR/TDEE/热量预算 |
| F2 | 首页 Today | 热量环（已摄入/预算）、宏量（蛋白质/碳水/脂肪）、今日餐次列表、快捷入口 |
| F3 | 拍照记录 | 拍照/相册选图 → AI 识别（热量+三大宏量）→ 用户确认/修正份量 → 选择餐次(早/午/晚/加餐) → 保存 |
| F4 | 手动记录 | 文字搜索食物库 or 手动输入热量/宏量（AI 不可用时兜底） |
| F5 | 今日吃什么 | 基于剩余预算+口味偏好生成 3 个菜谱候选，菜谱详情（用料/步骤/营养），可加入购物清单 |
| F6 | 购物清单 | 由所选菜谱自动合并食材清单，条目可勾选/删除，可清空 |
| F7 | 历史/日报 | 按日查看历史餐次记录与热量汇总 |
| F8 | 设置 | 目标调整、AI Provider 配置（BYOK）、单位（公制/英制）、主题、数据导出 |
| F9 | 本地存储 | 所有记录本地持久化（Room），无账号 |

### P1（后续迭代，MVP 不做）
- AI 教练对话（多轮问答）
- 体重/体脂趋势图
- 间歇性断食计时器
- 习惯提醒（按时记录）
- 团队挑战/社交
- Apple Health / Health Connect 同步
- 条码扫描（Open Food Facts）
- 语音输入

### 明确不做（v0.1）
- 账号体系 / 云同步 / 社交 / 食物库搜索（食物库列入 P1，内置静态库 vs Open Food Facts 待选）
- 医疗/诊断类功能（纯生活方式管理，不做医疗建议）
- 多语言（v0.1 仅简体中文，架构上预留 i18n）
- 用户自定义菜谱编辑（AI 生成菜谱仅记录生成历史作缓存，见 RECIPE_LOG）

---

## 3. 信息架构（IA）

```
NutriMate (单 Activity + Navigation Compose, 底部 4 Tab)
├── Tab1 首页 Today
│   ├── 热量预算环 (已摄入 / 目标)
│   ├── 宏量营养素条 (蛋白质/碳水/脂肪)
│   ├── 今日餐次列表 (早餐/午餐/晚餐/加餐, 各条目可编辑删除)
│   ├── [记录一餐] FAB (核心入口)
│   └── [今日吃什么] 入口卡片
├── Tab2 记录一餐 (核心流程, 全屏页)
│   ├── Step1 输入方式选择: 拍照 / 相册 / 文字手动
│   ├── Step2 (拍照) AI 识别结果预览: 食物名 + 热量 + 宏量 + 置信度
│   │     ├── 确认 ✓
│   │     └── 修正: 手动改份量 / 重拍 / 改为手动输入
│   └── Step3 保存: 选餐次(早/午/晚/加餐) → 保存 → 返回首页刷新
├── Tab3 今日吃什么
│   ├── 生成条件展示 (剩余热量预算, 口味偏好)
│   ├── [生成推荐] 按钮 → 3 个菜谱卡片 (封面/名称/热量/蛋白)
│   ├── 菜谱详情: 用料列表 / 步骤 / 营养分解 / [加入购物清单]
│   └── 已加入状态提示
└── Tab4 购物清单
    ├── 条目列表 (食材名 / 数量 / 已勾选状态)
    ├── 勾选/取消 / 删除 / 清空
    └── 空态引导 (去生成菜谱)
辅助页 (无 Tab):
├── Onboarding (首次启动 3 步引导)
├── 历史日报 (按日查看, 从首页日期切换进入)
└── 设置 (从首页右上角齿轮进入)
```

### 页面→功能映射
| 页面 | 承载功能 |
|------|---------|
| Onboarding（3 屏向导） | F1 |
| Today | F2, F7（日期切换） |
| 拍照记录（全屏 3 步） | F3, F4 |
| 今日吃什么（列表+详情） | F5 |
| 购物清单 | F6 |
| 设置 | F8 |
| 本地数据库（Room） | F9 |

---

## 4. 核心页面流程图（Mermaid）

### 4.1 冷启动 → Onboarding → 首页
```mermaid
flowchart TD
    A[App 启动] --> B{首次启动?}
    B -- 是 --> C[Onboarding Step1: 基本信息\n性别/生日/身高/体重]
    C --> D[Onboarding Step2: 目标与活动\n减脂/增肌/维持 + 活动水平]
    D --> E[Onboarding Step3: 口味偏好\n兼容生成菜谱]
    E --> F[计算 BMR/TDEE/热量预算\n持久化 Profile]
    F --> G[首页 Today]
    B -- 否 --> G
    G --> H[读取本地数据\n渲染热量环/宏量/餐次]
```

### 4.2 核心闭环：记录一餐（拍照）
```mermaid
flowchart TD
    A[首页 FAB 记录一餐] --> B{选择输入方式}
    B -- 拍照 --> P1{相机权限?}
    P1 -- 已授予 --> C[CameraX 拍摄]
    P1 -- 未申请 --> P2[上下文内申请]
    P2 -- 授予 --> C
    P2 -- 拒绝/永久拒绝 --> P3[提示: 用相册或手动\n+ 去设置按钮]
    P3 -- 去设置 --> P4[系统设置]
    P3 -- 改用相册 --> D[Photo Picker 选图]
    P1 -- 拒绝 --> P3
    C --> E[AI 识别: 食物名/热量/蛋白质/碳水/脂肪\n+ 置信度 + 参考份量克数]
    D --> E
    B -- 相册 --> D
    B -- 手动 --> M[手动输入: 名称/热量/宏量/餐次]
    M --> H
    E --> F[识别结果预览页\n食物名可编辑+营养可编辑+置信度徽标]
    F --> G{需要修正?}
    G -- 否 --> H[确认份量倍率 + 餐次]
    G -- 是 --> I{修正方式}
    I -- 就地改名称/宏量 --> F
    I -- 改份量(倍率/克数) --> H
    I -- 重拍 --> P1
    I -- 改手动 --> M
    H --> S[保存到 Room\nFoodLogEntry source=AI_PHOTO]
    S --> T[刷新首页热量环/宏量/餐次列表]
    T --> U[提示: 今日已用 X 千卡\n还可吃 Y 千卡]
    U --> V[跳转 Tab3 今日吃什么]
    U -- 稍后 --> A0[返回首页]
```

### 4.3 今日吃什么 → 菜谱 → 购物清单
```mermaid
flowchart TD
    A[Tab3 今日吃什么] --> B[读取剩余预算+偏好+忌口+当前时间]
    B --> C{剩余预算 >= 150kcal?}
    C -- 否 --> D[空态: 今日已达标\n提示轻食/明日再看]
    C -- 是 --> E{今日生成次数 < 5?}
    E -- 否 --> E1[拦截: 提示今日次数已用完]
    E -- 是 --> F[AI 生成 3 个菜谱\n目标餐次+热量在预算60%-110%\n排除过敏原]
    F --> G[菜谱列表卡片 显示剩余次数]
    G --> H[点开菜谱详情]
    H --> I[查看用料/步骤/营养]
    I --> J{加入购物清单?}
    J -- 是 --> K[食材按基础名合并去重\nGroceryItem amount+unit]
    K --> L[Toast/角标: 已加入 N 项]
    J -- 否 --> H
    L --> M[Tab4 购物清单展示]
    M --> N[勾选/删除/清空]
```

### 4.4 历史日报
```mermaid
flowchart TD
    A[首页日期区 < 或 > 切换] --> B[按日查询 FoodLogEntry\n未来日期显示空态]
    B --> C[渲染该日热量/宏量/餐次]
    C --> D[点击单条记录]
    D --> E{操作}
    E -- 编辑 --> F[改份量倍率/餐次/数值\n倍率变更按参考克数重算营养]
    E -- 删除 --> G[确认删除软删除 → 移除]
    F --> C
    G --> C
```

### 4.5 AI 请求链路（BYOK, 离线兜底）
```mermaid
flowchart TD
    A[User 请求: 识别图片 / 生成菜谱] --> B[FoodAnalyzer / RecipeGenerator]
    B --> C{Provider 已配置?}
    C -- 否 --> D[提示: 请前往设置配置 AI Key\n或使用手动记录]
    C -- 是 --> E[调用 Provider 适配层\nGemini/OpenAI/自定义 OpenAI 兼容]
    E --> F{成功?}
    F -- 是 --> G{JSON 解析成功?}
    G -- 是 --> H[返回结构化结果]
    G -- 否 --> K{重试 1 次?}
    K -- 是 --> E
    K -- 否 --> I[降级: 手动输入 / 提示稍后重试]
    F -- 否(超时/限流) --> J{重试计数 < 2?}
    J -- 是 --> E
    J -- 否 --> I
    H --> Z[返回 UI]
    I --> Z
    D --> Z
```

---

## 5. 关键状态机

### FoodLogEntry（一条餐次记录）
```
DRAFT(识别中/预览) → CONFIRMED(已保存) → {EDITABLE, DELETABLE}
- DRAFT: AI 返回结果但用户未确认
- CONFIRMED: 用户确认保存, 计入当日热量
- 编辑: 份量/餐次变更 → 保持 CONFIRMED, 更新时间戳
- 删除: 软删除标记, 不参与当日统计
```

### 热量预算（每日）
```
budget(预算) = TDEE + 增肌盈余 / 减脂赤字 (由 Profile 目标决定)
剩余 = budget - Σ(当日已确认记录)
```

---

## 6. 数据模型（草案）

```mermaid
erDiagram
    PROFILE ||--o{ FOOD_LOG : "1..N"
    PROFILE {
        long id PK
        int gender        "MALE|FEMALE (v0.1 仅两档)"
        date birthday
        double height_cm
        double weight_kg
        string goal        "LOSE|MAINTAIN|GAIN"
        int activity_level "1..6"
        string taste_pref  "逗号分隔标签"
        string allergens   "逗号分隔忌口/过敏"
        int calories_budget
        int protein_gram
        int carb_gram
        int fat_gram
        long last_recalc_at
        long updated_at
    }
    FOOD_LOG {
        long id PK
        long profile_id FK
        string date         "LocalDate"
        string meal_type    "BREAKFAST|LUNCH|DINNER|SNACK"
        string food_name
        string source       "AI_PHOTO|MANUAL"
        string image_uri
        double calories
        double protein
        double carbs
        double fat
        double serving_multiplier  "份量倍率, 默认1.0"
        double reference_serving_g "AI参考份量克数"
        double confidence          "AI置信度 0..1"
        long created_at
        long updated_at
        boolean deleted    "软删除"
    }
    RECIPE_LOG {
        long id PK
        string date
        string name
        string target_meal "BREAKFAST|LUNCH|DINNER|SNACK"
        int calories
        int protein_gram
        string ingredients   "JSON 数组 {name,amount,unit}"
        string steps         "JSON 数组"
        long created_at
    }
    GROCERY_ITEM {
        long id PK
        string name         "基础食材名(不含份量)"
        double amount
        string unit
        boolean checked
        long created_at
    }
```

---

## 7. 技术架构（草案，OOP/分层/高内聚低耦合）

### 分层（Clean Architecture 风格）
```
app (Android 单模块 MVP 起步, 预留拆分多模块)
├── presentation/     (Compose UI + ViewModel + 状态流)
│   ├── onboarding/
│   ├── home/
│   ├── logmeal/
│   ├── recipe/
│   ├── grocery/
│   ├── history/
│   └── settings/
├── domain/           (纯 Kotlin: 实体 + 用例 + 仓库接口)
│   ├── model/
│   ├── repository/   (接口)
│   └── usecase/
├── data/             (Room + DataStore + 网络/Provider 适配)
│   ├── local/        (Room DAO + 实体映射)
│   ├── ai/           (Provider 接口 + Gemini/OpenAI/自定义实现)
│   └── repository/   (仓库实现)
└── di/               (Hilt 依赖注入)
```

### 架构原则
- **单向数据流**：UI → ViewModel(StateFlow) → UseCase → Repository → (Data/Local/AI)
- **依赖倒置**：domain 只定义接口，data 实现；presentation 依赖 domain 接口
- **模块内聚**：每个功能页 = 自己的 feature 包（UI+VM+状态），通过 DI 组装
- **AI Provider 可插拔**：`interface FoodAnalyzer` / `interface RecipeGenerator`，实现类按 Provider 注入
- **测试友好**：Usecase/Repository 纯逻辑可单测；AI 层用接口替身

### 关键依赖（建议版本基线，编码时以本机可构建版本验证）
| 依赖 | 版本 |
|------|------|
| Kotlin | 2.1.x（本机验证） |
| Jetpack Compose BOM | 2025.xx（本机验证） |
| Navigation Compose | 2.8.x |
| Room | 2.7.x |
| Hilt | 2.55.x |
| CameraX | 1.4.x |
| DataStore | 1.1.x |
| OkHttp / Retrofit | 4.12 / 2.11 |
| kotlinx.serialization | 1.7.x |
| AndroidX Core | 1.15.x |

---

## 8. MVP 验收标准（DoD）

1. 新用户走完 Onboarding 后能看到个性化热量预算与目标宏量。
2. 拍照（或相册选图）→ AI 识别 → 确认 → 保存 → 首页热量环/宏量/餐次实时更新的完整路径可用；无 AI Key 时手动记录可用。
3. 「今日吃什么」能基于剩余预算生成 3 个菜谱，菜谱详情可查看并加入购物清单。
4. 购物清单支持勾选/删除/清空；多菜谱食材自动合并去重。
5. 历史日报支持按日查看、编辑、删除记录。
6. 所有数据本地持久化，重启 App 不丢失；无账号体系。
7. 单元测试覆盖：热量预算计算、餐次状态机、购物清单合并去重、AI 响应解析。