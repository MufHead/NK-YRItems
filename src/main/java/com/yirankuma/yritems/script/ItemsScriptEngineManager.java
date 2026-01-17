package com.yirankuma.yritems.script;

import cn.nukkit.Server;
import cn.nukkit.event.Event;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.Listener;
import cn.nukkit.plugin.PluginManager;
import com.yirankuma.yritems.YRItems;
import org.mozilla.javascript.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 脚本引擎管理器
 * 参考YRRoom实现，支持通过注解注册事件监听器
 */
public class ItemsScriptEngineManager {
    private final YRItems plugin;
    private final File scriptsDir;
    private Context rhinoContext;
    private Scriptable scope;
    private final Map<String, String> loadedScripts = new HashMap<>();
    private final List<Listener> registeredListeners = new ArrayList<>();

    public ItemsScriptEngineManager(YRItems plugin) {
        this.plugin = plugin;
        this.scriptsDir = new File(plugin.getDataFolder(), "Scripts");
        ensureDirAndExample();
        initialize();
    }

    /**
     * 初始化脚本引擎
     */
    private void initialize() {
        try {
            rhinoContext = Context.enter();
            scope = rhinoContext.initStandardObjects();

            // 暴露基本对象
            ScriptableObject.putProperty(scope, "plugin", plugin);
            ScriptableObject.putProperty(scope, "Server", Server.getInstance());

            // 添加print函数
            ScriptableObject.putProperty(scope, "print", new BaseFunction() {
                @Override
                public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < args.length; i++) {
                        if (i > 0) sb.append(" ");
                        sb.append(Context.toString(args[i]));
                    }
                    Server.getInstance().getLogger().info("[YRItems脚本] " + sb);
                    return Undefined.instance;
                }
            });

            // 添加Java类访问工具
            addJavaClassUtils();

        } catch (Exception e) {
            plugin.getLogger().error("初始化脚本引擎失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 添加Java类访问工具
     */
    private void addJavaClassUtils() {
        // 添加Packages访问（标准方式）
        try {
            // 获取Packages对象（Rhino内置）
            Object packagesObj = rhinoContext.evaluateString(scope, "Packages", "packages", 1, null);
            if (packagesObj == null || packagesObj == Undefined.instance) {
                // 如果Packages不存在，手动初始化
                ScriptableObject.putProperty(scope, "Packages", new NativeJavaPackage("", rhinoContext.getApplicationClassLoader()));
            }
        } catch (Exception e) {
            plugin.getLogger().debug("Packages已自动初始化");
        }

        // 添加常用类的快捷访问
        try {
            ScriptableObject.putProperty(scope, "Item",
                new NativeJavaClass(scope, Class.forName("cn.nukkit.item.Item")));
            ScriptableObject.putProperty(scope, "Player",
                new NativeJavaClass(scope, Class.forName("cn.nukkit.Player")));
            ScriptableObject.putProperty(scope, "CompoundTag",
                new NativeJavaClass(scope, Class.forName("cn.nukkit.nbt.tag.CompoundTag")));
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("添加Java类快捷访问失败: " + e.getMessage());
        }
    }

    /**
     * 确保脚本目录存在并复制示例
     */
    private void ensureDirAndExample() {
        if (!scriptsDir.exists()) scriptsDir.mkdirs();
        try {
            plugin.saveResource("Scripts/ExampleScript.js", false);
            plugin.saveResource("Scripts/DynamicLore.js", false);
        } catch (Exception ignored) {}
    }

    /**
     * 加载所有脚本文件
     */
    public void loadAllScripts() {
        File[] files = scriptsDir.listFiles((dir, name) -> name.endsWith(".js"));
        if (files != null) {
            for (File file : files) {
                try {
                    String scriptContent = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                    String scriptPath = file.getPath();

                    // 执行脚本
                    evalScript(scriptContent);

                    // 保存脚本内容
                    loadedScripts.put(scriptPath, scriptContent);

                    // 注册事件处理器
                    registerEventHandlers(scriptContent, scriptPath);

                    plugin.getLogger().info("已加载脚本: " + file.getName());

                } catch (Exception e) {
                    plugin.getLogger().warning("加载脚本失败: " + file.getName() + " - " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 执行脚本字符串
     */
    public void evalScript(String scriptContent) {
        try {
            if (rhinoContext != null && scope != null) {
                rhinoContext.evaluateString(scope, scriptContent, "script", 1, null);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("执行脚本失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 执行JS表达式
     */
    public Object evalExpression(String jsExpr) {
        try {
            return rhinoContext.evaluateString(scope, jsExpr, "expr", 1, null);
        } catch (Exception e) {
            plugin.getLogger().warning("JS表达式执行失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 调用脚本路径指定的函数
     */
    public Object invokePath(String path, List<String> args) {
        try {
            String[] parts = path.split("::", 2);
            if (parts.length < 2) return null;

            String fileName = parts[0];
            String funcName = parts[1];

            // 确保脚本已加载
            File scriptFile = new File(scriptsDir, fileName);
            if (scriptFile.exists() && !loadedScripts.containsKey(scriptFile.getPath())) {
                String content = Files.readString(scriptFile.toPath(), StandardCharsets.UTF_8);
                evalScript(content);
                loadedScripts.put(scriptFile.getPath(), content);
            }

            Object fnObj = scope.get(funcName, scope);
            if (fnObj instanceof Function) {
                Function fn = (Function) fnObj;
                Object[] jsArgs = args == null ? new Object[0] : args.toArray();
                return fn.call(rhinoContext, scope, scope, jsArgs);
            } else {
                plugin.getLogger().warning("未找到函数: " + funcName + " 于脚本 " + fileName);
                return null;
            }
        } catch (Exception e) {
            plugin.getLogger().error("调用JS节点失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 注册事件处理器
     * 参考YRRoom的实现
     */
    private void registerEventHandlers(String scriptContent, String scriptPath) {
        try {
            // 提取事件信息
            List<EventInfo> eventInfos = extractEventInfo(scriptContent);

            for (EventInfo eventInfo : eventInfos) {
                try {
                    // 获取事件类
                    Class<?> eventClass = Class.forName(eventInfo.eventName);

                    if (!Event.class.isAssignableFrom(eventClass)) {
                        plugin.getLogger().warning("类 " + eventInfo.eventName + " 不是有效的事件类");
                        continue;
                    }

                    // 创建动态监听器
                    Listener dynamicListener = createDynamicListener(eventClass, eventInfo, scriptPath);

                    // 注册事件监听器
                    EventPriority priority = EventPriority.valueOf(eventInfo.priority.toUpperCase());
                    PluginManager pm = Server.getInstance().getPluginManager();

                    pm.registerEvent(
                        (Class<? extends Event>) eventClass,
                        dynamicListener,
                        priority,
                        (listener, event) -> {
                            if (listener instanceof DynamicListener) {
                                ((DynamicListener) listener).onEvent(event);
                            }
                        },
                        plugin
                    );

                    registeredListeners.add(dynamicListener);

                    plugin.getLogger().info("已注册事件: " + eventClass.getSimpleName() +
                                          " -> " + eventInfo.methodName);

                } catch (ClassNotFoundException e) {
                    plugin.getLogger().warning("未找到事件类: " + eventInfo.eventName);
                }
            }

        } catch (Exception e) {
            plugin.getLogger().error("注册事件处理器失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 提取事件信息
     * 格式: // @Event(eventName = "cn.nukkit.event.EventClass", priority="NORMAL", ignoreCancelled=false)
     */
    private List<EventInfo> extractEventInfo(String scriptContent) {
        List<EventInfo> eventInfos = new ArrayList<>();

        Pattern pattern = Pattern.compile(
            "(?:^|\\n)//\\s*@Event\\s*\\(\\s*" +
            "eventName\\s*=\\s*\"([^\"]+)\"(?:\\s*,\\s*" +
            "priority\\s*=\\s*\"([^\"]+)\")?(?:\\s*,\\s*" +
            "ignoreCancelled\\s*=\\s*(true|false))?\\s*\\)"
        );

        Matcher matcher = pattern.matcher(scriptContent);
        while (matcher.find()) {
            String eventName = matcher.group(1);
            String priority = matcher.group(2) != null ? matcher.group(2) : "NORMAL";
            boolean ignoreCancelled = "true".equals(matcher.group(3));

            // 提取下一行的函数名
            String methodName = extractMethodNameFromNextLine(scriptContent, matcher.end());

            if (methodName != null) {
                eventInfos.add(new EventInfo(eventName, methodName, priority, ignoreCancelled));
            }
        }

        return eventInfos;
    }

    /**
     * 从注释后的下一行提取函数名
     */
    private String extractMethodNameFromNextLine(String scriptContent, int commentEndIndex) {
        int nextLineStart = scriptContent.indexOf('\n', commentEndIndex);
        if (nextLineStart == -1) return null;

        Pattern functionPattern = Pattern.compile("function\\s+(\\w+)\\s*\\(");
        Matcher functionMatcher = functionPattern.matcher(scriptContent.substring(nextLineStart));

        if (functionMatcher.find()) {
            return functionMatcher.group(1);
        }
        return null;
    }

    /**
     * 创建动态监听器
     */
    private DynamicListener createDynamicListener(Class<?> eventClass, EventInfo eventInfo, String scriptPath) {
        return new DynamicListener(eventClass, scriptPath, this, loadedScripts, eventInfo);
    }

    /**
     * 调用JavaScript函数
     */
    public void invokeJSFunction(String functionName, Object... args) {
        try {
            Context cx = Context.enter();
            Object functionObj = scope.get(functionName, scope);

            if (functionObj instanceof Function) {
                Function function = (Function) functionObj;
                function.call(cx, scope, scope, args);
            } else {
                plugin.getLogger().warning("函数 " + functionName + " 未找到或不是函数");
            }
        } catch (Exception e) {
            plugin.getLogger().error("调用JavaScript函数失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            Context.exit();
        }
    }

    /**
     * 卸载脚本引擎
     */
    public void unload() {
        // 注销所有监听器
        for (Listener listener : registeredListeners) {
            try {
                // 使用HandlerList静态方法注销监听器
                HandlerList.unregisterAll(listener);
                plugin.getLogger().info("已注销监听器: " + listener.getClass().getSimpleName());
            } catch (Exception e) {
                plugin.getLogger().warning("注销监听器失败: " + e.getMessage());
            }
        }
        registeredListeners.clear();

        try {
            if (rhinoContext != null) Context.exit();
        } catch (Exception ignored) {}

        rhinoContext = null;
        scope = null;
        loadedScripts.clear();

        plugin.getLogger().info("脚本引擎已卸载");
    }

    // Getters
    public Context getContext() {
        return rhinoContext;
    }

    public Scriptable getScope() {
        return scope;
    }

    public YRItems getPlugin() {
        return plugin;
    }

    /**
     * 事件信息类
     */
    static class EventInfo {
        final String eventName;
        final String methodName;
        final String priority;
        final boolean ignoreCancelled;

        EventInfo(String eventName, String methodName, String priority, boolean ignoreCancelled) {
            this.eventName = eventName;
            this.methodName = methodName;
            this.priority = priority;
            this.ignoreCancelled = ignoreCancelled;
        }
    }

    /**
     * 动态事件监听器
     */
    static class DynamicListener implements Listener {
        private final Class<?> eventClass;
        private final String scriptPath;
        private final ItemsScriptEngineManager engineManager;
        private final Map<String, String> loadedScripts;
        private final EventInfo eventInfo;

        DynamicListener(Class<?> eventClass, String scriptPath,
                       ItemsScriptEngineManager engineManager,
                       Map<String, String> loadedScripts,
                       EventInfo eventInfo) {
            this.eventClass = eventClass;
            this.scriptPath = scriptPath;
            this.engineManager = engineManager;
            this.loadedScripts = loadedScripts;
            this.eventInfo = eventInfo;
        }

        public void onEvent(Event event) {
            if (!eventClass.isInstance(event)) {
                return;
            }

            // 检查是否应该忽略已取消的事件
            if (eventInfo.ignoreCancelled && event instanceof cn.nukkit.event.Cancellable) {
                cn.nukkit.event.Cancellable cancellable = (cn.nukkit.event.Cancellable) event;
                if (cancellable.isCancelled()) {
                    return;
                }
            }

            try {
                // 直接调用JavaScript函数，不重新执行脚本
                // 脚本已经在loadAllScripts()时执行过了
                engineManager.invokeJSFunction(eventInfo.methodName, event);
            } catch (Exception e) {
                engineManager.getPlugin().getLogger().error(
                    "执行事件处理函数失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
