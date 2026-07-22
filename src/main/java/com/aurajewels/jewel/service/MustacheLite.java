/*
 * MIT License
 *
 * Copyright (c) 2026 AuraJewels (Raviraj Bhosale)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.aurajewels.jewel.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * Minimal, dependency-free Mustache renderer — the build environment cannot resolve a templating
 * library. Supports the subset the invoice template uses:
 *
 * <ul>
 *   <li>{@code {{ name }}} — HTML-escaped variable
 *   <li>{@code {{# name }}...{{/ name }}} — section: repeats over a List, or renders once for a
 *       truthy scalar/boolean, or is skipped when falsy/empty
 *   <li>{@code {{^ name }}...{{/ name }}} — inverted section (renders when falsy/empty)
 *   <li>{@code {{! comment }}} — ignored
 * </ul>
 *
 * Lookups walk a context stack (list element → parent → …). Data is nested {@code Map<String,
 * Object>} with {@code List<Map<String,Object>>} for iterable sections.
 *
 * @author Raviraj Bhosale
 */
public final class MustacheLite {

    private MustacheLite() {}

    public static String render(String template, Map<String, Object> data) {
        Section root = parse(template);
        StringBuilder out = new StringBuilder(template.length() * 2);
        Deque<Object> stack = new ArrayDeque<>();
        stack.push(data);
        renderNodes(root.children, stack, out);
        return out.toString();
    }

    // ── node model ──────────────────────────────────────────────────

    private sealed interface Node permits Text, Var, Section {}

    private record Text(String value) implements Node {}

    private record Var(String name) implements Node {}

    private static final class Section implements Node {
        final String name;
        final boolean inverted;
        final List<Node> children = new ArrayList<>();

        Section(String name, boolean inverted) {
            this.name = name;
            this.inverted = inverted;
        }
    }

    // ── parse ───────────────────────────────────────────────────────

    private static Section parse(String tmpl) {
        Section root = new Section(null, false);
        Deque<Section> stack = new ArrayDeque<>();
        stack.push(root);

        int i = 0;
        while (i < tmpl.length()) {
            int open = tmpl.indexOf("{{", i);
            if (open < 0) {
                stack.peek().children.add(new Text(tmpl.substring(i)));
                break;
            }
            if (open > i) {
                stack.peek().children.add(new Text(tmpl.substring(i, open)));
            }
            int close = tmpl.indexOf("}}", open + 2);
            if (close < 0) {
                stack.peek().children.add(new Text(tmpl.substring(open)));
                break;
            }
            String tag = tmpl.substring(open + 2, close).trim();
            i = close + 2;

            if (tag.isEmpty() || tag.startsWith("!")) {
                continue; // comment / empty
            }
            char c = tag.charAt(0);
            if (c == '#' || c == '^') {
                Section s = new Section(tag.substring(1).trim(), c == '^');
                stack.peek().children.add(s);
                stack.push(s);
            } else if (c == '/') {
                if (stack.size() > 1) {
                    stack.pop();
                }
            } else {
                stack.peek().children.add(new Var(tag));
            }
        }
        return root;
    }

    // ── render ──────────────────────────────────────────────────────

    private static void renderNodes(List<Node> nodes, Deque<Object> stack, StringBuilder out) {
        for (Node node : nodes) {
            if (node instanceof Text t) {
                out.append(t.value());
            } else if (node instanceof Var v) {
                Object value = lookup(v.name(), stack);
                out.append(escape(value == null ? "" : String.valueOf(value)));
            } else if (node instanceof Section s) {
                renderSection(s, stack, out);
            }
        }
    }

    private static void renderSection(Section s, Deque<Object> stack, StringBuilder out) {
        Object value = lookup(s.name, stack);
        if (s.inverted) {
            if (isFalsy(value)) {
                renderNodes(s.children, stack, out);
            }
            return;
        }
        if (value instanceof List<?> list) {
            for (Object element : list) {
                stack.push(element);
                renderNodes(s.children, stack, out);
                stack.pop();
            }
        } else if (!isFalsy(value)) {
            stack.push(value);
            renderNodes(s.children, stack, out);
            stack.pop();
        }
    }

    @SuppressWarnings("unchecked")
    private static Object lookup(String name, Deque<Object> stack) {
        for (Object frame : stack) {
            if (frame instanceof Map<?, ?> map && ((Map<String, Object>) map).containsKey(name)) {
                return ((Map<String, Object>) map).get(name);
            }
        }
        return null;
    }

    private static boolean isFalsy(Object v) {
        if (v == null) {
            return true;
        }
        if (v instanceof Boolean b) {
            return !b;
        }
        if (v instanceof String str) {
            return str.isEmpty();
        }
        if (v instanceof List<?> list) {
            return list.isEmpty();
        }
        if (v instanceof Map<?, ?> map) {
            return map.isEmpty();
        }
        return false;
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
