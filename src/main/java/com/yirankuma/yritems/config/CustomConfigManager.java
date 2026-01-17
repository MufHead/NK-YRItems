package com.yirankuma.yritems.config;

import cn.nukkit.utils.Config;
import com.yirankuma.yritems.YRItems;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义配置文件管理器
 * 负责加载和管理 configs/ 目录下的所有YAML配置文件
 */
public class CustomConfigManager {

    private final YRItems plugin;
    private final File configsDir;
    private final Map<String, Config> loadedConfigs = new HashMap<>();

    public CustomConfigManager(YRItems plugin) {
        this.plugin = plugin;
        this.configsDir = new File(plugin.getDataFolder(), "configs");
        ensureDirAndExamples();
    }

    /**
     * 确保配置目录存在并创建示例文件
     */
    private void ensureDirAndExamples() {
        if (!configsDir.exists()) {
            configsDir.mkdirs();
        }

        // 创建示例配置文件
        File exampleFile = new File(configsDir, "display_rules.yml");
        if (!exampleFile.exists()) {
            try {
                plugin.saveResource("configs/display_rules.yml", false);
            } catch (Exception e) {
                plugin.getLogger().warning("无法创建示例配置文件: " + e.getMessage());
            }
        }
    }

    /**
     * 加载所有配置文件
     */
    public void loadAllConfigs() {
        loadedConfigs.clear();

        File[] files = configsDir.listFiles((dir, name) ->
            name.endsWith(".yml") || name.endsWith(".yaml"));

        if (files != null) {
            for (File file : files) {
                try {
                    Config config = new Config(file, Config.YAML);
                    String fileName = file.getName();
                    loadedConfigs.put(fileName, config);
                    plugin.getLogger().info("已加载配置文件: " + fileName);
                } catch (Exception e) {
                    plugin.getLogger().warning("加载配置文件失败: " + file.getName() + " - " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        plugin.getLogger().info("共加载 " + loadedConfigs.size() + " 个配置文件");
    }

    /**
     * 重载所有配置文件
     */
    public void reloadAllConfigs() {
        plugin.getLogger().info("正在重载配置文件...");
        loadAllConfigs();
    }

    /**
     * 获取指定配置文件
     * @param fileName 文件名（如 "display_rules.yml"）
     * @return Config对象，如果不存在返回null
     */
    public Config getConfig(String fileName) {
        return loadedConfigs.get(fileName);
    }

    /**
     * 获取所有已加载的配置
     */
    public Map<String, Config> getAllConfigs() {
        return new HashMap<>(loadedConfigs);
    }

    /**
     * 获取配置目录
     */
    public File getConfigsDir() {
        return configsDir;
    }
}
