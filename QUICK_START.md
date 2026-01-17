# 动态Lore系统 - 快速开始

## 功能说明

这个系统实现了**基于NBT的动态Lore**，物品的Lore不再固定，而是根据NBT数据实时生成。

### 核心特性

✅ **数据包拦截** - 拦截`InventoryContentPacket`和`InventorySlotPacket`，在发包时动态修改Lore
✅ **JavaScript渲染** - 使用JS脚本自定义Lore生成规则
✅ **NBT驱动** - Lore内容完全由`YRAttributes`等NBT数据决定
✅ **实时更新** - NBT变化时，Lore自动更新（无需重新创建物品）
✅ **高度可配置** - 通过配置文件和脚本灵活控制

## 安装

1. 编译插件：
   ```bash
   cd e:\ServerPLUGINS\NK-YRItems
   .\gradlew.bat shadowJar
   ```

2. 编译后的JAR会自动输出到：`E:\ServerPLUGINS\网易NK服务器插件\YRItems.jar`

3. 将JAR放入服务器的`plugins`目录

4. 启动服务器

## 基础使用

### 1️⃣ 创建带动态Lore的物品

编辑 `plugins/YRItems/items/my_weapon.yml`：

```yaml
传奇之剑:
  identifier: minecraft:diamond_sword
  name: "§6§l传奇之剑"
  # 不需要写lore，会自动生成
  nbt:
    YRAttributes:
      Damage: "50-80"
      CritRate: 25
      AttackSpeed: 1.6
```

### 2️⃣ 自定义Lore显示规则

编辑 `plugins/YRItems/Scripts/LoreRenderer.js`：

```javascript
function renderLore(itemId, itemDamage, itemName, nbtDataJSON) {
    var nbtData = JSON.parse(nbtDataJSON);
    if (!nbtData.YRAttributes) return null;

    var attr = nbtData.YRAttributes;
    var lore = [];

    // 自定义显示规则
    if (attr.Damage) {
        lore.push("§c攻击力: §f" + attr.Damage);
    }
    if (attr.CritRate) {
        lore.push("§e暴击率: §f" + attr.CritRate + "%");
    }

    return lore;
}
```

### 3️⃣ 获取物品并测试

```
/yritems get 传奇之剑
```

**效果：**
- 物品显示名称：`§6§l传奇之剑`
- 动态生成的Lore：
  ```
  §c攻击力: §f50-80
  §e暴击率: §f25%
  ```

### 4️⃣ 测试NBT变化

如果你通过某种方式修改了物品的NBT（比如自定义插件），例如将`Damage`改为`60-90`，下次玩家查看物品时Lore会自动变成：
```
§c攻击力: §f60-90
§e暴击率: §f25%
```

## 配置文件

### lore_config.yml

```yaml
# 启用/禁用动态Lore
enabled: true

# 渲染脚本路径
lore_script_path: "LoreRenderer.js::renderLore"

# 调试模式（开发时建议开启）
debug: false

# 过滤规则
filters:
  # 仅对这些物品ID生效（空=全部）
  item_ids: []

  # 排除这些物品ID
  excluded_item_ids: []

  # 必须包含这些NBT键才会渲染
  required_nbt_keys:
    - "YRAttributes"
```

## 文件结构

```
plugins/YRItems/
├── items/                          # 物品配置文件夹
│   ├── example.yml                 # 原有示例
│   └── dynamic_lore_example.yml    # 动态Lore示例
├── Scripts/                        # JS脚本文件夹
│   ├── ExampleScript.js            # 原有示例
│   └── LoreRenderer.js             # Lore渲染脚本
└── lore_config.yml                 # Lore配置文件
```

## 实现原理

```
客户端请求物品数据
        ↓
服务器准备发送数据包（InventoryContentPacket/InventorySlotPacket）
        ↓
PacketInterceptor拦截数据包
        ↓
检查物品NBT是否包含YRAttributes
        ↓
DynamicLoreRenderer调用JS脚本
        ↓
LoreRenderer.js::renderLore生成Lore数组
        ↓
修改数据包中的物品Lore
        ↓
发送给客户端
```

## 实战示例

### 示例1：范围伤害显示

**物品配置：**
```yaml
狂战士之斧:
  identifier: minecraft:iron_axe
  name: "§c狂战士之斧"
  nbt:
    YRAttributes:
      MinDamage: 35
      MaxDamage: 75
      CritRate: 15
```

**脚本：**
```javascript
if (attr.MinDamage && attr.MaxDamage) {
    lore.push("§c攻击力: §f" + attr.MinDamage + " - " + attr.MaxDamage);
}
```

**效果：**
```
§c攻击力: §f35 - 75
§e暴击率: §f15%
```

### 示例2：品质颜色

**物品配置：**
```yaml
龙鳞护甲:
  identifier: minecraft:diamond_chestplate
  name: "§9龙鳞护甲"
  nbt:
    YRAttributes:
      Defense: 120
      Quality: "传说"
```

**脚本：**
```javascript
if (attr.Quality) {
    var color = attr.Quality === "传说" ? "§6" : "§f";
    lore.push(color + "品质: " + attr.Quality);
}
if (attr.Defense) {
    lore.push("§9防御力: §f" + attr.Defense);
}
```

### 示例3：结合节点系统

**物品配置：**
```yaml
随机属性武器:
  identifier: minecraft:iron_sword
  name: "§e随机武器"
  sections:
    min-dmg:
      type: number
      min: 10
      max: 30
    max-dmg:
      type: number
      min: 40
      max: 80
    dmg-range:
      type: calculation
      expression: "'<min-dmg>-<max-dmg>'"
  nbt:
    YRAttributes:
      Damage: "<dmg-range>"  # 每次创建都不同
```

每次使用`/yritems get 随机属性武器`获得的物品伤害都不同，Lore也会相应变化！

## 常见问题

**Q: Lore没有显示？**
A: 检查：
1. `lore_config.yml`中`enabled: true`
2. 物品NBT包含`YRAttributes`
3. `Scripts/LoreRenderer.js`存在且无语法错误

**Q: 如何调试？**
A:
1. 设置`debug: true`
2. 在JS中使用`print("调试信息")`输出到控制台

**Q: 性能影响？**
A:
- 每次发包都会调用脚本，但计算量很小
- 可以通过`item_ids`过滤减少处理量
- 建议脚本逻辑保持简单

**Q: 支持哪些颜色代码？**
A: 支持Minecraft标准颜色代码：
- `§0-§9` 数字颜色
- `§a-§f` 字母颜色
- `§l` 粗体、`§m` 删除线、`§n` 下划线等

## 进阶功能

详见 [DYNAMIC_LORE_README.md](DYNAMIC_LORE_README.md) 完整文档。

## 技术支持

- 插件版本：1.0-SNAPSHOT
- Nukkit版本：MOT平台
- JavaScript引擎：Rhino 1.7.14

---

**恭喜！** 你已经掌握了动态Lore系统的基本用法。现在可以创建像真正RPG游戏一样的自定义物品了！🎉
