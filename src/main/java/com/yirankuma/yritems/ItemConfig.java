package com.yirankuma.yritems;

import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;

import java.io.File;
import java.util.*;
import com.yirankuma.yritems.node.NodeDefinition;
import com.yirankuma.yritems.node.NodeType;

public class ItemConfig {
    private final YRItems plugin;
    private final Map<String, ItemData> items = new HashMap<>();
    
    public ItemConfig(YRItems plugin) {
        this.plugin = plugin;
        loadItems();
    }
    
    public void loadItems() {
        items.clear();
        
        // 创建items文件夹
        File itemsFolder = new File(plugin.getDataFolder(), "items");
        if (!itemsFolder.exists()) {
            itemsFolder.mkdirs();
            plugin.getLogger().info("创建了items文件夹: " + itemsFolder.getPath());
        }

        // 使用saveResource从resources复制示例文件（每次启动都尝试，但不覆盖已存在的）
        try {
            plugin.saveResource("items/example.yml", false);
            plugin.getLogger().debug("已确保example.yml存在");
        } catch (Exception ignored) {}

        try {
            plugin.saveResource("items/dynamic_lore_example.yml", false);
            plugin.getLogger().debug("已确保dynamic_lore_example.yml存在");
        } catch (Exception ignored) {}
        
        // 读取所有yml文件
        File[] files = itemsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().info("items文件夹中没有找到yml配置文件");
            return;
        }
        
        for (File file : files) {
            try {
                loadItemsFromFile(file);
            } catch (Exception e) {
                plugin.getLogger().error("加载配置文件失败: " + file.getName(), e);
            }
        }
        
        plugin.getLogger().info("成功加载了 " + items.size() + " 个物品配置");
    }
    
    private void loadItemsFromFile(File file) {
        Config config = new Config(file, Config.YAML);
        
        for (String itemKey : config.getKeys(false)) {
            try {
                ConfigSection itemSection = config.getSection(itemKey);

                // 读取物品identifier
                String identifier = itemSection.getString("identifier", itemKey);
                
                // 读取物品名称
                String name = itemSection.getString("name", itemKey);
                
                // 读取lore
                List<String> lore = itemSection.getStringList("lore");
                
                // 读取nbt
                Map<String, Object> nbt = new HashMap<>();
                if (itemSection.exists("nbt")) {
                    ConfigSection nbtSection = itemSection.getSection("nbt");
                    nbt = parseNbtSection(nbtSection);
                }
                
                ItemData itemData = new ItemData(identifier, name, lore, nbt);

                // 读取sections（节点）
                if (itemSection.exists("sections")) {
                    Map<String, NodeDefinition> sections = parseSections(itemSection.getSection("sections"));
                    itemData.setSections(sections);
                }

                items.put(itemKey, itemData);
                
                plugin.getLogger().debug("加载物品: " + itemKey + " -> " + name);
                
            } catch (Exception e) {
                plugin.getLogger().error("加载物品配置失败: " + itemKey + " 在文件 " + file.getName(), e);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseNbtSection(ConfigSection nbtSection) {
        Map<String, Object> nbt = new HashMap<>();
        
        for (String key : nbtSection.getKeys(false)) {
            Object value = nbtSection.get(key);
            
            if (value instanceof ConfigSection) {
                // 递归处理嵌套的配置节
                nbt.put(key, parseNbtSection((ConfigSection) value));
            } else if (value instanceof List) {
                // 处理列表类型的NBT
                List<?> list = (List<?>) value;
                List<Object> nbtList = new ArrayList<>();
                
                for (Object item : list) {
                    if (item instanceof Map) {
                        // 如果列表项是Map，转换为普通Map
                        Map<String, Object> mapItem = new HashMap<>();
                        for (Map.Entry<?, ?> entry : ((Map<?, ?>) item).entrySet()) {
                            mapItem.put(String.valueOf(entry.getKey()), entry.getValue());
                        }
                        nbtList.add(mapItem);
                    } else {
                        nbtList.add(item);
                    }
                }
                nbt.put(key, nbtList);
            } else {
                // 直接存储基本类型
                nbt.put(key, value);
            }
        }
        
        return nbt;
    }
    
    public ItemData getItem(String key) {
        return items.get(key);
    }
    
    public Map<String, ItemData> getAllItems() {
        return new HashMap<>(items);
    }
    
    public boolean hasItem(String key) {
        return items.containsKey(key);
    }
    
    public Set<String> getItemKeys() {
        return items.keySet();
    }
    
    public void reloadItems() {
        loadItems();
    }

    private Map<String, NodeDefinition> parseSections(ConfigSection sectionsRoot) {
        Map<String, NodeDefinition> result = new HashMap<>();
        for (String id : sectionsRoot.getKeys(false)) {
            ConfigSection defSec = sectionsRoot.getSection(id);
            String typeStr = defSec.getString("type", "").toUpperCase();
            NodeType type;
            try {
                // map aliases
                switch (typeStr) {
                    case "STRINGS": type = NodeType.STRINGS; break;
                    case "NUMBER": type = NodeType.NUMBER; break;
                    case "CHANCE": type = NodeType.CHANCE; break;
                    case "GAUSSIAN": type = NodeType.GAUSSIAN; break;
                    case "CALCULATION": type = NodeType.CALCULATION; break;
                    case "FASTCALC": type = NodeType.FASTCALC; break;
                    case "WEIGHT": type = NodeType.WEIGHT; break;
                    case "WEIGHTDECLARE": type = NodeType.WEIGHTDECLARE; break;
                    case "JS": type = NodeType.JS; break;
                    default:
                        plugin.getLogger().warning("未知节点类型: " + typeStr + " @" + id);
                        continue;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("节点类型解析失败: " + typeStr + " @" + id);
                continue;
            }

            Map<String, Object> options = new HashMap<>();
            for (String k : defSec.getKeys(false)) {
                Object v = defSec.get(k);
                if (v instanceof ConfigSection) {
                    options.put(k, ((ConfigSection) v).getAll());
                } else {
                    options.put(k, v);
                }
            }
            NodeDefinition def = new NodeDefinition(id, type, options);
            result.put(id, def);
        }
        return result;
    }
}