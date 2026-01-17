# 动态Lore系统使用文档

## 概述

动态Lore系统允许你根据物品的NBT数据动态生成Lore文本，而不需要在物品配置中硬编码Lore。这样，当物品的属性（NBT）发生变化时，Lore会自动更新以反映最新的属性值。

## 工作原理

1. **拦截数据包**: 系统拦截发送给客户端的`InventoryContentPacket`和`InventorySlotPacket`
2. **检查NBT**: 检查物品是否包含`YRAttributes`等必需的NBT标签
3. **调用脚本**: 调用JavaScript脚本（默认是`LoreRenderer.js::renderLore`）
4. **渲染Lore**: 脚本根据NBT数据生成Lore文本数组
5. **发送给客户端**: 修改后的物品数据包发送给客户端

## 快速开始

### 1. 创建带YRAttributes的物品

在`items/`文件夹中创建物品配置：

```yaml
传奇之剑:
  identifier: minecraft:diamond_sword
  name: "§6§l传奇之剑"
  # 不需要设置lore，会自动从NBT生成
  nbt:
    YRAttributes:
      Damage: "50-80"
      CritRate: 25
      CritDamage: 150
      AttackSpeed: 1.6
      Quality: "传说"
```

### 2. 配置Lore渲染脚本

编辑`Scripts/LoreRenderer.js`文件，定义如何将NBT转换为Lore：

```javascript
function renderLore(itemId, itemDamage, itemName, nbtDataJSON) {
    var nbtData = JSON.parse(nbtDataJSON);
    if (!nbtData.YRAttributes) return null;

    var attributes = nbtData.YRAttributes;
    var loreLines = [];

    // 显示伤害
    if (attributes.Damage) {
        loreLines.push("§c攻击力: §f" + attributes.Damage);
    }

    // 显示暴击率
    if (attributes.CritRate) {
        loreLines.push("§e暴击率: §f" + attributes.CritRate + "%");
    }

    return loreLines;
}
```

### 3. 给玩家物品

使用命令获取物品：

```
/yritems get 传奇之剑
```

当玩家查看物品时，会看到动态生成的Lore：
```
§c攻击力: §f50-80
§e暴击率: §f25%
```

## 配置文件说明

### lore_config.yml

```yaml
# 是否启用动态Lore系统
enabled: true

# Lore渲染脚本路径（格式: "文件名::函数名"）
lore_script_path: "LoreRenderer.js::renderLore"

# 调试模式
debug: false

# 过滤规则
filters:
  # 仅对这些物品ID生效（空=所有）
  item_ids: []

  # 排除这些物品ID
  excluded_item_ids: []

  # 必需的NBT键
  required_nbt_keys:
    - "YRAttributes"
```

## JavaScript脚本编写指南

### 函数签名

```javascript
function renderLore(itemId, itemDamage, itemName, nbtDataJSON)
```

**参数:**
- `itemId` (number): 物品ID
- `itemDamage` (number): 物品损伤值/元数据
- `itemName` (string): 物品名称
- `nbtDataJSON` (string): NBT数据的JSON字符串

**返回值:**
- `Array<string>`: Lore文本行数组
- `null`: 不修改Lore

### 示例1: 基础属性显示

```javascript
function renderLore(itemId, itemDamage, itemName, nbtDataJSON) {
    var nbtData = JSON.parse(nbtDataJSON);
    if (!nbtData.YRAttributes) return null;

    var attr = nbtData.YRAttributes;
    var lore = [];

    lore.push("§8§m--------------------");
    if (attr.Damage) lore.push("§c攻击力: §f" + attr.Damage);
    if (attr.Defense) lore.push("§9防御力: §f" + attr.Defense);
    if (attr.CritRate) lore.push("§e暴击率: §f" + attr.CritRate + "%");
    lore.push("§8§m--------------------");

    return lore;
}
```

### 示例2: 条件格式化

```javascript
function renderLore(itemId, itemDamage, itemName, nbtDataJSON) {
    var nbtData = JSON.parse(nbtDataJSON);
    if (!nbtData.YRAttributes) return null;

    var attr = nbtData.YRAttributes;
    var lore = [];

    // 根据品质显示不同颜色
    if (attr.Quality) {
        var color = getQualityColor(attr.Quality);
        lore.push(color + "品质: " + attr.Quality);
    }

    // 显示等级需求（红色表示不满足）
    if (attr.RequiredLevel) {
        lore.push("§c需要等级: §f" + attr.RequiredLevel);
    }

    return lore;
}

function getQualityColor(quality) {
    switch (quality.toLowerCase()) {
        case "传说": return "§6";
        case "史诗": return "§5";
        case "稀有": return "§9";
        case "优秀": return "§a";
        default: return "§f";
    }
}
```

### 示例3: 嵌套对象和数组

```javascript
function renderLore(itemId, itemDamage, itemName, nbtDataJSON) {
    var nbtData = JSON.parse(nbtDataJSON);
    if (!nbtData.YRAttributes) return null;

    var attr = nbtData.YRAttributes;
    var lore = [];

    // 显示附加属性（嵌套对象）
    if (attr.ExtraStats) {
        lore.push("§b§l附加属性:");
        for (var key in attr.ExtraStats) {
            lore.push("§b  " + key + ": §f" + attr.ExtraStats[key]);
        }
    }

    // 显示附魔列表（数组）
    if (attr.Enchantments && attr.Enchantments.length > 0) {
        lore.push("§d§l附魔:");
        for (var i = 0; i < attr.Enchantments.length; i++) {
            var ench = attr.Enchantments[i];
            lore.push("§d  " + ench.name + " " + romanNumeral(ench.level));
        }
    }

    return lore;
}
```

### 示例4: 根据物品类型使用不同逻辑

```javascript
function renderLore(itemId, itemDamage, itemName, nbtDataJSON) {
    // 武器类物品ID
    var weaponIds = [268, 272, 267, 276, 283]; // 木剑、石剑、铁剑、钻石剑、金剑

    // 防具类物品ID
    var armorIds = [298, 299, 300, 301]; // 皮革防具

    if (weaponIds.indexOf(itemId) !== -1) {
        return renderWeaponLore(itemId, itemDamage, itemName, nbtDataJSON);
    } else if (armorIds.indexOf(itemId) !== -1) {
        return renderArmorLore(itemId, itemDamage, itemName, nbtDataJSON);
    }

    // 默认处理
    return renderDefaultLore(itemId, itemDamage, itemName, nbtDataJSON);
}

function renderWeaponLore(itemId, itemDamage, itemName, nbtDataJSON) {
    var nbtData = JSON.parse(nbtDataJSON);
    if (!nbtData.YRAttributes) return null;

    var attr = nbtData.YRAttributes;
    var lore = ["§c§l⚔ 武器属性 ⚔", ""];

    if (attr.Damage) lore.push("§c攻击力: §f" + attr.Damage);
    if (attr.CritRate) lore.push("§e暴击率: §f" + attr.CritRate + "%");

    return lore;
}
```

## 实用技巧

### 1. 使用节点系统生成随机属性

```yaml
随机武器:
  identifier: minecraft:iron_sword
  name: "§e随机武器"
  sections:
    min-dmg:
      type: number
      min: 10
      max: 30
      fixed: 0

    max-dmg:
      type: number
      min: 40
      max: 80
      fixed: 0

    damage-str:
      type: calculation
      expression: "'<min-dmg>-<max-dmg>'"

  nbt:
    YRAttributes:
      Damage: "<damage-str>"
```

### 2. 在Lore中显示计算结果

```javascript
function renderLore(itemId, itemDamage, itemName, nbtDataJSON) {
    var nbtData = JSON.parse(nbtDataJSON);
    var attr = nbtData.YRAttributes;
    var lore = [];

    // 显示伤害范围
    if (attr.Damage) {
        lore.push("§c攻击力: §f" + attr.Damage);

        // 如果是范围格式 "10-30"，计算平均值
        if (attr.Damage.indexOf("-") !== -1) {
            var parts = attr.Damage.split("-");
            var min = parseFloat(parts[0]);
            var max = parseFloat(parts[1]);
            var avg = (min + max) / 2;
            lore.push("§7  (平均: " + avg.toFixed(1) + ")");
        }
    }

    return lore;
}
```

### 3. 多语言支持

```javascript
var lang = "zh_CN"; // 可以从配置读取

var translations = {
    "zh_CN": {
        "damage": "攻击力",
        "defense": "防御力",
        "crit_rate": "暴击率"
    },
    "en_US": {
        "damage": "Damage",
        "defense": "Defense",
        "crit_rate": "Crit Rate"
    }
};

function t(key) {
    return translations[lang][key] || key;
}

function renderLore(itemId, itemDamage, itemName, nbtDataJSON) {
    var nbtData = JSON.parse(nbtDataJSON);
    var attr = nbtData.YRAttributes;
    var lore = [];

    if (attr.Damage) lore.push("§c" + t("damage") + ": §f" + attr.Damage);
    if (attr.Defense) lore.push("§9" + t("defense") + ": §f" + attr.Defense);

    return lore;
}
```

## 命令

目前没有专用命令，但可以使用现有的YRItems命令：

```
/yritems get <物品ID>        # 获取物品
/yritems reload              # 重新加载配置（需要重新加载lore_config.yml）
```

## 性能优化建议

1. **避免复杂计算**: 在脚本中尽量避免循环和复杂计算
2. **提前返回**: 如果物品不需要动态Lore，尽早返回`null`
3. **使用过滤器**: 在`lore_config.yml`中配置`item_ids`只对特定物品生效
4. **禁用调试**: 生产环境关闭`debug`模式

## 故障排除

### Lore没有显示

1. 检查`lore_config.yml`中`enabled`是否为`true`
2. 检查物品NBT是否包含`YRAttributes`
3. 检查`Scripts/LoreRenderer.js`是否存在且无语法错误
4. 启用`debug: true`查看详细日志

### Lore显示不正确

1. 在脚本中使用`print()`输出NBT数据检查
2. 检查JSON解析是否正确
3. 确保返回的是字符串数组

### 性能问题

1. 减少需要处理的物品ID（使用`item_ids`过滤）
2. 简化脚本逻辑
3. 检查是否有死循环或递归

## 示例物品配置

详见 `items/dynamic_lore_example.yml` 文件。

## 进阶用法

### 自定义渲染函数

你可以在配置中指定不同的渲染函数：

```yaml
# lore_config.yml
lore_script_path: "CustomRenderer.js::myRenderFunction"
```

然后创建`Scripts/CustomRenderer.js`：

```javascript
function myRenderFunction(itemId, itemDamage, itemName, nbtDataJSON) {
    // 自定义逻辑
    return ["§a自定义Lore行1", "§b自定义Lore行2"];
}
```

## 结语

动态Lore系统为物品系统带来了极大的灵活性。你可以：
- ✅ 根据NBT动态显示属性
- ✅ 使用JavaScript编写复杂的渲染逻辑
- ✅ 实现类似RPG游戏的装备系统
- ✅ 无需修改插件代码即可自定义Lore格式

如有问题，请查看插件日志或启用调试模式。
