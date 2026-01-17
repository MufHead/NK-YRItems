/**
 * 动态Lore脚本 - 配置驱动版
 *
 * 此脚本实现动态Lore功能：
 * - 从YAML配置文件读取显示规则
 * - 根据物品NBT动态生成Lore
 * - 完全由配置文件控制显示内容
 *
 * 配置文件位置: configs/display_rules.yml
 */

// ==================== 配置区 ====================

var DYNAMIC_LORE_ENABLED = true;  // 是否启用动态Lore
var DEBUG_MODE = true;             // 调试模式 - 启用以查看详细日志
var CONFIG_FILE = "display_rules.yml";  // 配置文件名

// 显示规则缓存（从YAML加载）
var displayRules = null;

// 已处理物品的缓存（基于物品hash，避免重复处理）
var processedItemsCache = {};

// ==================== 工具函数 ====================

/**
 * 生成物品的唯一标识（基于NBT内容）
 */
function getItemFingerprint(item) {
    try {
        var nbt = item.getNamedTag();
        if (!nbt || nbt.isEmpty()) {
            return item.getId() + ":" + item.getDamage();
        }
        // 使用Java的hashCode作为指纹
        return item.getId() + ":" + item.getDamage() + ":" + nbt.hashCode();
    } catch (e) {
        return item.getId() + ":" + item.getDamage() + ":" + Math.random();
    }
}

/**
 * 将NBT CompoundTag转换为JavaScript对象
 */
function nbtToObject(nbt) {
    if (!nbt) return {};

    var obj = {};
    var tags = nbt.getTags();
    var keys = tags.keySet().toArray();

    for (var i = 0; i < keys.length; i++) {
        var key = keys[i];
        var tag = tags.get(key);

        if (tag instanceof Packages.cn.nukkit.nbt.tag.CompoundTag) {
            obj[key] = nbtToObject(tag);
        } else if (tag instanceof Packages.cn.nukkit.nbt.tag.ListTag) {
            obj[key] = listTagToArray(tag);
        } else {
            obj[key] = tag.parseValue();
        }
    }

    return obj;
}

/**
 * 将ListTag转换为数组
 */
function listTagToArray(listTag) {
    var arr = [];
    for (var i = 0; i < listTag.size(); i++) {
        var element = listTag.get(i);
        if (element instanceof Packages.cn.nukkit.nbt.tag.CompoundTag) {
            arr.push(nbtToObject(element));
        } else {
            arr.push(element);
        }
    }
    return arr;
}

/**
 * 加载显示规则配置
 */
function loadDisplayRules() {
    try {
        var configManager = plugin.getCustomConfigManager();
        if (!configManager) {
            print("[ERROR] CustomConfigManager 未初始化");
            return null;
        }

        var config = configManager.getConfig(CONFIG_FILE);
        if (!config) {
            print("[ERROR] 未找到配置文件: " + CONFIG_FILE);
            return null;
        }

        // 将Nukkit Config转换为JS对象
        var rulesMap = config.getAll();
        var rules = {};

        var keys = rulesMap.keySet().toArray();
        for (var i = 0; i < keys.length; i++) {
            var key = keys[i];
            rules[key] = convertConfigValue(rulesMap.get(key));
        }

        print("[INFO] 已加载显示规则: " + keys.length + " 条");
        return rules;

    } catch (e) {
        print("[ERROR] 加载显示规则失败: " + e);
        return null;
    }
}

/**
 * 转换Config值为JS对象
 */
function convertConfigValue(value) {
    if (value === null) return null;

    // 处理Map类型
    if (value instanceof java.util.Map) {
        var obj = {};
        var map = value;
        var keys = map.keySet().toArray();
        for (var i = 0; i < keys.length; i++) {
            var key = keys[i];
            obj[String(key)] = convertConfigValue(map.get(key));
        }
        return obj;
    }

    // 处理List类型
    if (value instanceof java.util.List) {
        var arr = [];
        var list = value;
        for (var i = 0; i < list.size(); i++) {
            arr.push(convertConfigValue(list.get(i)));
        }
        return arr;
    }

    // 基本类型
    return value;
}

/**
 * 根据规则和NBT值生成显示文本
 * @param {Object} rule - 显示规则
 * @param {*} nbtValue - NBT值
 * @returns {Array} 显示文本数组
 */
function generateDisplayText(rule, nbtValue) {
    if (!rule || nbtValue === undefined || nbtValue === null) {
        return [];
    }

    var lines = [];

    // 1. 处理值映射（value_mappings）
    if (rule.value_mappings) {
        var mapping = rule.value_mappings[String(nbtValue)];
        if (mapping) {
            // 显示主文本
            if (mapping.display) {
                lines.push(mapping.display);
            } else if (mapping.level_text) {
                lines.push(mapping.level_text);
            }

            // 显示描述
            if (mapping.description) {
                lines.push(mapping.description);
            }

            return lines;
        }
    }

    // 2. 处理范围映射（value_ranges）
    if (rule.value_ranges) {
        var numValue = Number(nbtValue);
        for (var i = 0; i < rule.value_ranges.length; i++) {
            var range = rule.value_ranges[i];
            if (numValue >= range.min && numValue <= range.max) {
                var text = rule.display_format || "{value}";
                text = text.replace("{value}", nbtValue);
                if (range.color) {
                    text = range.color + text;
                }
                lines.push(text);
                return lines;
            }
        }
    }

    // 3. 处理格式化字符串（display_format）
    if (rule.display_format) {
        var text = String(rule.display_format);
        var valueStr = String(nbtValue);

        // 使用正则表达式全局替换
        text = text.replace(/{value}/g, valueStr);

        // 替换 {value_plus_1} (仅当值是数字时)
        var numValue = Number(nbtValue);
        if (!isNaN(numValue)) {
            text = text.replace(/{value_plus_1}/g, String(numValue + 1));
        }

        lines.push(text);
    }

    return lines;
}

// ==================== Lore渲染逻辑 ====================

/**
 * 渲染动态Lore
 * @param {Object} nbtData - NBT数据对象
 * @returns {Array} Lore行数组
 */
function renderDynamicLore(nbtData) {
    if (DEBUG_MODE) {
        print("[DynamicLore] renderDynamicLore 被调用");
    }

    if (!nbtData.YRAttributes) {
        if (DEBUG_MODE) {
            print("[DynamicLore] 物品没有 YRAttributes，跳过");
        }
        return null; // 没有YRAttributes，不修改Lore
    }

    if (DEBUG_MODE) {
        print("[DynamicLore] 找到 YRAttributes: " + JSON.stringify(nbtData.YRAttributes));
    }

    // 确保规则已加载
    if (!displayRules) {
        print("[DynamicLore] 首次加载显示规则...");
        displayRules = loadDisplayRules();
        if (!displayRules) {
            print("[DynamicLore] 无法加载显示规则！");
            return null;  // 无法加载规则
        }
    }

    var attr = nbtData.YRAttributes;
    var lore = [];

    // 添加分隔线
    lore.push("§8§m--------------------");

    // 遍历所有显示规则
    var ruleKeys = Object.keys(displayRules);
    if (DEBUG_MODE) {
        print("[DynamicLore] 显示规则数量: " + ruleKeys.length);
    }

    for (var i = 0; i < ruleKeys.length; i++) {
        var ruleKey = ruleKeys[i];
        var rule = displayRules[ruleKey];

        if (!rule || !rule.nbt_key) {
            if (DEBUG_MODE) {
                print("[DynamicLore] 规则 " + ruleKey + " 无效或缺少 nbt_key");
            }
            continue;
        }

        // 获取NBT值
        var nbtValue = attr[rule.nbt_key];
        if (nbtValue === undefined || nbtValue === null) {
            if (DEBUG_MODE) {
                print("[DynamicLore] NBT键 '" + rule.nbt_key + "' 不存在，跳过");
            }
            continue;
        }

        if (DEBUG_MODE) {
            print("[DynamicLore] 处理规则 '" + ruleKey + "', NBT键='" + rule.nbt_key + "', 值=" + nbtValue);
        }

        // 生成显示文本
        var textLines = generateDisplayText(rule, nbtValue);
        if (DEBUG_MODE) {
            print("[DynamicLore] 生成了 " + textLines.length + " 行文本");
        }
        for (var j = 0; j < textLines.length; j++) {
            lore.push(textLines[j]);
        }
    }

    // 显示附魔（如果有）
    if (attr.Enchantments && attr.Enchantments.length > 0) {
        lore.push("");
        lore.push("§d§l附魔:");
        for (var i = 0; i < attr.Enchantments.length; i++) {
            var ench = attr.Enchantments[i];
            lore.push("§d  " + ench.name + " " + romanNumeral(ench.level));
        }
    }

    // 添加分隔线
    lore.push("§8§m--------------------");

    return lore;
}

/**
 * 将数字转换为罗马数字
 */
function romanNumeral(num) {
    var roman = ["I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"];
    return num > 0 && num <= 10 ? roman[num - 1] : String(num);
}

/**
 * 处理物品，返回带动态Lore的克隆物品
 */
function processItem(item) {
    if (!item || item.getId() == 0) {
        if (DEBUG_MODE) {
            print("[DynamicLore] processItem: 物品为空或ID为0");
        }
        return null;
    }

    try {
        // 生成物品指纹
        var fingerprint = getItemFingerprint(item);

        // 检查缓存（避免重复处理导致槽位跳动）
        if (processedItemsCache[fingerprint]) {
            if (DEBUG_MODE) {
                print("[DynamicLore] processItem: 物品已处理过（缓存命中），跳过");
            }
            return null;
        }

        var nbt = item.getNamedTag();
        if (!nbt || nbt.isEmpty()) {
            if (DEBUG_MODE) {
                print("[DynamicLore] processItem: NBT为空");
            }
            return null;
        }

        var nbtData = nbtToObject(nbt);
        if (!nbtData.YRAttributes) {
            if (DEBUG_MODE) {
                print("[DynamicLore] processItem: 没有YRAttributes");
            }
            return null;
        }

        if (DEBUG_MODE) {
            print("[DynamicLore] processItem: 开始渲染Lore...");
        }

        // 渲染Lore
        var newLore = renderDynamicLore(nbtData);
        if (!newLore) {
            print("[DynamicLore] processItem: 渲染Lore失败！");
            return null;
        }

        if (DEBUG_MODE) {
            print("[DynamicLore] processItem: 成功渲染 " + newLore.length + " 行Lore");
            for (var i = 0; i < newLore.length; i++) {
                print("[DynamicLore]   Lore[" + i + "]: " + newLore[i]);
            }
        }

        // 克隆物品并设置Lore
        var cloned = item.clone();
        cloned.setLore(newLore);

        // 添加动态Lore标记
        var clonedNbt = cloned.getNamedTag();
        if (clonedNbt != null) {
            clonedNbt.putByte("_DynamicLore", 1);
            cloned.setNamedTag(clonedNbt);
        }

        if (DEBUG_MODE) {
            print("[DynamicLore] processItem: 成功处理物品");
        }

        // 添加到缓存（带过期时间，避免内存泄漏）
        processedItemsCache[fingerprint] = Date.now();

        // 清理过期缓存（超过5秒的认为已过期）
        var now = Date.now();
        var expireTime = 5000; // 5秒
        for (var key in processedItemsCache) {
            if (now - processedItemsCache[key] > expireTime) {
                delete processedItemsCache[key];
            }
        }

        return cloned;

    } catch (e) {
        print("[DynamicLore] 处理物品失败: " + e);
        if (DEBUG_MODE) {
            print("[DynamicLore] 错误堆栈: " + (e.stack || e));
        }
        return null;
    }
}

// ==================== 事件监听器 ====================

/**
 * 监听数据包发送事件
 */
// @Event(eventName = "cn.nukkit.event.server.DataPacketSendEvent", priority="HIGH")
function onDataPacketSend(event) {
    if (!DYNAMIC_LORE_ENABLED) {
        if (DEBUG_MODE) {
            print("[DynamicLore] 动态Lore已禁用");
        }
        return;
    }

    try {
        var packet = event.getPacket();

        // 只处理背包相关数据包
        if (!(packet instanceof Packages.cn.nukkit.network.protocol.InventoryContentPacket ||
              packet instanceof Packages.cn.nukkit.network.protocol.InventorySlotPacket)) {
            return;
        }

        if (DEBUG_MODE) {
            print("[DynamicLore] 捕获到数据包: " + packet.getClass().getSimpleName());
        }

        // 处理背包内容数据包
        if (packet instanceof Packages.cn.nukkit.network.protocol.InventoryContentPacket) {
            var items = packet.slots;
            if (items == null || items.length == 0) {
                if (DEBUG_MODE) {
                    print("[DynamicLore] 背包为空或没有物品");
                }
                return;
            }

            if (DEBUG_MODE) {
                print("[DynamicLore] 处理背包内容，共 " + items.length + " 个物品");
            }

            for (var i = 0; i < items.length; i++) {
                var originalItem = items[i];
                if (originalItem != null && originalItem.getId() != 0) {
                    var clonedItem = processItem(originalItem);
                    if (clonedItem != null) {
                        items[i] = clonedItem;
                        if (DEBUG_MODE) {
                            print("[DynamicLore] 已处理槽位 " + i);
                        }
                    }
                }
            }
        }
        // 处理单个物品槽位数据包
        else if (packet instanceof Packages.cn.nukkit.network.protocol.InventorySlotPacket) {
            if (DEBUG_MODE) {
                print("[DynamicLore] 处理单个槽位数据包");
            }
            var originalItem = packet.item;
            if (originalItem != null && originalItem.getId() != 0) {
                var clonedItem = processItem(originalItem);
                if (clonedItem != null) {
                    packet.item = clonedItem;
                    if (DEBUG_MODE) {
                        print("[DynamicLore] 已处理单个物品");
                    }
                }
            }
        }

    } catch (e) {
        print("[DynamicLore] 数据包处理失败: " + e);
        if (DEBUG_MODE) {
            print("[DynamicLore] 错误堆栈: " + (e.stack || e));
        }
    }
}

// ==================== 初始化 ====================

if (typeof SCRIPT_INITIALIZED === 'undefined') {
    SCRIPT_INITIALIZED = true;
    print("动态Lore脚本已加载 - 状态: " + (DYNAMIC_LORE_ENABLED ? "启用" : "禁用"));
    print("配置文件: configs/" + CONFIG_FILE);

    // 立即加载显示规则
    displayRules = loadDisplayRules();
}
