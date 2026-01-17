package com.yirankuma.yritems.node;

import com.yirankuma.yritems.YRItems;
import com.yirankuma.yritems.script.ItemsScriptEngineManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class NodeEvaluator {
    private final Random random = new Random();

    public Map<String, NodeResult> evaluateAll(Map<String, NodeDefinition> defs) {
        Map<String, NodeResult> out = new HashMap<>();
        if (defs == null) return out;
        // First pass: evaluate independent nodes
        for (Map.Entry<String, NodeDefinition> e : defs.entrySet()) {
            NodeDefinition def = e.getValue();
            NodeResult r = evaluateNode(def, out);
            if (r != null) {
                out.put(e.getKey(), r);
                if (def.getType() == NodeType.WEIGHTDECLARE) {
                    String keyAlias = def.getString("key", def.getId());
                    out.put(keyAlias, r);
                }
            }
        }
        return out;
    }

    private NodeResult evaluateNode(NodeDefinition def, Map<String, NodeResult> ctx) {
        switch (def.getType()) {
            case STRINGS: return evalStrings(def);
            case NUMBER: return evalNumber(def);
            case CHANCE: return evalChance(def);
            case GAUSSIAN: return evalGaussian(def);
            case CALCULATION: return evalCalc(def, ctx, true);
            case FASTCALC: return evalCalc(def, ctx, false);
            case WEIGHT: return evalWeight(def);
            case WEIGHTDECLARE: return evalWeightDeclare(def);
            case JS: return evalJS(def);
            default: return null;
        }
    }

    private NodeResult evalStrings(NodeDefinition def) {
        List<String> values = def.getStringList("values");
        if (values == null || values.isEmpty()) return NodeResult.ofString("");
        String v = values.get(random.nextInt(values.size()));
        return NodeResult.ofString(v);
    }

    private NodeResult evalWeight(NodeDefinition def) {
        List<String> values = def.getStringList("values");
        if (values == null || values.isEmpty()) return NodeResult.ofString("");
        Map<String, Integer> weightMap = new LinkedHashMap<>();
        for (String s : values) {
            String[] parts = s.split("::", 2);
            if (parts.length < 2) continue;
            int w = safeInt(parts[0], 0);
            String text = parts[1];
            weightMap.put(text, weightMap.getOrDefault(text, 0) + w);
        }
        if (weightMap.isEmpty()) return NodeResult.ofString("");
        int total = weightMap.values().stream().mapToInt(Integer::intValue).sum();
        int r = random.nextInt(Math.max(total, 1)) + 1;
        int cum = 0;
        for (Map.Entry<String, Integer> e : weightMap.entrySet()) {
            cum += e.getValue();
            if (r <= cum) return NodeResult.ofString(e.getKey());
        }
        return NodeResult.ofString("");
    }

    private NodeResult evalWeightDeclare(NodeDefinition def) {
        List<String> list = def.getStringList("list");
        if (list == null) list = Collections.emptyList();
        String key = def.getString("key", def.getId());
        int amount = def.getInt("amount", 1);
        boolean shuffled = def.getBoolean("shuffled", false);
        boolean putelse = def.getBoolean("putelse", false);
        boolean order = def.getBoolean("order", false);

        Map<String, Integer> weightMap = new LinkedHashMap<>();
        for (String s : list) {
            String[] parts = s.split("::", 2);
            if (parts.length < 2) continue;
            int w = safeInt(parts[0], 0);
            String text = parts[1];
            weightMap.put(text, weightMap.getOrDefault(text, 0) + w);
        }
        List<String> allKeys = new ArrayList<>(weightMap.keySet());
        List<String> selected = new ArrayList<>();
        Map<String, Integer> working = new LinkedHashMap<>(weightMap);
        for (int i = 0; i < amount && !working.isEmpty(); i++) {
            String pick = weightedPick(working);
            selected.add(pick);
            working.remove(pick);
        }

        if (shuffled) Collections.shuffle(selected, random);
        else if (order) {
            // keep given list order by re-sorting selected to match allKeys order
            selected.sort(Comparator.comparingInt(allKeys::indexOf));
        }

        List<String> elseList = new ArrayList<>();
        if (putelse) {
            for (String s : allKeys) {
                if (!selected.contains(s)) elseList.add(s);
            }
        }
        return NodeResult.ofList(selected, elseList);
    }

    private String weightedPick(Map<String, Integer> weightMap) {
        int total = weightMap.values().stream().mapToInt(Integer::intValue).sum();
        int r = random.nextInt(Math.max(total, 1)) + 1;
        int cum = 0;
        for (Map.Entry<String, Integer> e : weightMap.entrySet()) {
            cum += e.getValue();
            if (r <= cum) return e.getKey();
        }
        return weightMap.keySet().iterator().next();
    }

    private NodeResult evalNumber(NodeDefinition def) {
        double min = def.getDouble("min", 0);
        double max = def.getDouble("max", 0);
        int fixed = def.getInt("fixed", 0);
        String modeStr = def.getString("mode", "HALF_UP");
        double val = min + random.nextDouble() * (max - min);
        val = applyRounding(val, fixed, modeStr);
        return NodeResult.ofNumber(val);
    }

    private NodeResult evalChance(NodeDefinition def) {
        double success = def.getDouble("success", 0);
        double total = def.getDouble("total", 1);
        int repeat = def.getInt("repeat", 1);
        Integer min = def.getOptions().containsKey("min") ? def.getInt("min", 0) : null;
        Integer max = def.getOptions().containsKey("max") ? def.getInt("max", Integer.MAX_VALUE) : null;
        double p = total == 0 ? 0 : success / total;
        int count = 0;
        for (int i = 0; i < repeat; i++) {
            if (random.nextDouble() < p) count++;
        }
        if (min != null) count = Math.max(count, min);
        if (max != null) count = Math.min(count, max);
        return NodeResult.ofNumber((double) count);
    }

    private NodeResult evalGaussian(NodeDefinition def) {
        double base = def.getDouble("base", 0);
        double spread = def.getDouble("spread", 0.1);
        double maxSpread = def.getDouble("maxSpread", 1.0);
        int fixed = def.getInt("fixed", 1);
        String modeStr = def.getString("mode", "HALF_UP");
        Double min = def.getOptions().containsKey("min") ? def.getDouble("min", Double.NEGATIVE_INFINITY) : null;
        Double max = def.getOptions().containsKey("max") ? def.getDouble("max", Double.POSITIVE_INFINITY) : null;
        double sigma = Math.abs(base) * spread;
        double g = random.nextGaussian();
        double val = base + g * sigma;
        double clampLow = base - Math.abs(base) * maxSpread;
        double clampHigh = base + Math.abs(base) * maxSpread;
        val = Math.max(clampLow, Math.min(clampHigh, val));
        if (min != null) val = Math.max(min, val);
        if (max != null) val = Math.min(max, val);
        val = applyRounding(val, fixed, modeStr);
        return NodeResult.ofNumber(val);
    }

    private NodeResult evalCalc(NodeDefinition def, Map<String, NodeResult> ctx, boolean useJS) {
        String formula = def.getString("formula", "");
        if (formula.isEmpty()) return NodeResult.ofNumber(0.0);
        // 替换 <nodeId> 依赖
        String replaced = replacePlaceholdersInFormula(formula, ctx);
        double val;
        try {
            if (useJS) {
                // 用Rhino执行表达式
                Object ret = YRItems.getInstance().getScriptEngine().evalExpression(replaced);
                val = Double.parseDouble(String.valueOf(ret));
            } else {
                val = FastCalc.eval(replaced);
            }
        } catch (Exception e) {
            val = 0.0;
        }
        Integer min = def.getOptions().containsKey("min") ? def.getInt("min", 0) : null;
        Integer max = def.getOptions().containsKey("max") ? def.getInt("max", Integer.MAX_VALUE) : null;
        int fixed = def.getInt("fixed", 0);
        String modeStr = def.getString("mode", "HALF_UP");
        if (min != null) val = Math.max(min, val);
        if (max != null) val = Math.min(max, val);
        val = applyRounding(val, fixed, modeStr);
        return NodeResult.ofNumber(val);
    }

    private NodeResult evalJS(NodeDefinition def) {
        String path = def.getString("path", "");
        List<String> args = def.getStringList("args");
        ItemsScriptEngineManager engine = YRItems.getInstance().getScriptEngine();
        Object result = engine.invokePath(path, args == null ? Collections.emptyList() : args);
        if (result == null) return NodeResult.ofString("");
        try {
            double d = Double.parseDouble(String.valueOf(result));
            return NodeResult.ofNumber(d);
        } catch (Exception e) {
            return NodeResult.ofString(String.valueOf(result));
        }
    }

    private String replacePlaceholdersInFormula(String formula, Map<String, NodeResult> ctx) {
        String replaced = formula;
        for (Map.Entry<String, NodeResult> e : ctx.entrySet()) {
            String key = e.getKey();
            NodeResult r = e.getValue();
            Double num = r.asNumberOrNull();
            String val = num != null ? String.valueOf(num) : r.asString();
            replaced = replaced.replace("<" + key + ">", val);
        }
        return replaced;
    }

    private double applyRounding(double val, int fixed, String modeStr) {
        RoundingMode mode;
        try {
            mode = RoundingMode.valueOf(modeStr);
        } catch (Exception e) {
            mode = RoundingMode.HALF_UP;
        }
        BigDecimal bd = BigDecimal.valueOf(val);
        bd = bd.setScale(Math.max(fixed, 0), mode);
        return bd.doubleValue();
    }

    private int safeInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
}