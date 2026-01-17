# 动态Lore扩展说明

## 概述

DynamicLore.js 脚本已经重构为高度可扩展的架构，你可以轻松添加新的属性显示规则。

## 属性计算规则配置

### 规则格式

```javascript
{
    displayName: "显示名称",           // 在Lore中显示的名称
    color: "§c",                      // 颜色代码
    baseKey: "Damage",                // 基础属性的NBT键名
    extraKeys: ["ExtraDamage", "BonusDamage"],  // 额外属性键名（可选）
    suffix: "",                       // 后缀，如 "%" 或 "/秒"
    formatter: function(val) {        // 可选的格式化函数
        return val.toFixed(2);
    }
}
```

### 如何添加新属性

编辑 `Scripts/DynamicLore.js`，在 `ATTRIBUTE_RULES` 数组中添加新规则：

```javascript
var ATTRIBUTE_RULES = [
    // ... 现有规则 ...

    // 添加新属性：吸血
    {
        displayName: "生命偷取",
        color: "§c",
        baseKey: "LifeSteal",
        extraKeys: ["ExtraLifeSteal"],
        suffix: "%"
    },

    // 添加新属性：护甲穿透
    {
        displayName: "护甲穿透",
        color: "§7",
        baseKey: "ArmorPenetration",
        extraKeys: ["ExtraArmorPenetration"],
        suffix: "%"
    }
];
```

### 属性累加示例

如果你的物品NBT是这样的：

```yaml
YRAttributes:
  Damage: 50           # 基础攻击力
  ExtraDamage: 10      # 额外攻击力
  BonusDamage: 5       # 加成攻击力
```

那么显示的攻击力会是：**50 + 10 + 5 = 65**

## 自定义格式化

### 示例1：保留小数

```javascript
{
    displayName: "攻击速度",
    color: "§a",
    baseKey: "AttackSpeed",
    extraKeys: ["ExtraAttackSpeed"],
    suffix: "",
    formatter: function(val) {
        return val.toFixed(2);  // 保留2位小数
    }
}
```

### 示例2：带符号显示

```javascript
{
    displayName: "移动速度",
    color: "§f",
    baseKey: "Speed",
    extraKeys: ["ExtraSpeed"],
    suffix: "%",
    formatter: function(val) {
        return (val > 0 ? "+" : "") + val;  // 正数前加 "+"
    }
}
```

### 示例3：范围值显示

```javascript
{
    displayName: "伤害",
    color: "§c",
    baseKey: "MinDamage",  // 最小伤害
    extraKeys: [],
    suffix: "",
    formatter: function(val) {
        // 假设 attr.MaxDamage 存储最大伤害
        // 注意：这需要在 calculateAttributeValue 中特殊处理
        return val + "-" + (val + 10);  // 示例：显示为 "50-60"
    }
}
```

## 完整的物品配置示例

```yaml
legendary_sword:
  identifier: "minecraft:diamond_sword"
  name: "§6屠龙之刃"
  lore: []  # 留空，由脚本动态生成
  nbt:
    YRAttributes:
      # 基础属性会自动累加额外属性
      Damage: 100
      ExtraDamage: 20        # 总攻击力: 120

      Defense: 50
      ExtraDefense: 10       # 总防御力: 60

      CritRate: 30
      ExtraCritRate: 10      # 总暴击率: 40%

      CritDamage: 200

      AttackSpeed: 1.8

      Health: 1000
      ExtraHealth: 200       # 总生命值: 1200

      # 其他属性
      Quality: "传说"
      RequiredLevel: 60
      Durability: 1000

      # 附魔
      Enchantments:
        - name: "锋利"
          level: 10
        - name: "火焰附加"
          level: 5

      # 自定义属性（会单独显示在"附加属性"区域）
      ExtraStats:
        生命回复: "+10/秒"
        魔法伤害: "+50"
```

## 显示效果

上面的配置会生成如下Lore：

```
§8§m--------------------
§c攻击力: §f120
§9防御力: §f60
§e暴击率: §f40%
§6暴击伤害: §f200%
§a攻击速度: §f1.80
§c生命值: §f1200
§7耐久度: §f1000
§6品质: 传说
§c需要等级: §f60

§b§l附加属性:
§b  生命回复: §f+10/秒
§b  魔法伤害: §f+50

§d§l附魔:
§d  锋利 X
§d  火焰附加 V
§8§m--------------------
```

## 热重载

修改 `DynamicLore.js` 后，使用以下命令重载脚本：

```
/yritems reload
```

**无需重启服务器！**

## 性能优化

脚本已内置以下优化：

1. **Lore缓存**：相同NBT的物品会复用已生成的Lore
2. **早期过滤**：非背包数据包立即跳过
3. **直接修改**：避免不必要的对象克隆

如果你的服务器在线人数较多，可以调整缓存策略：

```javascript
var USE_CACHE = true;   // 推荐开启
var DEBUG_MODE = false; // 生产环境关闭调试
```

## 代码清理总结

已删除的冗余代码：

- ❌ **PacketSendListener.java** - 功能与 DynamicLore.js 重复
- ❌ **ItemData.toItem() 中的标记添加** - 改为脚本处理

保留的核心代码：

- ✅ **PacketReceiveListener.java** - 清理 `_DynamicLore` 标记 + 槽位同步
- ✅ **BinaryStreamHook.java** - ASM注入，移除Lore
- ✅ **DynamicLore.js** - 完全脚本驱动的动态Lore生成

## 架构说明

```
物品创建 (ItemData.toItem)
    ↓
服务器端物品: YRAttributes + Lore (无标记)
    ↓
发送数据包 (DynamicLore.js)
    ↓
克隆物品 → 添加 _DynamicLore 标记 → 发送给客户端
    ↓
客户端显示: 带动态Lore的物品
    ↓
客户端操作 → 发回服务器
    ↓
ASM注入 (BinaryStreamHook)
    ↓
移除 _DynamicLore 和 Lore → MOT验证通过
```
