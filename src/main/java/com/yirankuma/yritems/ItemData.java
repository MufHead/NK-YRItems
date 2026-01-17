package com.yirankuma.yritems;

import cn.nukkit.item.Item;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.nbt.tag.Tag;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.yirankuma.yritems.node.NodeDefinition;
import com.yirankuma.yritems.node.NodeEvaluator;
import com.yirankuma.yritems.node.NodeResult;

public class ItemData {
    private String identifier;
    private String name;
    private List<String> lore;
    private Map<String, Object> nbt;
    private Map<String, NodeDefinition> sections;
    private boolean useDynamicLore; // 是否启用动态Lore

    public ItemData(String identifier, String name, List<String> lore, Map<String, Object> nbt) {
        this.identifier = identifier;
        this.name = name;
        this.lore = lore;
        this.nbt = nbt;
        this.sections = new HashMap<>();
        this.useDynamicLore = false; // 默认不启用
    }
    
    /**
     * 从Item对象创建ItemData
     * @param item Nukkit的Item对象
     */
    public ItemData(Item item) {
        // 使用 getId() 和 getDamage() 来构造标识符
        this.identifier = "minecraft:" + item.getId() + (item.getDamage() != 0 ? ":" + item.getDamage() : "");
        this.name = item.hasCustomName() ? item.getCustomName() : null;
        this.lore = item.getLore() != null ? Arrays.asList(item.getLore()) : new ArrayList<>();
        this.nbt = extractNbtFromItem(item);
    }
    
    /**
     * 从Item对象创建ItemData的静态工厂方法
     * @param item Nukkit的Item对象
     * @return 创建的ItemData对象
     */
    public static ItemData fromItem(Item item) {
        return new ItemData(item);
    }
    
    public String getIdentifier() {
        return identifier;
    }
    
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public List<String> getLore() {
        return lore;
    }
    
    public void setLore(List<String> lore) {
        this.lore = lore;
    }
    
    public Map<String, Object> getNbt() {
        return nbt;
    }
    
    public void setNbt(Map<String, Object> nbt) {
        this.nbt = nbt;
    }

    public Map<String, NodeDefinition> getSections() {
        return sections;
    }

    public void setSections(Map<String, NodeDefinition> sections) {
        this.sections = sections != null ? sections : new HashMap<>();
    }

    public boolean isUseDynamicLore() {
        return useDynamicLore;
    }

    public void setUseDynamicLore(boolean useDynamicLore) {
        this.useDynamicLore = useDynamicLore;
    }

    /**
     * 将ItemData转换为Nukkit的Item对象
     * @return 创建的Item对象
     */
    public Item toItem() {
        return toItem(1);
    }
    
    /**
     * 将ItemData转换为Nukkit的Item对象
     * @param count 物品数量
     * @return 创建的Item对象
     */
    public Item toItem(int count) {
        Item item;
        
        // 根据identifier创建物品
        if (this.identifier != null && !this.identifier.isEmpty()) {
            try {
                // 解析identifier，例如 "minecraft:diamond" 或 "diamond"
                String[] parts = this.identifier.split(":");
                String itemName = parts.length > 1 ? parts[1] : parts[0];
                
                // 尝试通过名称获取物品ID
                item = Item.fromString(itemName);
                if (item.getId() == 0) {
                    // 如果无法识别，默认使用钻石
                    item = Item.get(Item.DIAMOND, 0, count);
                } else {
                    item.setCount(count);
                }
            } catch (Exception e) {
                // 解析失败时使用默认物品
                item = Item.get(Item.DIAMOND, 0, count);
            }
        } else {
            // 没有identifier时使用默认物品
            item = Item.get(Item.DIAMOND, 0, count);
        }
        
        // 节点计算与占位符替换
        Map<String, NodeResult> nodeResults = new NodeEvaluator().evaluateAll(this.sections);

        // 设置物品名称（带节点解析）
        if (this.name != null) {
            String resolvedName = resolvePlaceholders(this.name, nodeResults);
            item.setCustomName(resolvedName);
        }
        
        // 设置lore
        if (this.lore != null && !this.lore.isEmpty()) {
            List<String> resolvedLore = new ArrayList<>();
            for (String line : this.lore) {
                resolvedLore.add(resolvePlaceholders(line, nodeResults));
            }
            String[] loreArray = resolvedLore.toArray(new String[0]);
            item.setLore(loreArray);
        }
        
        // 设置NBT
        CompoundTag nbt = item.getNamedTag();
        if (nbt == null) {
            nbt = new CompoundTag();
        }

        if (this.nbt != null && !this.nbt.isEmpty()) {
            applyNbtToItem(nbt, this.nbt, nodeResults);
        }

        // 如果配置了使用动态Lore，添加标记
        if (this.useDynamicLore) {
            nbt.putByte("_DynamicLore", (byte) 1);
        }

        item.setNamedTag(nbt);

        return item;
    }

    private String resolvePlaceholders(String text, Map<String, NodeResult> ctx) {
        if (text == null || text.isEmpty() || ctx == null || ctx.isEmpty()) return text;
        String result = text;
        // 支持 <id> <id.0> <id.length> <id.else.0> <id.else.length>
        Pattern p = Pattern.compile("<([a-zA-Z0-9_-]+)(?:\\.([a-zA-Z0-9_]+))?(?:\\.([a-zA-Z0-9_]+))?>");
        Matcher m = p.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String id = m.group(1);
            String part1 = m.group(2);
            String part2 = m.group(3);
            NodeResult r = ctx.get(id);
            String replacement = m.group(0);
            if (r != null) {
                if (part1 == null) {
                    replacement = r.asString();
                } else if ("length".equalsIgnoreCase(part1)) {
                    replacement = String.valueOf(r.getLength());
                } else if ("else".equalsIgnoreCase(part1)) {
                    if (part2 == null || "length".equalsIgnoreCase(part2)) {
                        replacement = String.valueOf(r.getElseLength());
                    } else {
                        int idx = safeIndex(part2);
                        List<String> elseList = r.getElseList();
                        replacement = idx >= 0 && idx < elseList.size() ? elseList.get(idx) : "";
                    }
                } else {
                    int idx = safeIndex(part1);
                    List<String> list = r.getList();
                    replacement = idx >= 0 && idx < list.size() ? list.get(idx) : "";
                }
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private int safeIndex(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return -1; }
    }
    
    /**
     * 将NBT数据应用到物品的NBT标签中
     * @param nbt 物品的NBT标签
     * @param nbtData 要应用的NBT数据
     */
    @SuppressWarnings("unchecked")
    private void applyNbtToItem(CompoundTag nbt, Map<String, Object> nbtData, Map<String, NodeResult> nodeCtx) {
        for (Map.Entry<String, Object> entry : nbtData.entrySet()) {
            String key = entry.getKey();
            Object value = deepResolvePlaceholders(entry.getValue(), nodeCtx);
            
            // 处理带类型指定的NBT值
            if (value instanceof Map) {
                Map<String, Object> valueMap = (Map<String, Object>) value;
                if (valueMap.containsKey("type") && valueMap.containsKey("value")) {
                    // 格式: { type: "byte", value: 1 }
                    String type = (String) valueMap.get("type");
                    Object val = valueMap.get("value");
                    applyTypedNbtValue(nbt, key, type, val);
                } else {
                    // 嵌套的NBT
                    CompoundTag nestedTag = new CompoundTag();
                    applyNbtToItem(nestedTag, valueMap, nodeCtx);
                    nbt.putCompound(key, nestedTag);
                }
            } else if (value instanceof String) {
                String strValue = (String) value;
                if (strValue.contains(":")) {
                    // 格式: "type:value" 例如 "byte:1", "int:63"
                    String[] parts = strValue.split(":", 2);
                    if (parts.length == 2) {
                        String type = parts[0];
                        String val = parts[1];
                        applyTypedNbtValue(nbt, key, type, val);
                    } else {
                        nbt.putString(key, strValue);
                    }
                } else {
                    nbt.putString(key, strValue);
                }
            } else if (value instanceof List) {
                // 处理列表类型的NBT
                List<?> list = (List<?>) value;
                if (!list.isEmpty()) {
                    // 检查列表中是否包含复合标签（Map类型）
                    boolean hasCompoundTags = list.stream().anyMatch(item -> item instanceof Map);
                    
                    if (hasCompoundTags) {
                        // 复合标签列表（如附魔列表）
                        ListTag<CompoundTag> listTag = new ListTag<>(key);
                        for (Object item : list) {
                            if (item instanceof Map) {
                                CompoundTag itemTag = new CompoundTag();
                                applyNbtToItem(itemTag, (Map<String, Object>) item, nodeCtx);
                                listTag.add(itemTag);
                            }
                        }
                        nbt.putList(listTag);
                    } else {
                        // 混合类型列表，统一转换为字符串列表
                        ListTag<cn.nukkit.nbt.tag.StringTag> stringListTag = new ListTag<>(key);
                        for (Object item : list) {
                            String s = String.valueOf(item);
                            stringListTag.add(new cn.nukkit.nbt.tag.StringTag("", s));
                        }
                        nbt.putList(stringListTag);
                    }
                }
            } else {
                // 自动类型推断
                applyAutoTypedNbtValue(nbt, key, value);
            }
        }
    }

    // 深度解析：在写入 NBT 之前，递归替换 Map/List/String 中的占位符
    @SuppressWarnings("unchecked")
    private Object deepResolvePlaceholders(Object value, Map<String, NodeResult> nodeCtx) {
        if (value == null || nodeCtx == null || nodeCtx.isEmpty()) return value;
        if (value instanceof String) {
            return resolvePlaceholders((String) value, nodeCtx);
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            List<Object> resolved = new ArrayList<>(list.size());
            for (Object item : list) {
                resolved.add(deepResolvePlaceholders(item, nodeCtx));
            }
            return resolved;
        }
        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            // 若是 {type, value} 结构，仅解析 value；否则解析所有键值
            if (map.containsKey("type") && map.containsKey("value")) {
                Map<String, Object> copy = new HashMap<>(map);
                copy.put("value", deepResolvePlaceholders(copy.get("value"), nodeCtx));
                return copy;
            }
            Map<String, Object> res = new HashMap<>();
            for (Map.Entry<String, Object> e : map.entrySet()) {
                res.put(e.getKey(), deepResolvePlaceholders(e.getValue(), nodeCtx));
            }
            return res;
        }
        return value;
    }
    
    /**
     * 根据指定的类型应用NBT值
     */
    private void applyTypedNbtValue(CompoundTag nbt, String key, String type, Object value) {
        try {
            switch (type.toLowerCase()) {
                case "byte":
                    if (value instanceof Number) {
                        nbt.putByte(key, ((Number) value).byteValue());
                    } else {
                        nbt.putByte(key, Byte.parseByte(String.valueOf(value)));
                    }
                    break;
                case "short":
                    if (value instanceof Number) {
                        nbt.putShort(key, ((Number) value).shortValue());
                    } else {
                        nbt.putShort(key, Short.parseShort(String.valueOf(value)));
                    }
                    break;
                case "int":
                case "integer":
                    if (value instanceof Number) {
                        nbt.putInt(key, ((Number) value).intValue());
                    } else {
                        nbt.putInt(key, Integer.parseInt(String.valueOf(value)));
                    }
                    break;
                case "long":
                    if (value instanceof Number) {
                        nbt.putLong(key, ((Number) value).longValue());
                    } else {
                        nbt.putLong(key, Long.parseLong(String.valueOf(value)));
                    }
                    break;
                case "float":
                    if (value instanceof Number) {
                        nbt.putFloat(key, ((Number) value).floatValue());
                    } else {
                        nbt.putFloat(key, Float.parseFloat(String.valueOf(value)));
                    }
                    break;
                case "double":
                    if (value instanceof Number) {
                        nbt.putDouble(key, ((Number) value).doubleValue());
                    } else {
                        nbt.putDouble(key, Double.parseDouble(String.valueOf(value)));
                    }
                    break;
                case "string":
                    nbt.putString(key, String.valueOf(value));
                    break;
                case "boolean":
                case "bool":
                    if (value instanceof Boolean) {
                        nbt.putBoolean(key, (Boolean) value);
                    } else {
                        nbt.putBoolean(key, Boolean.parseBoolean(String.valueOf(value)));
                    }
                    break;
                default:
                    // 未知类型，默认为字符串
                    nbt.putString(key, String.valueOf(value));
                    break;
            }
        } catch (NumberFormatException e) {
            // 解析失败，使用字符串类型
            nbt.putString(key, String.valueOf(value));
        }
    }
    
    /**
     * 自动推断类型并应用NBT值（保持向后兼容）
     */
    private void applyAutoTypedNbtValue(CompoundTag nbt, String key, Object value) {
        if (value instanceof Boolean) {
            nbt.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            nbt.putInt(key, (Integer) value);
        } else if (value instanceof Long) {
            nbt.putLong(key, (Long) value);
        } else if (value instanceof Float) {
            nbt.putFloat(key, (Float) value);
        } else if (value instanceof Double) {
            nbt.putDouble(key, (Double) value);
        } else {
            nbt.putString(key, String.valueOf(value));
        }
    }
    
    /**
     * 从Item对象中提取NBT数据
     * @param item 物品对象
     * @return 提取的NBT数据
     */
    private Map<String, Object> extractNbtFromItem(Item item) {
        Map<String, Object> nbtData = new HashMap<>();
        
        CompoundTag namedTag = item.getNamedTag();
        if (namedTag != null && !namedTag.isEmpty()) {
            nbtData = parseCompoundTag(namedTag);
        }
        
        return nbtData;
    }
    
    /**
     * 解析CompoundTag为Map
     * @param tag CompoundTag对象
     * @return 解析后的Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseCompoundTag(CompoundTag tag) {
        Map<String, Object> result = new HashMap<>();
        
        // 修正：使用getAllTags()返回Collection<Tag>，然后通过Tag的getName()获取键名
        Collection<Tag> allTags = tag.getAllTags();
        for (Tag nbtTag : allTags) {
            String key = nbtTag.getName();
            Object value = parseNbtTag(nbtTag);
            if (value != null && key != null) {
                result.put(key, value);
            }
        }
        
        return result;
    }
    
    /**
     * 解析NBT标签为Java对象
     * @param tag NBT标签
     * @return 解析后的对象
     */
    private Object parseNbtTag(Tag tag) {
        switch (tag.getId()) {
            case Tag.TAG_Byte:
                return tag.parseValue();
            case Tag.TAG_Short:
                return tag.parseValue();
            case Tag.TAG_Int:
                return tag.parseValue();
            case Tag.TAG_Long:
                return tag.parseValue();
            case Tag.TAG_Float:
                return tag.parseValue();
            case Tag.TAG_Double:
                return tag.parseValue();
            case Tag.TAG_String:
                return tag.parseValue();
            case Tag.TAG_Compound:
                return parseCompoundTag((CompoundTag) tag);
            case Tag.TAG_List:
                // 处理列表类型，这里简化处理
                return tag.parseValue().toString();
            default:
                return tag.parseValue();
        }
    }
}