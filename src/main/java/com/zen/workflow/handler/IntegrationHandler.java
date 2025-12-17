package com.zen.workflow.handler;

import com.zen.workflow.engine.VariableResolver;
import com.zen.workflow.model.ExecutionContext;
import com.zen.workflow.model.ExecutionResult;
import com.zen.workflow.model.NodeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles all integration operations:
 * - webhook, api_call, custom_function, call_subflow, external_service
 */
@Slf4j
@Component
public class IntegrationHandler implements NodeHandler {

    @Autowired
    private VariableResolver variableResolver;
    
    @Autowired
    private com.zen.workflow.service.WebhookService webhookService;
    
    @Autowired
    private com.zen.workflow.service.WorkflowExtensionService extensionService;

    @Override
    public ExecutionResult execute(NodeConfig config, ExecutionContext context) {
        String subtype = config.getSubtype();
        log.info("Executing Integration: {}", subtype);

        try {
            switch (subtype) {
                case "webhook":
                case "api_call":
                    return handleWebhook(config, context);
                case "custom_function":
                    return handleCustomFunction(config, context);
                case "call_subflow":
                    return handleCallSubflow(config, context);
                case "external_service":
                    return handleExternalService(config, context);
                default:
                    return ExecutionResult.failed("Unknown integration type: " + subtype);
            }
        } catch (Exception e) {
            log.error("Integration failed", e);
            return ExecutionResult.failed(e.getMessage());
        }
    }

    private ExecutionResult handleWebhook(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String url = variableResolver.resolve((String) nodeConfig.get("url"), context);
        String method = (String) nodeConfig.getOrDefault("method", "POST");
        Map<String, Object> body = (Map<String, Object>) nodeConfig.get("body");
        Map<String, String> headers = (Map<String, String>) nodeConfig.get("headers");
        String authType = (String) nodeConfig.getOrDefault("authType", "NONE");
        Map<String, String> authConfig = (Map<String, String>) nodeConfig.get("authConfig");
        
        // Resolve variables in body
        if (body != null) {
            body = variableResolver.resolveMap(body, context);
        }
        
        // Resolve variables in headers
        if (headers != null) {
            Map<String, String> resolvedHeaders = new HashMap<>();
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                resolvedHeaders.put(entry.getKey(), variableResolver.resolve(entry.getValue(), context));
            }
            headers = resolvedHeaders;
        }
        
        log.info("Calling webhook: {} {}", method, url);
        
        try {
            // Check if webhook service is available
            if (!webhookService.isWebhookServiceAvailable()) {
                log.warn("Webhook service not available.");
                return ExecutionResult.success(Map.of(
                    "success", false,
                    "reason", "Webhook service not configured"
                ));
            }
            
            // Make HTTP request
            Map<String, Object> response = webhookService.makeRequest(
                url, method, body, headers, authType, authConfig
            );
            
            // Store response in context
            context.setVariable("webhookResponse", response);
            context.setVariable("webhookStatusCode", response.get("statusCode"));
            context.setVariable("webhookBody", response.get("body"));
            
            log.info("Webhook call completed. Success: {}", response.get("success"));
            return ExecutionResult.success(response);
            
        } catch (Exception e) {
            log.error("Webhook call failed", e);
            return ExecutionResult.failed("Webhook call failed: " + e.getMessage());
        }
    }

    private ExecutionResult handleCustomFunction(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String functionName = (String) nodeConfig.get("functionName");
        Map<String, Object> parameters = (Map<String, Object>) nodeConfig.get("parameters");
        
        if (parameters != null) {
            parameters = variableResolver.resolveMap(parameters, context);
        } else {
            parameters = new HashMap<>();
        }
        
        log.info("Executing custom function: {}", functionName);
        
        try {
            if (!extensionService.hasCustomFunction(functionName)) {
                return ExecutionResult.failed("Custom function not found: " + functionName);
            }
            
            Object result = extensionService.executeCustomFunction(functionName, parameters);
            
            // Store result in context
            String resultVariable = (String) nodeConfig.getOrDefault("resultVariable", "functionResult");
            context.setVariable(resultVariable, result);
            
            Map<String, Object> output = new HashMap<>();
            output.put("functionName", functionName);
            output.put("result", result);
            output.put("success", true);
            
            log.info("Custom function executed successfully");
            return ExecutionResult.success(output);
            
        } catch (Exception e) {
            log.error("Custom function execution failed", e);
            return ExecutionResult.failed("Custom function failed: " + e.getMessage());
        }
    }
    
    private ExecutionResult handleCallSubflow(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        Long subflowId = Long.parseLong(variableResolver.resolve((String) nodeConfig.get("subflowId"), context));
        Map<String, Object> inputData = (Map<String, Object>) nodeConfig.get("inputData");
        Boolean waitForCompletion = (Boolean) nodeConfig.getOrDefault("waitForCompletion", true);
        
        if (inputData != null) {
            inputData = variableResolver.resolveMap(inputData, context);
        } else {
            inputData = new HashMap<>();
        }
        
        log.info("Calling subflow: {} with wait={}", subflowId, waitForCompletion);
        
        try {
            Map<String, Object> result = extensionService.executeSubflow(subflowId, inputData);
            
            // Store subflow output in context
            context.setVariable("subflowResult", result);
            context.setVariable("subflowOutput", result.get("output"));
            
            Map<String, Object> output = new HashMap<>();
            output.put("subflowId", subflowId);
            output.put("status", result.get("status"));
            output.put("output", result.get("output"));
            output.put("success", true);
            
            if (waitForCompletion) {
                log.info("Subflow completed successfully");
            } else {
                log.info("Subflow started asynchronously");
                output.put("async", true);
            }
            
            return ExecutionResult.success(output);
            
        } catch (Exception e) {
            log.error("Subflow execution failed", e);
            return ExecutionResult.failed("Subflow execution failed: " + e.getMessage());
        }
    }
    
    private ExecutionResult handleExternalService(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String serviceName = (String) nodeConfig.get("serviceName");
        String method = (String) nodeConfig.get("method");
        Map<String, Object> parameters = (Map<String, Object>) nodeConfig.get("parameters");
        
        if (parameters != null) {
            parameters = variableResolver.resolveMap(parameters, context);
        } else {
            parameters = new HashMap<>();
        }
        
        log.info("Calling external service: {}.{}", serviceName, method);
        
        try {
            if (!extensionService.hasExternalService(serviceName)) {
                return ExecutionResult.failed("External service not found: " + serviceName);
            }
            
            Object result = extensionService.callExternalService(serviceName, method, parameters);
            
            // Store result in context
            String resultVariable = (String) nodeConfig.getOrDefault("resultVariable", "serviceResult");
            context.setVariable(resultVariable, result);
            
            Map<String, Object> output = new HashMap<>();
            output.put("serviceName", serviceName);
            output.put("method", method);
            output.put("result", result);
            output.put("success", true);
            
            log.info("External service called successfully");
            return ExecutionResult.success(output);
            
        } catch (Exception e) {
            log.error("External service call failed", e);
            return ExecutionResult.failed("External service call failed: " + e.getMessage());
        }
    }
}
