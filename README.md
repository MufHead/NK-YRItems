# YRItems - 强大的物品库与脚本系统


**一个为 Nukkit MOT 设计的高度可扩展物品管理插件**

[![Nukkit](https://img.shields.io/badge/Nukkit-MOT-blue.svg)](https://motci.cn/)
[![License](https://img.shields.io/badge/License-AGPL--3.0-blue.svg)](LICENSE)
[![Non-Commercial](https://img.shields.io/badge/License-Non--Commercial-red.svg)](LICENSE)


---

## 📖 简介

YRItems 是一个**物品库插件**，专注于提供强大的物品配置、脚本系统和节点功能。它本身**不实现 RPG 属性计算**（如攻击力、暴击等），而是提供完整的基础设施，让你可以通过脚本和配置文件自由实现各种功能。

> **注意**：RPG 属性的实际效果（如战斗伤害计算、属性加成等）需要配合其他插件实现，例如 YRAttribute 插件（未开放）。

### ✨ 核心特性

- 🎯 **YAML 物品配置** - 通过配置文件定义物品属性、NBT 数据
- 🔧 **JavaScript 脚本系统** - 使用 Rhino 引擎，支持事件监听和自定义逻辑
- 📊 **节点系统** - 强大的随机属性生成框架（数值、权重、公式计算）
- 🎨 **动态 Lore 显示** - 根据 NBT 数据实时生成物品说明（客户端显示）
- 🔄 **热重载** - 无需重启服务器即可重载配置和脚本
- 🛠️ **高度可扩展** - 所有功能都可以通过配置和脚本自定义

---

## 🚀 快速开始

### 安装

1. 下载 `YRItems.jar`
2. 放入服务器的 `plugins/` 目录
3. 重启服务器
4. 插件将自动生成配置文件和示例脚本

### 基本命令

```
/yritems give <玩家> <物品ID> [数量]  - 给予玩家物品
/yritems reload                      - 重载配置和脚本
/yritems list                        - 列出所有已配置的物品
```

---

## 📦 物品配置系统

### 配置文件位置

```
YRItems/
  └── items/
      ├── weapons.yml      # 武器配置
      ├── armor.yml        # 防具配置
      └── custom.yml       # 自定义物品配置
```

### 物品配置示例

```yaml
# items/weapons.yml
legendary_sword:
  identifier: "minecraft:diamond_sword"  # 物品类型
  name: "§6传说之刃"                      # 显示名称
  lore: []                               # 留空让动态 Lore 生成

  # 自定义 NBT 数据
  nbt:
    YRAttributes:
      level: 2              # 强化等级 (int)
      Quality: "传说"       # 品质 (string)
      Damage: 150           # 基础攻击力
      CritRate: 35          # 暴击率
      Durability: 850       # 耐久度

  # 原版附魔
  enchantments:
    - id: 9   # 锋利
      level: 5
```

### 给予物品

```bash
/yritems give PlayerName legendary_sword 1
```

---

## 🔧 脚本系统

YRItems 使用 **Rhino JavaScript 引擎**，支持完整的事件监听和自定义逻辑。

### 脚本文件位置

```
YRItems/
  └── Scripts/
      ├── DynamicLore.js        # 内置：动态 Lore 显示
      ├── ExampleScript.js      # 示例脚本
      └── YourCustomScript.js   # 你的自定义脚本
```

### 事件监听示例

使用 `@Event` 注解注册事件监听器：

```javascript
/**
 * 监听玩家交互事件
 */
// @Event(eventName = "cn.nukkit.event.player.PlayerInteractEvent", priority="NORMAL")
function onPlayerInteract(event) {
    var player = event.getPlayer();
    var item = event.getItem();

    // 检查物品是否有自定义 NBT
    var nbt = item.getNamedTag();
    if (nbt.exist("YRAttributes")) {
        var attr = nbt.getCompound("YRAttributes");

        // 执行自定义逻辑
        player.sendMessage("§a你使用了特殊物品！");

        // 可以调用其他插件的 API
        // 例如：触发技能、计算伤害等
    }
}
```

### 访问插件 API

脚本中可以访问以下全局对象：

```javascript
plugin          // YRItems 插件实例
Server          // Nukkit Server 实例
print(msg)      // 输出日志到控制台

// Java 类访问
Packages.cn.nukkit.item.Item
Packages.cn.nukkit.Player
// ... 等等
```

### 热重载

修改脚本后，使用 `/yritems reload` 即可重载，无需重启服务器。

---

## 📊 节点系统（属性生成）

节点系统用于在物品配置中**生成随机属性值**，支持多种随机算法和公式计算。

### 支持的节点类型

| 节点类型 | 说明 | 用途示例 |
|---------|------|---------|
| `NUMBER` | 随机数值 | 生成攻击力：100-200 |
| `GAUSSIAN` | 高斯分布数值 | 更自然的属性分布 |
| `WEIGHT` | 权重随机选择 | 品质选择：普通(50%)、稀有(30%)、史诗(20%) |
| `STRINGS` | 随机字符串列表 | 随机后缀名称 |
| `CALCULATION` | 公式计算（JS引擎） | 基于其他节点计算复杂公式 |
| `FASTCALC` | 快速公式计算 | 简单数学运算 |
| `WEIGHTDECLARE` | 权重声明（多选） | 随机选择多个属性 |
| `JS` | JavaScript调用 | 完全自定义逻辑 |

### 节点配置示例

#### 示例1：随机数值属性

```yaml
legendary_sword:
  identifier: "minecraft:diamond_sword"
  name: "§6传说之刃"

  # 节点定义
  nodes:
    # 基础攻击力：100-200之间
    base_damage:
      type: NUMBER
      min: 100
      max: 200
      fixed: 0  # 整数

    # 暴击率：高斯分布，平均25%
    crit_rate:
      type: GAUSSIAN
      base: 25
      spread: 0.1      # 波动10%
      maxSpread: 0.3   # 最大波动30%
      fixed: 1         # 保留1位小数
```

#### 示例2：权重随机选择

```yaml
mystery_box:
  nodes:
    # 品质选择
    quality:
      type: WEIGHT
      values:
        - "50::普通"    # 50%概率
        - "30::稀有"    # 30%概率
        - "15::史诗"    # 15%概率
        - "5::传说"     # 5%概率
```

#### 示例3：公式计算

```yaml
enhanced_sword:
  nodes:
    base_damage:
      type: NUMBER
      min: 100
      max: 150

    # 最终伤害 = 基础伤害 * 1.5
    final_damage:
      type: CALCULATION
      formula: "<base_damage> * 1.5"
      fixed: 0
```

#### 示例4：JavaScript节点

```yaml
custom_item:
  nodes:
    special_value:
      type: JS
      path: "MyScript.js::calculateSpecialValue"
      args:
        - "100"
        - "200"
```

对应的JavaScript函数：

```javascript
function calculateSpecialValue(args) {
    var min = parseInt(args[0]);
    var max = parseInt(args[1]);

    // 自定义计算逻辑
    var value = min + Math.random() * (max - min);

    return value;
}
```

### 节点结果应用

节点计算的结果可以应用到物品NBT中：

```yaml
legendary_sword:
  nodes:
    damage_value:
      type: NUMBER
      min: 100
      max: 200

  # 将节点结果应用到NBT
  nbt:
    YRAttributes:
      Damage: "<damage_value>"  # 引用节点结果
```

---

## 🎨 动态 Lore 系统

动态 Lore 是 YRItems 自带的强大功能，可以根据物品的 NBT 数据**实时生成**物品说明。

### 工作原理

1. 读取物品的 `YRAttributes` NBT 数据
2. 根据 `configs/display_rules.yml` 中的规则生成 Lore
3. 仅在**客户端显示**，不修改物品实际 NBT
4. 支持热重载配置

### 显示规则配置

**配置文件位置**：`YRItems/configs/display_rules.yml`

```yaml
# ==================== 强化等级显示 ====================
enhance_level:
  nbt_key: "level"  # 读取的 NBT 键名

  # 值映射：不同等级显示不同文本
  value_mappings:
    '0':
      level_text: "§f强化等级 §71§f"
      description: "§7初级强化"
    '1':
      level_text: "§f强化等级 §e2§f"
      description: "§e中级强化"
    '2':
      level_text: "§f强化等级 §63§f"
      description: "§6高级强化"

# ==================== 品质显示 ====================
quality:
  nbt_key: "Quality"

  value_mappings:
    "普通":
      display: "§f品质: 普通"
    "稀有":
      display: "§9品质: 稀有"
    "史诗":
      display: "§5品质: 史诗"
    "传说":
      display: "§6品质: 传说"

# ==================== 数值属性显示 ====================
damage:
  nbt_key: "Damage"
  display_format: "§c⚔ 攻击力: §f{value}"

crit_rate:
  nbt_key: "CritRate"
  display_format: "§e☆ 暴击率: §f{value}%"

# ==================== 范围映射（不同颜色）====================
durability:
  nbt_key: "Durability"
  display_format: "耐久: {value}/1000"

  value_ranges:
    - min: 0
      max: 300
      color: "§c"  # 红色（低耐久）
    - min: 301
      max: 700
      color: "§e"  # 黄色（中等）
    - min: 701
      max: 1000
      color: "§a"  # 绿色（高耐久）
```

### 显示效果

物品 NBT：
```yaml
YRAttributes:
  level: 2
  Quality: "传说"
  Damage: 150
  CritRate: 35
  Durability: 850
```

游戏内显示：
```
§8§m--------------------
§f强化等级 §63§f
§6高级强化
§6品质: 传说
§c⚔ 攻击力: §f150
§e☆ 暴击率: §f35%
§a耐久: 850/1000
§8§m--------------------
```

### 占位符

在 `display_format` 中可使用：

| 占位符 | 说明 | 示例 |
|-------|------|------|
| `{value}` | NBT 原始值 | `level=2` → `"2"` |
| `{value_plus_1}` | NBT 值 +1 | `level=2` → `"3"` |

### 重载配置

```bash
/yritems reload
```

修改 `display_rules.yml` 后重载即可生效，无需重启服务器。

---

## ⚙️ 配置文件功能

YRItems 支持多配置文件组织，所有配置都会在启动时自动加载。

### 目录结构

```
YRItems/
  ├── config.yml              # 主配置文件
  ├── items/                  # 物品配置目录
  │   ├── weapons.yml
  │   ├── armor.yml
  │   └── consumables.yml
  ├── configs/                # 自定义配置目录
  │   ├── display_rules.yml  # 动态 Lore 规则
  │   ├── custom_config.yml  # 你的自定义配置
  │   └── ...
  └── Scripts/                # 脚本目录
      ├── DynamicLore.js
      └── ...
```

### 脚本中访问配置

```javascript
// 获取自定义配置文件
var configManager = plugin.getCustomConfigManager();
var myConfig = configManager.getConfig("custom_config.yml");

// 读取配置值
var value = myConfig.get("some_key");
```

---

## 🔄 重要说明

### 关于 RPG 属性实现

YRItems 是一个**物品库插件**，它提供：

✅ 物品配置和管理
✅ NBT 数据存储
✅ 动态 Lore 显示
✅ 脚本和节点系统

**但不提供**：

❌ 战斗伤害计算
❌ 属性加成效果
❌ 技能系统
❌ RPG 游戏逻辑

这些功能需要配合其他插件实现，例如：

- **YRAttribute**（未开放）- 实现属性计算和战斗系统
- 或者通过 YRItems 的脚本系统自行实现

### 工作流程示例

1. **YRItems** 定义物品，存储 NBT 数据（`Damage: 150`, `CritRate: 35`）
2. **YRItems** 通过动态 Lore 显示这些属性给玩家看
3. **YRAttribute**（或你的自定义插件）读取这些 NBT 数据
4. **YRAttribute** 在战斗时计算实际伤害、暴击等效果

---

## 📚 高级用法

### 创建自定义显示规则

1. 在物品中添加自定义 NBT：
```yaml
magic_staff:
  nbt:
    YRAttributes:
      MagicPower: 500
      ManaRegen: 10
```

2. 在 `configs/display_rules.yml` 中定义规则：
```yaml
magic_power:
  nbt_key: "MagicPower"
  display_format: "§b✦ 魔法强度: §f{value}"

mana_regen:
  nbt_key: "ManaRegen"
  display_format: "§9◈ 法力回复: §f{value}/秒"
```

3. 重载插件：`/yritems reload`

### 编写自定义脚本

1. 在 `Scripts/` 目录创建 `MyScript.js`
2. 使用 `@Event` 注解监听事件
3. 实现你的自定义逻辑
4. 重载插件生效

### 使用节点系统生成随机属性

通过节点系统可以为物品生成多样化的属性：

- 随机数值范围（攻击力、防御力）
- 高斯分布（更自然的属性波动）
- 权重随机（品质、类型选择）
- 公式计算（基于其他属性的复杂计算）
- JavaScript调用（完全自定义的随机逻辑）

---

## 🤝 支持与反馈

如有问题或建议，欢迎联系作者：

- **QQ**: 1244894362
- **GitHub**: 提交 Issue

---

## 📄 许可证

本项目采用 **GNU AGPL v3** 许可证，附加非商用条款。

### 简单说明

- ✅ **可以**：学习、修改、分发、私人使用
- ✅ **必须**：开源你的修改版本（包括网络使用）
- ✅ **必须**：保留作者署名
- ❌ **禁止**：商业用途（需要单独授权）

### 详细条款

- 遵循 GNU AGPL v3 协议
- 任何修改版本**必须开源**
- **网络使用**也需公开源码（AGPL特性）
- **不可商用**（需要商业授权请联系作者）

查看完整许可证：[LICENSE](LICENSE)

商业授权咨询：QQ 1244894362

---

<div align="center">

**使用 YRItems 打造你的专属 RPG 服务器！**

</div>
