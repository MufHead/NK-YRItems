package com.yirankuma.yritems.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NodeResult {
    private String stringValue;
    private Double numberValue;
    private List<String> list;
    private List<String> elseList;

    public static NodeResult ofString(String s) {
        NodeResult r = new NodeResult();
        r.stringValue = s;
        return r;
    }

    public static NodeResult ofNumber(Double d) {
        NodeResult r = new NodeResult();
        r.numberValue = d;
        return r;
    }

    public static NodeResult ofList(List<String> list, List<String> elseList) {
        NodeResult r = new NodeResult();
        r.list = list != null ? new ArrayList<>(list) : new ArrayList<>();
        r.elseList = elseList != null ? new ArrayList<>(elseList) : new ArrayList<>();
        return r;
    }

    public String asString() {
        if (stringValue != null) return stringValue;
        if (numberValue != null) return String.valueOf(numberValue);
        if (list != null) return String.join(" | ", list);
        return "";
    }

    public Double asNumberOrNull() {
        return numberValue;
    }

    public List<String> getList() {
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    public List<String> getElseList() {
        return elseList == null ? Collections.emptyList() : Collections.unmodifiableList(elseList);
    }

    public int getLength() {
        return list == null ? 0 : list.size();
    }

    public int getElseLength() {
        return elseList == null ? 0 : elseList.size();
    }
}