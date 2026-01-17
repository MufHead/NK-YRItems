package com.yirankuma.yritems.node;

import java.util.List;
import java.util.Map;

public class NodeDefinition {
    private final String id;
    private final NodeType type;
    private final Map<String, Object> options;

    public NodeDefinition(String id, NodeType type, Map<String, Object> options) {
        this.id = id;
        this.type = type;
        this.options = options;
    }

    public String getId() {
        return id;
    }

    public NodeType getType() {
        return type;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    @SuppressWarnings("unchecked")
    public List<String> getStringList(String key) {
        Object v = options.get(key);
        if (v instanceof List) {
            return (List<String>) v;
        }
        return null;
    }

    public String getString(String key, String def) {
        Object v = options.get(key);
        return v == null ? def : String.valueOf(v);
    }

    public double getDouble(String key, double def) {
        Object v = options.get(key);
        if (v == null) return def;
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return def;
        }
    }

    public int getInt(String key, int def) {
        Object v = options.get(key);
        if (v == null) return def;
        try {
            return (int) Math.floor(Double.parseDouble(String.valueOf(v)));
        } catch (Exception e) {
            return def;
        }
    }

    public boolean getBoolean(String key, boolean def) {
        Object v = options.get(key);
        if (v == null) return def;
        if (v instanceof Boolean) return (Boolean) v;
        return Boolean.parseBoolean(String.valueOf(v));
    }
}