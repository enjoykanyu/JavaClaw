package com.kanyu.companion.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 提示词模板引擎
 * 
 * 技术点：
 * 1. 支持变量替换 {{variable}}
 * 2. 支持条件渲染 {{#if condition}}...{{/if}}
 * 3. 支持循环渲染 {{#each items}}...{{/each}}
 * 4. 支持默认值 {{variable|default}}
 * 5. 支持过滤器（uppercase, lowercase, trim等）
 * 
 * 优化原因：
 * - 简单字符串替换无法满足复杂提示词需求
 * - 条件渲染可以根据上下文动态调整提示词内容
 * - 循环渲染可以处理列表数据（如技能列表、工具列表）
 * - 默认值和过滤器提高模板健壮性和可读性
 */
@Slf4j
@Component
public class PromptTemplateEngine {

    // 变量匹配模式: {{variable}} 或 {{variable|filter}} 或 {{variable|filter|default}}
    private static final Pattern VARIABLE_PATTERN = Pattern.compile(
        "\\{\\{\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*(\\|\\s*([a-zA-Z_]+)\\s*)?(\\|\\s*([^}]+))?\\s*\\}\\}"
    );

    // 条件渲染模式: {{#if condition}}...{{/if}}
    private static final Pattern IF_PATTERN = Pattern.compile(
        "\\{\\{\\s*#if\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\}\\}(.*?)(\\{\\{\\s*/if\\s*\\}\\})",
        Pattern.DOTALL
    );

    // 循环渲染模式: {{#each items}}...{{/each}}
    private static final Pattern EACH_PATTERN = Pattern.compile(
        "\\{\\{\\s*#each\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\}\\}(.*?)(\\{\\{\\s*/each\\s*\\}\\})",
        Pattern.DOTALL
    );

    // 嵌套变量模式（在循环内部）: {{this.property}} 或 {{this}}
    private static final Pattern THIS_PATTERN = Pattern.compile(
        "\\{\\{\\s*this(\\.([a-zA-Z_][a-zA-Z0-9_]*))?\\s*(\\|\\s*([a-zA-Z_]+)\\s*)?\\s*\\}\\}"
    );

    /**
     * 渲染模板
     * 
     * @param template 模板字符串
     * @param variables 变量映射
     * @return 渲染后的字符串
     */
    public String render(String template, Map<String, Object> variables) {
        if (template == null || template.isEmpty()) {
            return "";
        }

        String result = template;

        // 1. 处理条件渲染
        result = processIfConditions(result, variables);

        // 2. 处理循环渲染
        result = processEachLoops(result, variables);

        // 3. 处理变量替换
        result = processVariables(result, variables);

        return result;
    }

    /**
     * 处理条件渲染
     * 
     * 技术点：根据变量值决定是否渲染内容块
     * 支持：布尔值、非空字符串、非空集合、非空Map
     */
    private String processIfConditions(String template, Map<String, Object> variables) {
        StringBuffer result = new StringBuffer();
        Matcher matcher = IF_PATTERN.matcher(template);

        while (matcher.find()) {
            String conditionVar = matcher.group(1);
            String content = matcher.group(2);

            Object value = variables.get(conditionVar);
            boolean shouldRender = evaluateCondition(value);

            String replacement = shouldRender ? content : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 评估条件
     */
    private boolean evaluateCondition(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return !((String) value).isEmpty();
        }
        if (value instanceof Collection) {
            return !((Collection<?>) value).isEmpty();
        }
        if (value instanceof Map) {
            return !((Map<?, ?>) value).isEmpty();
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue() != 0;
        }
        return true;
    }

    /**
     * 处理循环渲染
     * 
     * 技术点：遍历集合，为每个元素渲染模板块
     * 支持：List、Set、Map（遍历时key为this.key, value为this.value）
     */
    @SuppressWarnings("unchecked")
    private String processEachLoops(String template, Map<String, Object> variables) {
        StringBuffer result = new StringBuffer();
        Matcher matcher = EACH_PATTERN.matcher(template);

        while (matcher.find()) {
            String listVar = matcher.group(1);
            String itemTemplate = matcher.group(2);

            Object listValue = variables.get(listVar);
            StringBuilder replacement = new StringBuilder();

            if (listValue instanceof Collection) {
                Collection<?> collection = (Collection<?>) listValue;
                for (Object item : collection) {
                    replacement.append(renderItemTemplate(itemTemplate, item));
                }
            } else if (listValue instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) listValue;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    Map<String, Object> itemContext = new HashMap<>();
                    itemContext.put("key", entry.getKey());
                    itemContext.put("value", entry.getValue());
                    replacement.append(renderItemTemplate(itemTemplate, itemContext));
                }
            } else if (listValue instanceof Object[]) {
                Object[] array = (Object[]) listValue;
                for (Object item : array) {
                    replacement.append(renderItemTemplate(itemTemplate, item));
                }
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 渲染单项模板
     */
    private String renderItemTemplate(String template, Object item) {
        if (item instanceof Map) {
            // 如果是Map，直接替换this.xxx
            return processThisVariables(template, (Map<String, Object>) item);
        } else {
            // 如果是普通对象，替换{{this}}为对象值
            return processThisVariables(template, Collections.singletonMap("this", item));
        }
    }

    /**
     * 处理this变量
     */
    private String processThisVariables(String template, Map<String, Object> context) {
        StringBuffer result = new StringBuffer();
        Matcher matcher = THIS_PATTERN.matcher(template);

        while (matcher.find()) {
            String property = matcher.group(2); // 可能是null
            String filter = matcher.group(4);   // 可能是null

            Object value;
            if (property == null) {
                value = context.get("this");
            } else {
                value = context.get(property);
            }

            String stringValue = value != null ? value.toString() : "";

            // 应用过滤器
            if (filter != null) {
                stringValue = applyFilter(stringValue, filter);
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(stringValue));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 处理变量替换
     */
    private String processVariables(String template, Map<String, Object> variables) {
        StringBuffer result = new StringBuffer();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);

        while (matcher.find()) {
            String varName = matcher.group(1);
            String filter = matcher.group(3);   // 可能是null
            String defaultValue = matcher.group(5); // 可能是null

            Object value = variables.get(varName);
            String stringValue;

            if (value == null) {
                stringValue = defaultValue != null ? defaultValue.trim() : "";
            } else {
                stringValue = value.toString();
            }

            // 应用过滤器
            if (filter != null && value != null) {
                stringValue = applyFilter(stringValue, filter);
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(stringValue));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 应用过滤器
     * 
     * 支持的过滤器：
     * - uppercase: 转大写
     * - lowercase: 转小写
     * - trim: 去除首尾空格
     * - capitalize: 首字母大写
     * - reverse: 反转字符串
     */
    private String applyFilter(String value, String filter) {
        if (value == null) {
            return "";
        }

        return switch (filter.toLowerCase()) {
            case "uppercase" -> value.toUpperCase();
            case "lowercase" -> value.toLowerCase();
            case "trim" -> value.trim();
            case "capitalize" -> capitalize(value);
            case "reverse" -> new StringBuilder(value).reverse().toString();
            default -> value;
        };
    }

    private String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    /**
     * 验证模板语法
     * 
     * @param template 模板字符串
     * @return 验证结果
     */
    public ValidationResult validate(String template) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 检查未闭合的if标签
        int ifOpenCount = countOccurrences(template, "{{#if");
        int ifCloseCount = countOccurrences(template, "{{/if");
        if (ifOpenCount != ifCloseCount) {
            errors.add("Unmatched #if tags: " + ifOpenCount + " open, " + ifCloseCount + " close");
        }

        // 检查未闭合的each标签
        int eachOpenCount = countOccurrences(template, "{{#each");
        int eachCloseCount = countOccurrences(template, "{{/each");
        if (eachOpenCount != eachCloseCount) {
            errors.add("Unmatched #each tags: " + eachOpenCount + " open, " + eachCloseCount + " close");
        }

        // 检查变量语法
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            String filter = matcher.group(3);
            if (filter != null && !isValidFilter(filter)) {
                warnings.add("Unknown filter: " + filter);
            }
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }

    private boolean isValidFilter(String filter) {
        return Set.of("uppercase", "lowercase", "trim", "capitalize", "reverse")
            .contains(filter.toLowerCase());
    }

    /**
     * 验证结果
     */
    public record ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
    }
}
