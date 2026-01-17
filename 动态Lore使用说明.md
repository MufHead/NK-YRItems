# 动态Lore系统 - 完全脚本驱动版

## 🎯 核心特性

✅ **完全脚本驱动** - 所有逻辑都在JavaScript脚本中，没有硬编码的Java类
✅ **可选功能** - 删除脚本文件即可禁用，不影响插件其他功能
✅ **纯发包实现** - 只修改数据包，物品本身没有Lore
✅ **自动加载** - 插件启动时自动复制示例脚本
✅ **易于自定义** - 修改脚本即可改变Lore显示规则

---

## 📁 文件结构

```
plugins/YRItems/
├── Scripts/
│   ├── ExampleScript.js      # 原有示例脚本
│   └── DynamicLore.js         # 动态Lore脚本（可选）
└── items/
    └── *.yml                  # 物品配置文件
```

---

## 🚀 快速开始

### 1. 启用动态Lore

**插件首次启动时会自动创建 `Scripts/DynamicLore.js`**

脚本内容已包含完整的动态Lore逻辑，无需额外配置！

### 2. 创建带YRAttributes的物品

在 `items/` 文件夹创建物品配置：

```yaml
传奇之剑:
  identifier: minecraft:diamond_sword
  name: "§6§l传奇之剑"
  # 不需要写lore，会自动生成
  nbt:
    YRAttributes:
      Damage: "10-30"
      CritRate: 25
      CritDamage: 150
      Quality: "传说"
```

### 3. 获取物品

```
/yritems get 传奇之剑
```

### 4. 查看效果

玩家看到的Lore会显示：
```
§8§m--------------------
§c攻击力: §f10-30
§e暴击率: §f25%
§6暴击伤害: §f150%
§6品质: 传说
§8§m--------------------
```

---

## ⚙️ 配置选项

编辑 `Scripts/DynamicLore.js` 顶部的配置区：

```javascript
// ==================== 配置区 ====================

var DYNAMIC_LORE_ENABLED = true;  // 是否启用动态Lore
var DEBUG_MODE = false;            // 调试模式
```

**选项说明：**

- `DYNAMIC_LORE_ENABLED`: 设为 `false` 可临时禁用（但建议直接删除脚本）
- `DEBUG_MODE`: 启用后会在控制台输出详细日志

---

## 🔧 自定义Lore显示

编辑 `Scripts/DynamicLore.js` 中的 `renderDynamicLore` 函数：

### 示例1：添加新属性

```javascript
function renderDynamicLore(nbtData) {
    var attr = nbtData.YRAttributes;
    var lore = [];

    // 添加你的自定义属性
    if (attr.MyCustomAttribute) {
        lore.push("§b自定义: §f" + attr.MyCustomAttribute);
    }

    return lore;
}
```

### 示例2：条件格式化

```javascript
// 根据属性值显示不同颜色
if (attr.Damage) {
    var damageStr = String(attr.Damage);
    if (damageStr.indexOf("-") !== -1) {
        // 范围伤害：10-30
        var parts = damageStr.split("-");
        var min = parseInt(parts[0]);
        var max = parseInt(parts[1]);
        var avg = (min + max) / 2;

        lore.push("§c攻击力: §f" + damageStr);
        lore.push("§7  (平均: " + avg.toFixed(1) + ")");
    } else {
        lore.push("§c攻击力: §f" + attr.Damage);
    }
}
```

### 示例3：嵌套对象

```javascript
// 显示附加属性
if (attr.ExtraStats) {
    lore.push("§b§l附加属性:");
    for (var key in attr.ExtraStats) {
        lore.push("§b  " + key + ": §f" + attr.ExtraStats[key]);
    }
}
```

物品配置：
```yaml
my_armor:
  identifier: minecraft:diamond_chestplate
  name: "§9龙鳞护甲"
  nbt:
    YRAttributes:
      Defense: 120
      ExtraStats:
        "火焰抗性": "50%"
        "击退抗性": "80%"
```

---

## 🛑 如何禁用动态Lore

### 方法1：删除脚本文件（推荐）

```bash
删除 plugins/YRItems/Scripts/DynamicLore.js
```

重启服务器后，动态Lore功能将被禁用。

### 方法2：临时禁用

编辑 `Scripts/DynamicLore.js`：

```javascript
var DYNAMIC_LORE_ENABLED = false;  // 改为false
```

执行 `/yritems reload` 重新加载脚本。

---

## 📝 支持的NBT属性

脚本默认支持以下属性（可自行扩展）：

| NBT键 | 说明 | 示例值 | 显示效果 |
|-------|------|--------|----------|
| `Damage` | 攻击力 | `"10-30"` | §c攻击力: §f10-30 |
| `Defense` | 防御力 | `120` | §9防御力: §f120 |
| `CritRate` | 暴击率 | `25` | §e暴击率: §f25% |
| `CritDamage` | 暴击伤害 | `150` | §6暴击伤害: §f150% |
| `AttackSpeed` | 攻击速度 | `1.6` | §a攻击速度: §f1.6 |
| `Durability` | 耐久度 | `500` | §7耐久度: §f500 |
| `Quality` | 品质 | `"传说"` | §6品质: 传说 |
| `RequiredLevel` | 需要等级 | `50` | §c需要等级: §f50 |
| `ExtraStats` | 附加属性（对象） | `{...}` | 嵌套显示 |
| `Enchantments` | 附魔（数组） | `[...]` | 列表显示 |

---

## 🎨 完整示例

### 物品配置

```yaml
终极神剑:
  identifier: minecraft:diamond_sword
  name: "§6§l终极神剑"
  nbt:
    YRAttributes:
      Damage: "80-120"
      CritRate: 35
      CritDamage: 200
      AttackSpeed: 1.8
      Quality: "传说"
      RequiredLevel: 60
      ExtraStats:
        "生命偷取": "15%"
        "护甲穿透": "30%"
      Enchantments:
        - name: "锋利"
          level: 10
        - name: "耐久"
          level: 5
```

### 客户端显示效果

```
§8§m--------------------
§c攻击力: §f80-120
§e暴击率: §f35%
§6暴击伤害: §f200%
§a攻击速度: §f1.8
§6品质: 传说
§c需要等级: §f60

§b§l附加属性:
§b  生命偷取: §f15%
§b  护甲穿透: §f30%

§d§l附魔:
§d  锋利 X
§d  耐久 V
§8§m--------------------
```

---

## 🔬 技术原理

### 工作流程

```
客户端请求物品数据
    ↓
Nukkit准备发送InventoryContentPacket
    ↓
ScriptEventManager拦截数据包
    ↓
调用脚本函数: onInventoryContentPacket(item, playerName, slotIndex)
    ↓
脚本检查item.getNamedTag()是否包含YRAttributes
    ↓
脚本调用 renderDynamicLore() 生成Lore数组
    ↓
脚本克隆物品: cloned = item.clone()
    ↓
脚本设置Lore: cloned.setLore(newLore)
    ↓
返回克隆的物品给Java层
    ↓
Java层替换数据包中的物品
    ↓
发送给客户端（客户端看到动态Lore）
```

### 关键点

1. **完全脚本驱动**
   - Java层只负责拦截数据包和调用脚本
   - 所有Lore生成逻辑都在JS脚本中

2. **纯发包实现**
   - 使用 `item.clone()` 创建副本
   - 只修改数据包中的物品
   - 服务器内存中的原物品完全不变

3. **可选功能**
   - 脚本不存在时，事件处理器仍会运行
   - 但找不到处理函数，直接跳过
   - 不影响插件其他功能

---

## ❓ 常见问题

**Q: 如何完全禁用动态Lore？**
A: 删除 `Scripts/DynamicLore.js` 文件即可。

**Q: 脚本修改后需要重启服务器吗？**
A: 执行 `/yritems reload` 即可重新加载脚本。

**Q: 如何调试脚本？**
A:
1. 设置 `DEBUG_MODE = true`
2. 在脚本中使用 `print("调试信息")`
3. 查看控制台输出

**Q: 物品本身有Lore吗？**
A: 没有！使用 `/yritems nbt` 查看物品NBT，你会发现没有Lore标签。

**Q: NBT变化后Lore会自动更新吗？**
A: 会！因为每次发包都重新生成Lore。

**Q: 性能影响如何？**
A: 很小。脚本计算非常快，且只在发包时执行。

**Q: 支持其他语言吗？**
A: 支持！在脚本中修改显示文本即可。

---

## 🌟 进阶用法

### 1. 根据物品ID使用不同Lore

```javascript
function renderDynamicLore(nbtData, itemId) {
    // 传递itemId参数
    if (itemId == 276) {  // 钻石剑
        return renderWeaponLore(nbtData);
    } else if (itemId == 311) {  // 钻石胸甲
        return renderArmorLore(nbtData);
    }
    return renderDefaultLore(nbtData);
}
```

### 2. 多语言支持

```javascript
var LANG = "zh_CN";  // 或 "en_US"

var translations = {
    "zh_CN": {
        "damage": "攻击力",
        "defense": "防御力"
    },
    "en_US": {
        "damage": "Damage",
        "defense": "Defense"
    }
};

function t(key) {
    return translations[LANG][key] || key;
}

lore.push("§c" + t("damage") + ": §f" + attr.Damage);
```

### 3. 条件显示

```javascript
// 只有满足条件才显示某些属性
if (attr.RequiredLevel && attr.RequiredLevel > 50) {
    lore.push("§c§l★ 高级装备 ★");
}
```

---

## 📚 相关文档

- **物品配置**: 查看 `items/dynamic_lore_example.yml`
- **脚本示例**: 查看 `Scripts/ExampleScript.js`
- **原有功能**: 查看项目根目录的其他文档

---

## ✨ 总结

这个动态Lore系统是**完全可选的脚本功能**：

- ✅ 不想用？删除脚本即可
- ✅ 想自定义？编辑脚本即可
- ✅ 不影响插件其他功能
- ✅ 纯JavaScript，易于修改

**祝你使用愉快！** 🎉
