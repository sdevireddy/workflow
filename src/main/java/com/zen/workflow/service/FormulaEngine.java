package com.zen.workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formula evaluation engine for workflows
 * Supports mathematical, logical, string, and date operations
 */
@Slf4j
@Service
public class FormulaEngine {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("([A-Z_]+)\\(([^)]*)\\)");

    /**
     * Evaluate formula with context variables
     */
    public Object evaluateFormula(String formula, Map<String, Object> context) {
        try {
            log.debug("Evaluating formula: {}", formula);

            // Replace variables
            String processedFormula = replaceVariables(formula, context);

            // Evaluate functions
            processedFormula = evaluateFunctions(processedFormula, context);

            // Evaluate expression
            Object result = evaluateExpression(processedFormula);

            log.debug("Formula result: {}", result);
            return result;

        } catch (Exception e) {
            log.error("Formula evaluation failed: {}", formula, e);
            throw new RuntimeException("Formula evaluation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Replace variables in formula
     */
    private String replaceVariables(String formula, Map<String, Object> context) {
        Matcher matcher = VARIABLE_PATTERN.matcher(formula);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            Object value = getNestedValue(varName, context);
            String replacement = value != null ? value.toString() : "null";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Get nested value from context (e.g., "user.address.city")
     */
    private Object getNestedValue(String path, Map<String, Object> context) {
        String[] parts = path.split("\\.");
        Object current = context;

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
     * Evaluate built-in functions
     */
    private String evaluateFunctions(String formula, Map<String, Object> context) {
        Matcher matcher = FUNCTION_PATTERN.matcher(formula);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String functionName = matcher.group(1);
            String args = matcher.group(2);
            String functionResult = evaluateFunction(functionName, args, context);
            matcher.appendReplacement(result, Matcher.quoteReplacement(functionResult));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Evaluate individual function
     */
    private String evaluateFunction(String functionName, String args, Map<String, Object> context) {
        String[] argArray = args.split(",");
        for (int i = 0; i < argArray.length; i++) {
            argArray[i] = argArray[i].trim();
        }

        switch (functionName) {
            // Math functions
            case "ABS":
                return String.valueOf(Math.abs(Double.parseDouble(argArray[0])));
            case "ROUND":
                int decimals = argArray.length > 1 ? Integer.parseInt(argArray[1]) : 0;
                return new BigDecimal(argArray[0]).setScale(decimals, RoundingMode.HALF_UP).toString();
            case "CEIL":
                return String.valueOf(Math.ceil(Double.parseDouble(argArray[0])));
            case "FLOOR":
                return String.valueOf(Math.floor(Double.parseDouble(argArray[0])));
            case "MAX":
                return String.valueOf(Arrays.stream(argArray)
                    .mapToDouble(Double::parseDouble)
                    .max()
                    .orElse(0));
            case "MIN":
                return String.valueOf(Arrays.stream(argArray)
                    .mapToDouble(Double::parseDouble)
                    .min()
                    .orElse(0));
            case "SUM":
                return String.valueOf(Arrays.stream(argArray)
                    .mapToDouble(Double::parseDouble)
                    .sum());
            case "AVG":
                return String.valueOf(Arrays.stream(argArray)
                    .mapToDouble(Double::parseDouble)
                    .average()
                    .orElse(0));

            // String functions
            case "UPPER":
                return argArray[0].toUpperCase();
            case "LOWER":
                return argArray[0].toLowerCase();
            case "TRIM":
                return argArray[0].trim();
            case "LEN":
                return String.valueOf(argArray[0].length());
            case "CONCAT":
                return String.join("", argArray);
            case "SUBSTRING":
                int start = Integer.parseInt(argArray[1]);
                int end = argArray.length > 2 ? Integer.parseInt(argArray[2]) : argArray[0].length();
                return argArray[0].substring(start, Math.min(end, argArray[0].length()));
            case "REPLACE":
                return argArray[0].replace(argArray[1], argArray[2]);

            // Date functions
            case "NOW":
                return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            case "TODAY":
                return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            case "DATE_ADD":
                LocalDate date = LocalDate.parse(argArray[0]);
                int days = Integer.parseInt(argArray[1]);
                return date.plusDays(days).format(DateTimeFormatter.ISO_LOCAL_DATE);
            case "DATE_DIFF":
                LocalDate date1 = LocalDate.parse(argArray[0]);
                LocalDate date2 = LocalDate.parse(argArray[1]);
                return String.valueOf(ChronoUnit.DAYS.between(date1, date2));
            case "YEAR":
                return String.valueOf(LocalDate.parse(argArray[0]).getYear());
            case "MONTH":
                return String.valueOf(LocalDate.parse(argArray[0]).getMonthValue());
            case "DAY":
                return String.valueOf(LocalDate.parse(argArray[0]).getDayOfMonth());

            // Logical functions
            case "IF":
                boolean condition = Boolean.parseBoolean(argArray[0]);
                return condition ? argArray[1] : argArray[2];
            case "AND":
                return String.valueOf(Arrays.stream(argArray)
                    .allMatch(Boolean::parseBoolean));
            case "OR":
                return String.valueOf(Arrays.stream(argArray)
                    .anyMatch(Boolean::parseBoolean));
            case "NOT":
                return String.valueOf(!Boolean.parseBoolean(argArray[0]));

            // Utility functions
            case "ISBLANK":
                return String.valueOf(argArray[0] == null || argArray[0].trim().isEmpty());
            case "ISNUMBER":
                try {
                    Double.parseDouble(argArray[0]);
                    return "true";
                } catch (NumberFormatException e) {
                    return "false";
                }

            default:
                log.warn("Unknown function: {}", functionName);
                return args;
        }
    }

    /**
     * Evaluate mathematical/logical expression
     */
    private Object evaluateExpression(String expression) {
        expression = expression.trim();

        // Handle boolean values
        if ("true".equalsIgnoreCase(expression)) return true;
        if ("false".equalsIgnoreCase(expression)) return false;
        if ("null".equalsIgnoreCase(expression)) return null;

        // Handle string literals
        if (expression.startsWith("\"") && expression.endsWith("\"")) {
            return expression.substring(1, expression.length() - 1);
        }
        if (expression.startsWith("'") && expression.endsWith("'")) {
            return expression.substring(1, expression.length() - 1);
        }

        // Handle numbers
        try {
            if (expression.contains(".")) {
                return Double.parseDouble(expression);
            } else {
                return Long.parseLong(expression);
            }
        } catch (NumberFormatException e) {
            // Not a number, continue
        }

        // Handle simple arithmetic
        if (expression.contains("+")) {
            String[] parts = expression.split("\\+");
            return evaluateArithmetic(parts, (a, b) -> a + b);
        }
        if (expression.contains("-") && !expression.startsWith("-")) {
            String[] parts = expression.split("-");
            return evaluateArithmetic(parts, (a, b) -> a - b);
        }
        if (expression.contains("*")) {
            String[] parts = expression.split("\\*");
            return evaluateArithmetic(parts, (a, b) -> a * b);
        }
        if (expression.contains("/")) {
            String[] parts = expression.split("/");
            return evaluateArithmetic(parts, (a, b) -> a / b);
        }

        // Handle comparisons
        if (expression.contains(">=")) {
            String[] parts = expression.split(">=");
            return compareNumbers(parts[0].trim(), parts[1].trim()) >= 0;
        }
        if (expression.contains("<=")) {
            String[] parts = expression.split("<=");
            return compareNumbers(parts[0].trim(), parts[1].trim()) <= 0;
        }
        if (expression.contains(">")) {
            String[] parts = expression.split(">");
            return compareNumbers(parts[0].trim(), parts[1].trim()) > 0;
        }
        if (expression.contains("<")) {
            String[] parts = expression.split("<");
            return compareNumbers(parts[0].trim(), parts[1].trim()) < 0;
        }
        if (expression.contains("==")) {
            String[] parts = expression.split("==");
            return parts[0].trim().equals(parts[1].trim());
        }
        if (expression.contains("!=")) {
            String[] parts = expression.split("!=");
            return !parts[0].trim().equals(parts[1].trim());
        }

        return expression;
    }

    /**
     * Evaluate arithmetic operation
     */
    private double evaluateArithmetic(String[] parts, ArithmeticOperation op) {
        double result = Double.parseDouble(parts[0].trim());
        for (int i = 1; i < parts.length; i++) {
            result = op.apply(result, Double.parseDouble(parts[i].trim()));
        }
        return result;
    }

    /**
     * Compare two numbers
     */
    private int compareNumbers(String a, String b) {
        return Double.compare(Double.parseDouble(a), Double.parseDouble(b));
    }

    @FunctionalInterface
    private interface ArithmeticOperation {
        double apply(double a, double b);
    }

    /**
     * Validate formula syntax
     */
    public boolean validateFormula(String formula) {
        try {
            // Basic validation
            if (formula == null || formula.trim().isEmpty()) {
                return false;
            }

            // Check balanced parentheses
            int openCount = 0;
            for (char c : formula.toCharArray()) {
                if (c == '(') openCount++;
                if (c == ')') openCount--;
                if (openCount < 0) return false;
            }

            return openCount == 0;

        } catch (Exception e) {
            log.error("Formula validation failed", e);
            return false;
        }
    }

    /**
     * Get list of supported functions
     */
    public List<String> getSupportedFunctions() {
        return Arrays.asList(
            // Math
            "ABS", "ROUND", "CEIL", "FLOOR", "MAX", "MIN", "SUM", "AVG",
            // String
            "UPPER", "LOWER", "TRIM", "LEN", "CONCAT", "SUBSTRING", "REPLACE",
            // Date
            "NOW", "TODAY", "DATE_ADD", "DATE_DIFF", "YEAR", "MONTH", "DAY",
            // Logical
            "IF", "AND", "OR", "NOT",
            // Utility
            "ISBLANK", "ISNUMBER"
        );
    }
}
