package com.zen.workflow.dto;

import lombok.Data;
import java.util.Map;

@Data
public class IntegrationDTO {
    private Long id;
    private String name;
    private String type; // WEBHOOK, API, CUSTOM_FUNCTION, EXTERNAL_SERVICE
    private String url;
    private String method;
    private Map<String, Object> configuration;
    private Map<String, String> headers;
    private String authType;
    private Map<String, String> authConfig;
    private Boolean active;
}
