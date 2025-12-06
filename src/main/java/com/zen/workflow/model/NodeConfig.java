package com.zen.workflow.model;

import lombok.Data;
import java.util.Map;

@Data
public class NodeConfig {
    private String id;
    private String type;
    private String subtype;
    private String label;
    private Map<String, Object> config;
    private Map<String, Object> connections;
    private Map<String, Object> position;
}
