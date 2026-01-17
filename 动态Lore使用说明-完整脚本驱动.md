# 动态Lore系统 - 完整脚本驱动版

## 🎯 核心升级

✅ **完全参考YRRoom实现** - 使用注解式事件系统
✅ **零Java硬编码** - 所有逻辑都在JavaScript脚本中
✅ **事件驱动架构** - 使用`@Event`注解注册监听器
✅ **可选功能** - 删除脚本文件即可禁用
✅ **纯发包实现** - 只修改数据包，物品本身没有Lore

---

## 📝 快速开始

### 1. 插件启动

插件首次启动会自动创建 `Scripts/DynamicLore.js`

### 2. 创建物品

```yaml
传奇之剑:
  identifier: minecraft:diamond_sword
  name: "§6§l传奇之剑"
  nbt:
    YRAttributes:
      Damage: "10-30"
      CritRate: 25
```

### 3. 获取物品

```
/yritems get 传奇之剑
```

### 4. 查看效果

客户端看到的Lore：
```
§8§m--------------------
§c攻击力: §f10-30
§e暴击率: §f25%
§8§m--------------------
```

---

## ⚙️ 配置选项

编辑 `Scripts/DynamicLore.js` 顶部的配置区：

```javascript
var DYNAMIC_LORE_ENABLED = true;  // 是否启用动态Lore
var DEBUG_MODE = false;            // 调试模式
var USE_CACHE = true;              // 是否使用缓存（推荐开启）
```

**配置说明：**
- `DYNAMIC_LORE_ENABLED`: 设为 `false` 临时禁用（但建议直接删除脚本）
- `DEBUG_MODE`: 启用后会在控制台输出详细日志
- `USE_CACHE`: 启用缓存以提升性能，相同NBT的物品会复用已生成的Lore

---

## ⚙️ 事件注解系统

### 注解格式

```javascript
// @Event(eventName = "事件类全名", priority="优先级", ignoreCancelled=true/false)
function 处理函数名(event) {
    // 处理逻辑
}
```

### 参数说明

- **eventName**: 事件类的完整路径（必填）
- **priority**: 事件优先级，可选值: `LOWEST`, `LOW`, `NORMAL`, `HIGH`, `HIGHEST`, `MONITOR` （默认: `NORMAL`）
- **ignoreCancelled**: 是否忽略已取消的事件（默认: `false`）

### DynamicLore.js 示例

```javascript
// @Event(eventName = "cn.nukkit.event.server.DataPacketSendEvent", priority="HIGH")
function onDataPacketSend(event) {
    var packet = event.getPacket();

    // 处理背包内容数据包
    if (packet instanceof Packages.cn.nukkit.network.protocol.InventoryContentPacket) {
        var items = packet.slots;
        for (var i = 0; i < items.length; i++) {
            var item = items[i];
            // 处理物品...
        }
    }
}
```

---

## 🔧 脚本可用API

### 全局对象

| 对象 | 说明 | 示例 |
|------|------|------|
| `plugin` | YRItems插件实例 | `plugin.getLogger().info("消息")` |
| `Server` | Nukkit服务器实例 | `Server.getInstance()` |
| `print()` | 日志输出函数 | `print("调试信息")` |
| `Packages` | Java包访问 | `Packages.cn.nukkit.item.Item` |

### 常用类快捷访问

| 变量 | 对应Java类 |
|------|-----------|
| `Item` | `cn.nukkit.item.Item` |
| `Player` | `cn.nukkit.Player` |
| `CompoundTag` | `cn.nukkit.nbt.tag.CompoundTag` |

---

## 📚 完整示例

### 示例1：基础事件监听

```javascript
// @Event(eventName = "cn.nukkit.event.player.PlayerJoinEvent")
function onPlayerJoin(event) {
    var player = event.getPlayer();
    print("玩家加入: " + player.getName());
}
```

### 示例2：监听玩家交互

```javascript
// @Event(eventName = "cn.nukkit.event.player.PlayerInteractEvent", ignoreCancelled=true)
function onPlayerInteract(event) {
    var player = event.getPlayer();
    var item = event.getItem();

    if (item.getId() == 276) {  // 钻石剑
        player.sendMessage("§a你手持钻石剑！");
    }
}
```

### 示例3：取消事件

```javascript
// @Event(eventName = "cn.nukkit.event.player.PlayerDropItemEvent", priority="HIGHEST")
function onPlayerDropItem(event) {
    var item = event.getItem();
    var nbt = item.getNamedTag();

    // 如果物品有YRAttributes，禁止丢弃
    if (nbt.contains("YRAttributes")) {
        event.setCancelled(true);
        event.getPlayer().sendMessage("§c此物品无法丢弃！");
    }
}
```

### 示例4：多个事件监听

```javascript
// @Event(eventName = "cn.nukkit.event.player.PlayerJoinEvent")
function welcomePlayer(event) {
    var player = event.getPlayer();
    player.sendMessage("§a欢迎来到服务器！");
}

// @Event(eventName = "cn.nukkit.event.player.PlayerQuitEvent")
function goodbyePlayer(event) {
    var player = event.getPlayer();
    print(player.getName() + " 离开了服务器");
}

// @Event(eventName = "cn.nukkit.event.server.DataPacketSendEvent", priority="HIGH")
function handlePackets(event) {
    // 动态Lore处理...
}
```

---

## 🛠️ 高级用法

### 访问Java类

```javascript
// 通过Packages访问任何Java类
var ItemClass = Packages.cn.nukkit.item.Item;
var diamond = new ItemClass(ItemClass.DIAMOND, 0, 1);

// 使用快捷访问
var player = event.getPlayer();
var item = new Item(Item.IRON_SWORD, 0, 1);
```

### NBT操作

```javascript
// 创建CompoundTag
var nbt = new CompoundTag();
nbt.putString("key", "value");
nbt.putInt("number", 123);

// 嵌套CompoundTag
var attr = new CompoundTag();
attr.putString("Damage", "10-30");
nbt.put("YRAttributes", attr);

// 设置到物品
item.setNamedTag(nbt);
```

### 调用其他脚本函数

```javascript
// 在同一脚本文件中可以直接调用其他函数
function helperFunction(value) {
    return value * 2;
}

// @Event(eventName = "cn.nukkit.event.player.PlayerJoinEvent")
function onJoin(event) {
    var result = helperFunction(10);
    print("结果: " + result);  // 输出: 结果: 20
}
```

---

## 🚫 如何禁用动态Lore

### 方法1：删除脚本（推荐）

```bash
删除 plugins/YRItems/Scripts/DynamicLore.js
```

### 方法2：临时禁用

编辑 `Scripts/DynamicLore.js`：

```javascript
var DYNAMIC_LORE_ENABLED = false;  // 改为false
```

### 方法3：注释掉事件注解

```javascript
// 注释掉注解，事件就不会被注册
// @Event(eventName = "cn.nukkit.event.server.DataPacketSendEvent", priority="HIGH")
function onDataPacketSend(event) {
    // 这个函数不会被调用
}
```

---

## 🎨 自定义Lore显示

编辑 `Scripts/DynamicLore.js` 中的 `renderDynamicLore` 函数：

```javascript
function renderDynamicLore(nbtData) {
    if (!nbtData.YRAttributes) return null;

    var attr = nbtData.YRAttributes;
    var lore = [];

    // 添加你的自定义属性
    if (attr.MyCustomAttribute) {
        lore.push("§b自定义: §f" + attr.MyCustomAttribute);
    }

    // 条件格式化
    if (attr.Damage) {
        var dmg = String(attr.Damage);
        if (dmg.indexOf("-") !== -1) {
            // 范围伤害
            var parts = dmg.split("-");
            var min = parseInt(parts[0]);
            var max = parseInt(parts[1]);
            var avg = (min + max) / 2;
            lore.push("§c攻击力: §f" + dmg + " §7(平均: " + avg.toFixed(1) + ")");
        } else {
            lore.push("§c攻击力: §f" + dmg);
        }
    }

    return lore;
}
```

---

## 📊 常见事件列表

| 事件类 | 说明 | 可取消 |
|--------|------|--------|
| `cn.nukkit.event.player.PlayerJoinEvent` | 玩家加入 | ❌ |
| `cn.nukkit.event.player.PlayerQuitEvent` | 玩家退出 | ❌ |
| `cn.nukkit.event.player.PlayerChatEvent` | 玩家聊天 | ✅ |
| `cn.nukkit.event.player.PlayerInteractEvent` | 玩家交互 | ✅ |
| `cn.nukkit.event.player.PlayerDropItemEvent` | 玩家丢物品 | ✅ |
| `cn.nukkit.event.player.PlayerPickupItemEvent` | 玩家捡物品 | ✅ |
| `cn.nukkit.event.inventory.InventoryClickEvent` | 点击背包 | ✅ |
| `cn.nukkit.event.server.DataPacketSendEvent` | 发送数据包 | ❌ |
| `cn.nukkit.event.entity.EntityDamageEvent` | 实体受伤 | ✅ |
| `cn.nukkit.event.block.BlockBreakEvent` | 破坏方块 | ✅ |

更多事件请查看Nukkit API文档。

---

## ❓ 常见问题

**Q: 如何查看脚本是否正确加载？**
A: 插件启动时会在控制台显示 `已加载脚本: DynamicLore.js` 和 `已注册事件: DataPacketSendEvent -> onDataPacketSend`

**Q: 事件没有被触发？**
A: 检查：
1. 注解格式是否正确（空格、引号）
2. 事件类名是否正确（完整路径）
3. 函数名是否在注解下一行

**Q: 如何调试脚本？**
A:
1. 设置 `DEBUG_MODE = true`
2. 使用 `print()` 输出调试信息
3. 查看控制台输出

**Q: Packages无法访问Java类？**
A: 确保类路径正确，例如：
```javascript
var Item = Packages.cn.nukkit.item.Item;  // 正确
var Item = Packages.Item;  // 错误
```

**Q: 性能影响如何？**
A: 已优化到最小。脚本包含以下性能优化：
1. **早期过滤**: 在函数开头立即过滤掉非背包数据包，避免不必要的类型检查
2. **Lore缓存**: 相同NBT的物品会复用已生成的Lore，避免重复计算
3. **直接修改**: 不使用clone()，直接修改物品Lore，避免对象创建开销
4. **脚本只执行一次**: 启动时执行脚本，事件触发时只调用函数，不重新执行脚本

如需进一步优化，可以：
1. 设置 `USE_CACHE = true`（默认已启用）
2. 减少DEBUG_MODE的使用
3. 只在必要的NBT属性上生成Lore

---

## 🌟 技术架构

### 工作流程

```
插件启动
    ↓
ItemsScriptEngineManager初始化
    ├─ 创建Rhino Context和Scope
    ├─ 暴露plugin、Server等全局对象
    ├─ 添加print()函数
    └─ 添加Java类访问（Packages、Item等）
    ↓
loadAllScripts()
    ├─ 遍历Scripts/*.js文件
    ├─ 读取脚本内容
    ├─ 执行脚本（evalScript）
    ├─ 提取@Event注解（extractEventInfo）
    └─ 注册事件监听器（registerEventHandlers）
        ├─ 解析事件类名
        ├─ 创建DynamicListener
        └─ 向PluginManager注册
    ↓
事件触发（如DataPacketSendEvent）
    ↓
DynamicListener.onEvent()
    ├─ 检查事件类型
    ├─ 检查ignoreCancelled
    ├─ 重新执行脚本（确保函数可用）
    └─ 调用JavaScript函数
        └─ onDataPacketSend(event)
```

### 关键实现

1. **注解提取** - 使用正则表达式匹配 `// @Event(...)`
2. **动态监听器** - 为每个注解创建独立的监听器实例
3. **脚本重新执行** - 每次事件触发时重新执行脚本，确保函数可用
4. **Java互操作** - 通过Rhino提供的Packages访问Java类

---

## 📁 文件结构

```
plugins/YRItems/
├── Scripts/
│   ├── ExampleScript.js          # 原有示例
│   └── DynamicLore.js             # 动态Lore脚本
└── items/
    └── *.yml                      # 物品配置
```

---

## ✨ 总结

这个系统完全参考了 **NK-YRRoom** 的脚本架构：

- ✅ 使用 `@Event` 注解注册事件
- ✅ 支持 `priority` 和 `ignoreCancelled`
- ✅ 自动提取注解并创建监听器
- ✅ 完全脚本驱动，零Java硬编码
- ✅ 可选功能，删除脚本即可禁用

**祝你使用愉快！** 🎉
