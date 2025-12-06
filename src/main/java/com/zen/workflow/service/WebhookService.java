package com.zen.workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for making HTTP requests and webhook calls in workflows
 * Supports REST API calls with various authentication methods
 */
@Slf4j
@Service
public class WebhookService {

    @Value("${workflow.webhook.enabled:true}")
    private boolean webhookEnabled;

    @Value("${workflow.webhook.timeout:30000}")
    private int timeout;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Make HTTP request
     */
    public Map<String, Object> makeRequest(String url, String method, Map<String, Object> body,
                                           Map<String, String> headers, String authType,
                                           Map<String, String> authConfig) {
        if (!webhookEnabled) {
            log.warn("Webhook service is disabled. Request not sent to: {}", url);
            return Map.of("success", false, "reason", "Webhook service disabled");
        }

        try {
            log.info("Making {} request to: {}", method, url);

            // Build headers
            HttpHeaders httpHeaders = new HttpHeaders();
            if (headers != null) {
                headers.forEach(httpHeaders::add);
            }

            // Add authentication
            addAuthentication(httpHeaders, authType, authConfig);

            // Set content type if not specified
            if (!httpHeaders.containsKey(HttpHeaders.CONTENT_TYPE)) {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            }

            // Build request entity
            HttpEntity<Object> requestEntity = new HttpEntity<>(body, httpHeaders);

            // Make request
            HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                httpMethod,
                requestEntity,
                String.class
            );

            // Build response
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("statusCode", response.getStatusCodeValue());
            result.put("body", response.getBody());
            result.put("headers", response.getHeaders().toSingleValueMap());

            log.info("Request completed successfully. Status: {}", response.getStatusCodeValue());
            return result;

        } catch (Exception e) {
            log.error("Request failed to: {}", url, e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());
            
            return result;
        }
    }

    /**
     * Make GET request
     */
    public Map<String, Object> get(String url, Map<String, String> headers, String authType,
                                   Map<String, String> authConfig) {
        return makeRequest(url, "GET", null, headers, authType, authConfig);
    }

    /**
     * Make POST request
     */
    public Map<String, Object> post(String url, Map<String, Object> body, Map<String, String> headers,
                                    String authType, Map<String, String> authConfig) {
        return makeRequest(url, "POST", body, headers, authType, authConfig);
    }

    /**
     * Make PUT request
     */
    public Map<String, Object> put(String url, Map<String, Object> body, Map<String, String> headers,
                                   String authType, Map<String, String> authConfig) {
        return makeRequest(url, "PUT", body, headers, authType, authConfig);
    }

    /**
     * Make DELETE request
     */
    public Map<String, Object> delete(String url, Map<String, String> headers, String authType,
                                      Map<String, String> authConfig) {
        return makeRequest(url, "DELETE", null, headers, authType, authConfig);
    }

    /**
     * Make PATCH request
     */
    public Map<String, Object> patch(String url, Map<String, Object> body, Map<String, String> headers,
                                     String authType, Map<String, String> authConfig) {
        return makeRequest(url, "PATCH", body, headers, authType, authConfig);
    }

    /**
     * Send webhook (POST request with JSON body)
     */
    public Map<String, Object> sendWebhook(String url, Map<String, Object> payload) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        
        return post(url, payload, headers, "NONE", null);
    }

    /**
     * Add authentication to headers
     */
    private void addAuthentication(HttpHeaders headers, String authType, Map<String, String> authConfig) {
        if (authType == null || "NONE".equalsIgnoreCase(authType)) {
            return;
        }

        if (authConfig == null) {
            log.warn("Authentication type specified but no config provided");
            return;
        }

        switch (authType.toUpperCase()) {
            case "BASIC":
                String username = authConfig.get("username");
                String password = authConfig.get("password");
                if (username != null && password != null) {
                    String auth = username + ":" + password;
                    String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
                    headers.add("Authorization", "Basic " + encodedAuth);
                }
                break;

            case "BEARER":
                String token = authConfig.get("token");
                if (token != null) {
                    headers.add("Authorization", "Bearer " + token);
                }
                break;

            case "API_KEY":
                String apiKey = authConfig.get("apiKey");
                String headerName = authConfig.getOrDefault("headerName", "X-API-Key");
                if (apiKey != null) {
                    headers.add(headerName, apiKey);
                }
                break;

            case "CUSTOM":
                String customHeader = authConfig.get("headerName");
                String customValue = authConfig.get("headerValue");
                if (customHeader != null && customValue != null) {
                    headers.add(customHeader, customValue);
                }
                break;

            default:
                log.warn("Unknown authentication type: {}", authType);
        }
    }

    /**
     * Parse JSON response
     */
    public Map<String, Object> parseJsonResponse(String jsonString) {
        try {
            // In production, use Jackson ObjectMapper
            // For now, return as-is
            Map<String, Object> result = new HashMap<>();
            result.put("raw", jsonString);
            return result;
        } catch (Exception e) {
            log.error("Failed to parse JSON response", e);
            return Map.of("error", "Failed to parse JSON: " + e.getMessage());
        }
    }

    /**
     * Build query string from parameters
     */
    public String buildQueryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        StringBuilder query = new StringBuilder("?");
        params.forEach((key, value) -> {
            if (query.length() > 1) {
                query.append("&");
            }
            query.append(key).append("=").append(value);
        });

        return query.toString();
    }

    /**
     * Check if webhook service is available
     */
    public boolean isWebhookServiceAvailable() {
        return webhookEnabled;
    }
}
