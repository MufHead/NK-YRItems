package com.yirankuma.yritems;

import cn.nukkit.plugin.PluginBase;
import com.yirankuma.yritems.command.YRItemsCommand;
import com.yirankuma.yritems.config.CustomConfigManager;

public class YRItems extends PluginBase {

    private static YRItems instance;
    private ItemConfig itemConfig;
    private CustomConfigManager customConfigManager;
    private com.yirankuma.yritems.script.ItemsScriptEngineManager scriptEngine;

    public static YRItems getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        // 保存默认配置
        saveDefaultConfig();

        // 注册数据包接收监听器（用于清理动态Lore标记 + 监听槽位切换）
        getServer().getPluginManager().registerEvents(
            new com.yirankuma.yritems.listener.PacketReceiveListener(this), this);

        // 初始化BinaryStreamHook
        com.yirankuma.yritems.hook.BinaryStreamHook.init(this);

        // 初始化配置
        this.itemConfig = new ItemConfig(this);

        // 初始化自定义配置管理器
        this.customConfigManager = new CustomConfigManager(this);
        this.customConfigManager.loadAllConfigs();

        // 初始化脚本引擎
        this.scriptEngine = new com.yirankuma.yritems.script.ItemsScriptEngineManager(this);

        // 自动加载所有脚本
        this.scriptEngine.loadAllScripts();

        // 注册命令
        this.getServer().getCommandMap().register("yritems", new YRItemsCommand(this));

        getLogger().info("YRItems插件已启用！");
    }

    @Override
    public void onDisable() {
        if (scriptEngine != null) scriptEngine.unload();
        getLogger().info("YRItems插件已禁用！");
    }

    public ItemConfig getItemConfig() {
        return itemConfig;
    }

    public CustomConfigManager getCustomConfigManager() {
        return customConfigManager;
    }

    public com.yirankuma.yritems.script.ItemsScriptEngineManager getScriptEngine() {
        return scriptEngine;
    }

    /**
     * 重载插件配置和脚本
     */
    public void reload() {
        getLogger().info("正在重载YRItems插件...");

        // 1. 重载主配置
        reloadConfig();

        // 2. 重载物品配置
        if (itemConfig != null) {
            itemConfig.reloadItems();
        }

        // 3. 重载自定义配置文件
        if (customConfigManager != null) {
            customConfigManager.reloadAllConfigs();
        }

        // 4. 卸载旧脚本
        if (scriptEngine != null) {
            scriptEngine.unload();
        }

        // 5. 重新初始化脚本引擎
        this.scriptEngine = new com.yirankuma.yritems.script.ItemsScriptEngineManager(this);
        this.scriptEngine.loadAllScripts();

        getLogger().info("YRItems插件重载完成！");
    }
}
