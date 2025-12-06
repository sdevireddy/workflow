package com.zen.workflow.handler;

import com.zen.workflow.engine.VariableResolver;
import com.zen.workflow.engine.WorkflowExecutionEngine;
import com.zen.workflow.model.ExecutionContext;
import com.zen.workflow.model.ExecutionResult;
import com.zen.workflow.model.NodeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles collection operations:
 * - loop: Iterate through collection and execute child nodes
 * - filter_collection: Filter collection by criteria
 * - sort_collection: Sort collection by field
 */
@Slf4j
@Component
public class CollectionHandler implements NodeHandler {

    @Autowired
    private VariableResolver variableResolver;

    @Autowired
    private WorkflowExecutionEngine executionEngine;

    @Override
    public ExecutionResult execute(NodeConfig config, ExecutionContext context) {
        String subtype = config.getSubtype();
        log.info("Executing Collection Operation: {}", subtype);

        try {
            switch (subtype) {
                case "loop":
                    return handleLoop(config, context);
                case "filter_collection":
                    return handleFilter(config, context);
                case "sort_collection":
                    return handleSort(config, context);
                default:
                    return ExecutionResult.failed("Unknown collection operation: " + subtype);
            }
        } catch (Exception e) {
            log.error("Collection operation failed", e);
            return ExecutionResult.failed(e.getMessage());
        }
    }

    /**
     * Loop through collection and execute child nodes for each item
     */
    private ExecutionResult handleLoop(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        // Get collection to iterate
        String collectionVar = (String) nodeConfig.get("collection");
        Object collectionObj = context.getVariable(collectionVar);
        
        if (collectionObj == null) {
            log.warn("Collection variable '{}' is null", collectionVar);
            return ExecutionResult.success(Map.of("iterations", 0, "results", new ArrayList<>()));
        }
        
        List<?> collection;
        if (collectionObj instanceof List) {
            collection = (List<?>) collectionObj;
        } else if (collectionObj instanceof Collection) {
            collection = new ArrayList<>((Collection<?>) collectionObj);
        } else {
            return ExecutionResult.failed("Variable '" + collectionVar + "' is not a collection");
        }
        
        // Get loop configuration
        String itemVar = (String) nodeConfig.getOrDefault("itemVariable", "currentItem");
        String indexVar = (String) nodeConfig.getOrDefault("indexVariable", "currentIndex");
        Integer maxIterations = (Integer) nodeConfig.get("maxIterations");
        
        log.info("Looping through {} items in collection '{}'", collection.size(), collectionVar);
        
        List<Map<String, Object>> results = new ArrayList<>();
        int iterations = 0;
        
        for (int i = 0; i < collection.size(); i++) {
            // Check max iterations
            if (maxIterations != null && iterations >= maxIterations) {
                log.info("Reached max iterations: {}", maxIterations);
                break;
            }
            
            Object item = collection.get(i);
            
            // Set loop variables in context
            context.setVariable(itemVar, item);
            context.setVariable(indexVar, i);
            
            log.debug("Loop iteration {}: item = {}", i, item);
            
            // Execute child nodes for this item
            // Note: Child node execution would be handled by WorkflowExecutionEngine
            // For now, we'll store the item and index
            Map<String, Object> iterationResult = new HashMap<>();
            iterationResult.put("index", i);
            iterationResult.put("item", item);
            iterationResult.put("success", true);
            
            results.add(iterationResult);
            iterations++;
        }
        
        // Store results in context
        context.setVariable("loopResults", results);
        context.setVariable("loopIterations", iterations);
        
        Map<String, Object> output = new HashMap<>();
        output.put("iterations", iterations);
        output.put("results", results);
        output.put("completed", true);
        
        log.info("Loop completed: {} iterations", iterations);
        return ExecutionResult.success(output);
    }

    /**
     * Filter collection by criteria
     */
    private ExecutionResult handleFilter(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        // Get collection to filter
        String collectionVar = (String) nodeConfig.get("collection");
        Object collectionObj = context.getVariable(collectionVar);
        
        if (collectionObj == null) {
            log.warn("Collection variable '{}' is null", collectionVar);
            return ExecutionResult.success(Map.of("filtered", new ArrayList<>(), "count", 0));
        }
        
        List<?> collection;
        if (collectionObj instanceof List) {
            collection = (List<?>) collectionObj;
        } else if (collectionObj instanceof Collection) {
            collection = new ArrayList<>((Collection<?>) collectionObj);
        } else {
            return ExecutionResult.failed("Variable '" + collectionVar + "' is not a collection");
        }
        
        // Get filter criteria
        String filterField = (String) nodeConfig.get("field");
        String operator = (String) nodeConfig.getOrDefault("operator", "equals");
        String filterValue = variableResolver.resolve((String) nodeConfig.get("value"), context);
        
        log.info("Filtering {} items by {}.{} {} {}", 
            collection.size(), collectionVar, filterField, operator, filterValue);
        
        List<Object> filtered = new ArrayList<>();
        
        for (Object item : collection) {
            try {
                Object fieldValue = getFieldValue(item, filterField);
                
                if (matchesCriteria(fieldValue, operator, filterValue)) {
                    filtered.add(item);
                }
            } catch (Exception e) {
                log.warn("Error filtering item: {}", e.getMessage());
            }
        }
        
        // Store filtered results
        String outputVar = (String) nodeConfig.getOrDefault("outputVariable", "filteredResults");
        context.setVariable(outputVar, filtered);
        
        Map<String, Object> output = new HashMap<>();
        output.put("filtered", filtered);
        output.put("count", filtered.size());
        output.put("originalCount", collection.size());
        
        log.info("Filter completed: {} items matched out of {}", filtered.size(), collection.size());
        return ExecutionResult.success(output);
    }

    /**
     * Sort collection by field
     */
    private ExecutionResult handleSort(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        // Get collection to sort
        String collectionVar = (String) nodeConfig.get("collection");
        Object collectionObj = context.getVariable(collectionVar);
        
        if (collectionObj == null) {
            log.warn("Collection variable '{}' is null", collectionVar);
            return ExecutionResult.success(Map.of("sorted", new ArrayList<>(), "count", 0));
        }
        
        List<?> collection;
        if (collectionObj instanceof List) {
            collection = new ArrayList<>((List<?>) collectionObj);
        } else if (collectionObj instanceof Collection) {
            collection = new ArrayList<>((Collection<?>) collectionObj);
        } else {
            return ExecutionResult.failed("Variable '" + collectionVar + "' is not a collection");
        }
        
        // Get sort configuration
        String sortField = (String) nodeConfig.get("field");
        String sortOrder = (String) nodeConfig.getOrDefault("order", "asc");
        
        log.info("Sorting {} items by {} ({})", collection.size(), sortField, sortOrder);
        
        try {
            // Sort the collection
            List<Object> sorted = collection.stream()
                .sorted((a, b) -> {
                    try {
                        Object valueA = getFieldValue(a, sortField);
                        Object valueB = getFieldValue(b, sortField);
                        
                        int comparison = compareValues(valueA, valueB);
                        
                        return "desc".equalsIgnoreCase(sortOrder) ? -comparison : comparison;
                    } catch (Exception e) {
                        log.warn("Error comparing items: {}", e.getMessage());
                        return 0;
                    }
                })
                .collect(Collectors.toList());
            
            // Store sorted results
            String outputVar = (String) nodeConfig.getOrDefault("outputVariable", "sortedResults");
            context.setVariable(outputVar, sorted);
            
            Map<String, Object> output = new HashMap<>();
            output.put("sorted", sorted);
            output.put("count", sorted.size());
            output.put("sortField", sortField);
            output.put("sortOrder", sortOrder);
            
            log.info("Sort completed: {} items sorted by {}", sorted.size(), sortField);
            return ExecutionResult.success(output);
            
        } catch (Exception e) {
            log.error("Sort failed", e);
            return ExecutionResult.failed("Sort failed: " + e.getMessage());
        }
    }

    // Helper methods

    /**
     * Get field value from object using reflection or map access
     */
    private Object getFieldValue(Object obj, String fieldName) throws Exception {
        if (obj == null) {
            return null;
        }
        
        // Handle Map
        if (obj instanceof Map) {
            return ((Map<?, ?>) obj).get(fieldName);
        }
        
        // Handle object with reflection
        try {
            java.lang.reflect.Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                return field.get(obj);
            }
        } catch (Exception e) {
            log.debug("Could not access field '{}' via reflection", fieldName);
        }
        
        // Try getter method
        try {
            String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            java.lang.reflect.Method getter = obj.getClass().getMethod(getterName);
            return getter.invoke(obj);
        } catch (Exception e) {
            log.debug("Could not access field '{}' via getter", fieldName);
        }
        
        return null;
    }

    /**
     * Find field in class hierarchy
     */
    private java.lang.reflect.Field findField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return findField(clazz.getSuperclass(), fieldName);
            }
            return null;
        }
    }

    /**
     * Check if value matches criteria
     */
    private boolean matchesCriteria(Object fieldValue, String operator, String filterValue) {
        if (fieldValue == null) {
            return "is_null".equals(operator);
        }
        
        String fieldStr = fieldValue.toString();
        
        switch (operator) {
            case "equals":
                return fieldStr.equals(filterValue);
            case "not_equals":
                return !fieldStr.equals(filterValue);
            case "contains":
                return fieldStr.toLowerCase().contains(filterValue.toLowerCase());
            case "not_contains":
                return !fieldStr.toLowerCase().contains(filterValue.toLowerCase());
            case "starts_with":
                return fieldStr.toLowerCase().startsWith(filterValue.toLowerCase());
            case "ends_with":
                return fieldStr.toLowerCase().endsWith(filterValue.toLowerCase());
            case "greater_than":
                return compareNumeric(fieldValue, filterValue) > 0;
            case "less_than":
                return compareNumeric(fieldValue, filterValue) < 0;
            case "greater_than_or_equal":
                return compareNumeric(fieldValue, filterValue) >= 0;
            case "less_than_or_equal":
                return compareNumeric(fieldValue, filterValue) <= 0;
            case "is_null":
                return false; // Already handled above
            case "is_not_null":
                return true;
            default:
                log.warn("Unknown operator: {}", operator);
                return false;
        }
    }

    /**
     * Compare two values
     */
    @SuppressWarnings("unchecked")
    private int compareValues(Object a, Object b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        
        // Try numeric comparison
        try {
            double numA = Double.parseDouble(a.toString());
            double numB = Double.parseDouble(b.toString());
            return Double.compare(numA, numB);
        } catch (NumberFormatException e) {
            // Not numeric, use string comparison
        }
        
        // Try Comparable
        if (a instanceof Comparable && b instanceof Comparable) {
            try {
                return ((Comparable<Object>) a).compareTo(b);
            } catch (Exception e) {
                log.debug("Could not compare as Comparable");
            }
        }
        
        // Fallback to string comparison
        return a.toString().compareTo(b.toString());
    }

    /**
     * Compare numeric values
     */
    private int compareNumeric(Object a, String b) {
        try {
            double numA = Double.parseDouble(a.toString());
            double numB = Double.parseDouble(b);
            return Double.compare(numA, numB);
        } catch (NumberFormatException e) {
            log.warn("Could not compare as numbers: {} and {}", a, b);
            return 0;
        }
    }
}
