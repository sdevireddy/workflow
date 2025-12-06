package com.zen.workflow.engine;

import com.zen.workflow.model.ExecutionContext;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class VariableResolver {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    /**
     * Resolve variables in a string
     * Example: "Hello {{user.name}}" -> "Hello John"
     */
    public String resolve(String template, ExecutionContext context) {
        if (template == null || !template.contains("{{")) {
            return template;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String variablePath = matcher.group(1).trim();
            Object value = resolveVariablePath(variablePath, context);
            matcher.appendReplacement(result, value != null ? value.toString() : "");
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Resolve a variable path like "user.name" or "lead.email"
     */
    private Object resolveVariablePath(String path, ExecutionContext context) {
        String[] parts = path.split("\\.");
        Object current = context.getVariables();

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }

        return current;
    }

    /**
     * Resolve all variables in a map
     */
    public Map<String, Object> resolveMap(Map<String, Object> map, ExecutionContext context) {
        if (map == null) {
            return null;
        }

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof String) {
                entry.setValue(resolve((String) entry.getValue(), context));
            } else if (entry.getValue() instanceof Map) {
                entry.setValue(resolveMap((Map<String, Object>) entry.getValue(), context));
            }
        }

        return map;
    }
}
