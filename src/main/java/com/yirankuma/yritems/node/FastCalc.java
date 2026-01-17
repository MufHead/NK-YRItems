package com.yirankuma.yritems.node;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 简易数学表达式解析器 (支持 + - * / 和括号)
 */
public class FastCalc {
    public static double eval(String expr) {
        String s = expr.replaceAll("\\s+", "");
        Deque<Double> values = new ArrayDeque<>();
        Deque<Character> ops = new ArrayDeque<>();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isDigit(c) || c == '.') {
                int j = i;
                while (j < s.length() && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '.')) j++;
                double v = Double.parseDouble(s.substring(i, j));
                values.push(v);
                i = j;
                continue;
            }
            if (c == '(') {
                ops.push(c);
            } else if (c == ')') {
                while (!ops.isEmpty() && ops.peek() != '(') {
                    applyTop(values, ops);
                }
                if (!ops.isEmpty() && ops.peek() == '(') ops.pop();
            } else if (isOp(c)) {
                while (!ops.isEmpty() && precedence(ops.peek()) >= precedence(c)) {
                    applyTop(values, ops);
                }
                ops.push(c);
            } else {
                throw new IllegalArgumentException("非法字符: " + c);
            }
            i++;
        }
        while (!ops.isEmpty()) applyTop(values, ops);
        if (values.isEmpty()) return 0;
        return values.pop();
    }

    private static boolean isOp(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/';
    }

    private static int precedence(char c) {
        if (c == '+' || c == '-') return 1;
        if (c == '*' || c == '/') return 2;
        return 0;
    }

    private static void applyTop(Deque<Double> values, Deque<Character> ops) {
        if (values.size() < 2 || ops.isEmpty()) return;
        double b = values.pop();
        double a = values.pop();
        char op = ops.pop();
        switch (op) {
            case '+': values.push(a + b); break;
            case '-': values.push(a - b); break;
            case '*': values.push(a * b); break;
            case '/': values.push(b == 0 ? 0 : a / b); break;
            default: break;
        }
    }
}