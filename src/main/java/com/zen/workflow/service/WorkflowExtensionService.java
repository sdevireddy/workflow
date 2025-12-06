package com.zen.workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Service for workflow extensions:
 * - Custom function registry
 * - Subflow execution
 * - External service registry
 */
@Slf4j
@Service
public class WorkflowExtensionService {

    private final Map<String, CustomFunction> customFunctions = new ConcurrentHashMap<>();
    private final Map<String, ExternalService> externalServices = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private com.zen.workflow.service.WorkflowExecutionService workflowExecutionService;

    /**
     * Register custom function
     */
    public void registerCustomFunction(String name, CustomFunction function) {
        log.info("Registering custom function: {}", name);
        customFunctions.put(name, function);
    }

    /**
     * Execute custom function
     */
    public Object executeCustomFunction(String functionName, Map<String, Object> parameters) {
        log.info("Executing custom function: {} with parameters: {}", functionName, parameters);

        CustomFunction function = customFunctions.get(functionName);
        if (function == null) {
            throw new RuntimeException("Custom function not found: " + functionName);
        }

        try {
            Object result = function.execute(parameters);
            log.info("Custom function executed successfully. Result: {}", result);
            return result;

        } catch (Exception e) {
            log.error("Custom function execution failed: {}", functionName, e);
            throw new RuntimeException("Custom function execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute subflow (call another workflow)
     */
    public Map<String, Object> executeSubflow(Long subflowId, Map<String, Object> inputData) {
        log.info("Executing subflow: {} with input: {}", subflowId, inputData);

        try {
            // In production, call WorkflowExecutionService
            // For now, simulate subflow execution
            Map<String, Object> result = new HashMap<>();
            result.put("subflowId", subflowId);
            result.put("status", "COMPLETED");
            result.put("output", new HashMap<>());
            result.put("executedAt", new Date());

            log.info("Subflow executed successfully");
            return result;

        } catch (Exception e) {
            log.error("Subflow execution failed: {}", subflowId, e);
            throw new RuntimeException("Subflow execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Register external service
     */
    public void registerExternalService(String serviceName, ExternalService service) {
        log.info("Registering external service: {}", serviceName);
        externalServices.put(serviceName, service);
    }

    /**
     * Call external service
     */
    public Object callExternalService(String serviceName, String method, Map<String, Object> parameters) {
        log.info("Calling external service: {}.{} with parameters: {}", serviceName, method, parameters);

        ExternalService service = externalServices.get(serviceName);
        if (service == null) {
            throw new RuntimeException("External service not found: " + serviceName);
        }

        try {
            Object result = service.call(method, parameters);
            log.info("External service called successfully. Result: {}", result);
            return result;

        } catch (Exception e) {
            log.error("External service call failed: {}.{}", serviceName, method, e);
            throw new RuntimeException("External service call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get list of registered custom functions
     */
    public List<String> getRegisteredFunctions() {
        return new ArrayList<>(customFunctions.keySet());
    }

    /**
     * Get list of registered external services
     */
    public List<String> getRegisteredServices() {
        return new ArrayList<>(externalServices.keySet());
    }

    /**
     * Check if custom function exists
     */
    public boolean hasCustomFunction(String functionName) {
        return customFunctions.containsKey(functionName);
    }

    /**
     * Check if external service exists
     */
    public boolean hasExternalService(String serviceName) {
        return externalServices.containsKey(serviceName);
    }

    /**
     * Unregister custom function
     */
    public void unregisterCustomFunction(String functionName) {
        log.info("Unregistering custom function: {}", functionName);
        customFunctions.remove(functionName);
    }

    /**
     * Unregister external service
     */
    public void unregisterExternalService(String serviceName) {
        log.info("Unregistering external service: {}", serviceName);
        externalServices.remove(serviceName);
    }

    // Built-in custom functions

    /**
     * Register built-in custom functions
     */
    public void registerBuiltInFunctions() {
        // String manipulation
        registerCustomFunction("capitalize", params -> {
            String text = (String) params.get("text");
            return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
        });

        // Number formatting
        registerCustomFunction("formatCurrency", params -> {
            Double amount = ((Number) params.get("amount")).doubleValue();
            String currency = (String) params.getOrDefault("currency", "USD");
            return String.format("%s %.2f", currency, amount);
        });

        // Date formatting
        registerCustomFunction("formatDate", params -> {
            String date = (String) params.get("date");
            String format = (String) params.getOrDefault("format", "yyyy-MM-dd");
            // In production, use DateTimeFormatter
            return date;
        });

        // Array operations
        registerCustomFunction("arrayLength", params -> {
            List<?> array = (List<?>) params.get("array");
            return array != null ? array.size() : 0;
        });

        // Object operations
        registerCustomFunction("getProperty", params -> {
            Map<String, Object> object = (Map<String, Object>) params.get("object");
            String property = (String) params.get("property");
            return object != null ? object.get(property) : null;
        });

        log.info("Built-in custom functions registered");
    }

    // Interfaces

    @FunctionalInterface
    public interface CustomFunction {
        Object execute(Map<String, Object> parameters) throws Exception;
    }

    @FunctionalInterface
    public interface ExternalService {
        Object call(String method, Map<String, Object> parameters) throws Exception;
    }
}
