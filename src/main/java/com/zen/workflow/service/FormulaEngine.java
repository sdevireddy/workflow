package com.zen.workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Formula evaluation engine for workflows
 * Supports mathematical operations, string operations, and logical operations
 */
@Slf4j
@Service
public class FormulaEngine {
    
    /**
     * Validate formula syntax
     */
    public boolean validateFormula(String formula) {
        if (formula == null || formula.trim().isEmpty()) {
            return false;
        }
        
        // Basic validation - check for balanced parentheses
        int openCount = 0;
        for (char c : formula.toCharArray()) {
            if (c == '(') openCount++;
            if (c == ')') openCount--;
            if (openCount < 0) return false;
        }
        
        return openCount == 0;
    }
    
    /**
     * Evaluate formula with context variables
     */
    public Object evaluateFormula(String formula, Map<String, Object> variables) {
        log.info("Evaluating formula: {}", formula);
        
        try {
            // Replace variables in formula
            String resolvedFormula = resolveVariables(formula, variables);
            
            // Evaluate the formula
            Object result = evaluate(resolvedFormula);
            
            log.info("Formula result: {}", result);
            return result;
            
        } catch (Exception e) {
            log.error("Formula evaluation failed: {}", formula, e);
            throw new RuntimeException("Formula evaluation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Resolve variables in formula
     */
    private String resolveVariables(String formula, Map<String, Object> variables) {
        String result = formula;
        
        // Replace {{variable}} with actual values
        Pattern pattern = Pattern.compile("\\{\\{([^}]+)\\}\\}");
        java.util.regex.Matcher matcher = pattern.matcher(formula);
        
        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            Object value = getNestedValue(varName, variables);
            
            if (value != null) {
                String replacement = value.toString();
                if (value instanceof String) {
                    replacement = "\"" + replacement + "\"";
                }
                result = result.replace("{{" + varName + "}}", replacement);
            }
        }
        
        return result;
    }
    
    /**
     * Get nested value from variables map (e.g., "user.name")
     */
    private Object getNestedValue(String path, Map<String, Object> variables) {
        String[] parts = path.split("\\.");
        Object current = variables;
        
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
     * Evaluate the resolved formula
     * This is a simplified implementation - in production, use a proper expression evaluator
     */
    private Object evaluate(String formula) {
        // Remove whitespace
        formula = formula.trim();
        
        // Handle boolean literals
        if ("true".equalsIgnoreCase(formula)) return true;
        if ("false".equalsIgnoreCase(formula)) return false;
        
        // Handle string literals
        if (formula.startsWith("\"") && formula.endsWith("\"")) {
            return formula.substring(1, formula.length() - 1);
        }
        
        // Handle numeric literals
        try {
            if (formula.contains(".")) {
                return Double.parseDouble(formula);
            } else {
                return Long.parseLong(formula);
            }
        } catch (NumberFormatException e) {
            // Not a number, continue
        }
        
        // Handle simple arithmetic operations
        if (formula.contains("+")) {
            String[] parts = formula.split("\\+");
            if (parts.length == 2) {
                Object left = evaluate(parts[0].trim());
                Object right = evaluate(parts[1].trim());
                return add(left, right);
            }
        }
        
        if (formula.contains("-")) {
            String[] parts = formula.split("-");
            if (parts.length == 2) {
                Object left = evaluate(parts[0].trim());
                Object right = evaluate(parts[1].trim());
                return subtract(left, right);
            }
        }
        
        if (formula.contains("*")) {
            String[] parts = formula.split("\\*");
            if (parts.length == 2) {
                Object left = evaluate(parts[0].trim());
                Object right = evaluate(parts[1].trim());
                return multiply(left, right);
            }
        }
        
        if (formula.contains("/")) {
            String[] parts = formula.split("/");
            if (parts.length == 2) {
                Object left = evaluate(parts[0].trim());
                Object right = evaluate(parts[1].trim());
                return divide(left, right);
            }
        }
        
        // Handle comparison operations
        if (formula.contains("==")) {
            String[] parts = formula.split("==");
            if (parts.length == 2) {
                Object left = evaluate(parts[0].trim());
                Object right = evaluate(parts[1].trim());
                return equals(left, right);
            }
        }
        
        if (formula.contains("!=")) {
            String[] parts = formula.split("!=");
            if (parts.length == 2) {
                Object left = evaluate(parts[0].trim());
                Object right = evaluate(parts[1].trim());
                return !equals(left, right);
            }
        }
        
        if (formula.contains(">")) {
            String[] parts = formula.split(">");
            if (parts.length == 2) {
                Object left = evaluate(parts[0].trim());
                Object right = evaluate(parts[1].trim());
                return greaterThan(left, right);
            }
        }
        
        if (formula.contains("<")) {
            String[] parts = formula.split("<");
            if (parts.length == 2) {
                Object left = evaluate(parts[0].trim());
                Object right = evaluate(parts[1].trim());
                return lessThan(left, right);
            }
        }
        
        // Handle logical operations
        if (formula.contains("&&")) {
            String[] parts = formula.split("&&");
            if (parts.length == 2) {
                Object left = evaluate(parts[0].trim());
                Object right = evaluate(parts[1].trim());
                return toBoolean(left) && toBoolean(right);
            }
        }
        
        if (formula.contains("||")) {
            String[] parts = formula.split("\\|\\|");
            if (parts.length == 2) {
                Object left = evaluate(parts[0].trim());
                Object right = evaluate(parts[1].trim());
                return toBoolean(left) || toBoolean(right);
            }
        }
        
        // Default: return as string
        return formula;
    }
    
    // Helper methods for operations
    
    private Object add(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            return ((Number) left).doubleValue() + ((Number) right).doubleValue();
        }
        return left.toString() + right.toString();
    }
    
    private Object subtract(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            return ((Number) left).doubleValue() - ((Number) right).doubleValue();
        }
        throw new RuntimeException("Cannot subtract non-numeric values");
    }
    
    private Object multiply(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            return ((Number) left).doubleValue() * ((Number) right).doubleValue();
        }
        throw new RuntimeException("Cannot multiply non-numeric values");
    }
    
    private Object divide(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            double divisor = ((Number) right).doubleValue();
            if (divisor == 0) {
                throw new RuntimeException("Division by zero");
            }
            return ((Number) left).doubleValue() / divisor;
        }
        throw new RuntimeException("Cannot divide non-numeric values");
    }
    
    private boolean equals(Object left, Object right) {
        if (left == null && right == null) return true;
        if (left == null || right == null) return false;
        
        if (left instanceof Number && right instanceof Number) {
            return ((Number) left).doubleValue() == ((Number) right).doubleValue();
        }
        
        return left.equals(right);
    }
    
    private boolean greaterThan(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            return ((Number) left).doubleValue() > ((Number) right).doubleValue();
        }
        throw new RuntimeException("Cannot compare non-numeric values");
    }
    
    private boolean lessThan(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            return ((Number) left).doubleValue() < ((Number) right).doubleValue();
        }
        throw new RuntimeException("Cannot compare non-numeric values");
    }
    
    private boolean toBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue() != 0;
        }
        if (value instanceof String) {
            return !value.toString().isEmpty();
        }
        return value != null;
    }
}
