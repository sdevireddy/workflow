package com.zen.workflow.handler;

import com.zen.workflow.engine.VariableResolver;
import com.zen.workflow.model.ExecutionContext;
import com.zen.workflow.model.ExecutionResult;
import com.zen.workflow.model.NodeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles all condition-based logic:
 * - if_else
 * - multi_branch
 * - switch
 * - field_check
 * - compare_fields
 * - formula
 */
@Slf4j
@Component
public class ConditionEvaluator implements NodeHandler {

    @Autowired
    private VariableResolver variableResolver;
    
    @Autowired
    private com.zen.workflow.service.FormulaEngine formulaEngine;

    @Override
    public ExecutionResult execute(NodeConfig config, ExecutionContext context) {
        String subtype = config.getSubtype();
        log.info("Executing Condition: {}", subtype);

        try {
            switch (subtype) {
                case "if_else":
                case "field_check":
                    return handleFieldCheck(config, context);
                case "multi_branch":
                    return handleMultiBranch(config, context);
                case "switch":
                    return handleSwitch(config, context);
                case "compare_fields":
                    return handleCompareFields(config, context);
                case "formula":
                    return handleFormula(config, context);
                default:
                    return ExecutionResult.failed("Unknown condition subtype: " + subtype);
            }
        } catch (Exception e) {
            log.error("Condition evaluation failed", e);
            return ExecutionResult.failed(e.getMessage());
        }
    }

    private ExecutionResult handleFieldCheck(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String field = (String) nodeConfig.get("field");
        String operator = (String) nodeConfig.get("operator");
        Object expectedValue = nodeConfig.get("value");
        
        // Resolve field value from context
        String fieldPath = variableResolver.resolve("{{" + field + "}}", context);
        Object actualValue = resolveFieldValue(field, context);
        
        log.info("Checking condition: {} {} {}", field, operator, expectedValue);
        log.info("Actual value: {}", actualValue);
        
        boolean result = evaluateCondition(actualValue, operator, expectedValue);
        
        Map<String, Object> output = new HashMap<>();
        output.put("conditionResult", result);
        output.put("field", field);
        output.put("actualValue", actualValue);
        output.put("expectedValue", expectedValue);
        
        return ExecutionResult.success(output);
    }

    private ExecutionResult handleMultiBranch(NodeConfig config, ExecutionContext context) {
        // Similar to field_check but with multiple conditions
        return handleFieldCheck(config, context);
    }

    private ExecutionResult handleSwitch(NodeConfig config, ExecutionContext context) {
        // Switch case logic
        return handleFieldCheck(config, context);
    }

    private ExecutionResult handleCompareFields(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String field1 = (String) nodeConfig.get("field1");
        String field2 = (String) nodeConfig.get("field2");
        String operator = (String) nodeConfig.get("operator");
        
        Object value1 = resolveFieldValue(field1, context);
        Object value2 = resolveFieldValue(field2, context);
        
        boolean result = evaluateCondition(value1, operator, value2);
        
        Map<String, Object> output = new HashMap<>();
        output.put("conditionResult", result);
        
        return ExecutionResult.success(output);
    }

    private ExecutionResult handleFormula(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String formula = (String) nodeConfig.get("formula");
        
        log.info("Evaluating formula: {}", formula);
        
        try {
            // Validate formula
            if (!formulaEngine.validateFormula(formula)) {
                return ExecutionResult.failed("Invalid formula syntax: " + formula);
            }
            
            // Evaluate formula with context variables
            Object result = formulaEngine.evaluateFormula(formula, context.getVariables());
            
            // Store result in context
            String resultVariable = (String) nodeConfig.getOrDefault("resultVariable", "formulaResult");
            context.setVariable(resultVariable, result);
            
            // Determine boolean result for branching
            boolean conditionResult;
            if (result instanceof Boolean) {
                conditionResult = (Boolean) result;
            } else if (result instanceof Number) {
                conditionResult = ((Number) result).doubleValue() != 0;
            } else if (result instanceof String) {
                conditionResult = !result.toString().isEmpty();
            } else {
                conditionResult = result != null;
            }
            
            Map<String, Object> output = new HashMap<>();
            output.put("conditionResult", conditionResult);
            output.put("formulaResult", result);
            output.put("formula", formula);
            
            log.info("Formula evaluated successfully. Result: {}", result);
            return ExecutionResult.success(output);
            
        } catch (Exception e) {
            log.error("Formula evaluation failed: {}", formula, e);
            return ExecutionResult.failed("Formula evaluation failed: " + e.getMessage());
        }
    }

    /**
     * Resolve field value from context
     */
    private Object resolveFieldValue(String field, ExecutionContext context) {
        String[] parts = field.split("\\.");
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
     * Evaluate condition based on operator
     */
    private boolean evaluateCondition(Object actual, String operator, Object expected) {
        if (actual == null) {
            return "is_null".equals(operator) || "is_empty".equals(operator);
        }

        switch (operator) {
            case "equals":
            case "==":
                return actual.equals(expected);
            
            case "not_equals":
            case "!=":
                return !actual.equals(expected);
            
            case "contains":
                return actual.toString().contains(expected.toString());
            
            case "starts_with":
                return actual.toString().startsWith(expected.toString());
            
            case "ends_with":
                return actual.toString().endsWith(expected.toString());
            
            case "greater_than":
            case ">":
                return compareNumbers(actual, expected) > 0;
            
            case "less_than":
            case "<":
                return compareNumbers(actual, expected) < 0;
            
            case "greater_than_or_equal":
            case ">=":
                return compareNumbers(actual, expected) >= 0;
            
            case "less_than_or_equal":
            case "<=":
                return compareNumbers(actual, expected) <= 0;
            
            case "is_null":
                return actual == null;
            
            case "is_not_null":
                return actual != null;
            
            case "is_empty":
                return actual.toString().isEmpty();
            
            case "is_not_empty":
                return !actual.toString().isEmpty();
            
            default:
                log.warn("Unknown operator: {}", operator);
                return false;
        }
    }

    /**
     * Compare numbers
     */
    private int compareNumbers(Object actual, Object expected) {
        double actualNum = Double.parseDouble(actual.toString());
        double expectedNum = Double.parseDouble(expected.toString());
        return Double.compare(actualNum, expectedNum);
    }
}
