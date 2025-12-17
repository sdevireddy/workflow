package com.zen.workflow.dto;

import lombok.Data;
import java.util.Map;

@Data
public class NodeDefinitionDTO {
    private Long id;
    private String type;
    private String subtype;
    private String name;
    private String description;
    private String icon;
    private String category;
    private Map<String, Object> defaultConfig;
    private Map<String, Object> schema;
    private Boolean active;
}
