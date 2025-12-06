package com.zen.workflow.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class NodeHandlerFactory {

    private final Map<String, NodeHandler> handlers = new HashMap<>();

    @Autowired
    public NodeHandlerFactory(
        RecordTriggerHandler recordTriggerHandler,
        ConditionEvaluator conditionEvaluator,
        CRUDHandler crudHandler,
        EmailHandler emailHandler,
        TaskManagementHandler taskManagementHandler,
        ApprovalHandler approvalHandler,
        DelayHandler delayHandler,
        IntegrationHandler integrationHandler,
        ListManagementHandler listManagementHandler,
        ErrorHandler errorHandler,
        CollectionHandler collectionHandler,
        ScheduledTriggerHandler scheduledTriggerHandler,
        EventTriggerHandler eventTriggerHandler
    ) {
        // Register all handlers
        handlers.put("trigger", recordTriggerHandler);
        handlers.put("condition", conditionEvaluator);
        handlers.put("data", crudHandler);
        handlers.put("communication", emailHandler);
        handlers.put("task", taskManagementHandler);
        handlers.put("approval", approvalHandler);
        handlers.put("delay", delayHandler);
        handlers.put("integration", integrationHandler);
        handlers.put("list", listManagementHandler);
        handlers.put("error", errorHandler);
        handlers.put("collection", collectionHandler);
        handlers.put("scheduled", scheduledTriggerHandler);
        handlers.put("event", eventTriggerHandler);
    }

    public NodeHandler getHandler(String nodeType) {
        NodeHandler handler = handlers.get(nodeType);
        if (handler == null) {
            throw new IllegalArgumentException("No handler found for node type: " + nodeType);
        }
        return handler;
    }
}
