# Workflow Trigger Integration Guide

## How to Trigger Workflows from CRM Service

When a record is created/updated in CRM service (e.g., Lead, Contact, Deal), you want to automatically trigger workflows.

## Architecture Options

### Option 1: Direct REST Call (Simple & Synchronous) ⭐ RECOMMENDED FOR NOW
```
CRM Service → HTTP POST → Workflow Service
```

**Pros:**
- Simple to implement
- Immediate execution
- Easy to debug
- No additional infrastructure

**Cons:**
- Tight coupling
- Blocks CRM operation if workflow service is slow
- No retry mechanism (unless added)

### Option 2: Spring Events (Decoupled & Asynchronous)
```
CRM Service → Spring Event → Event Listener → Workflow Service
```

**Pros:**
- Loose coupling
- Async execution
- Multiple listeners possible
- Clean separation

**Cons:**
- Only works within same JVM (not microservices)
- Need shared event bus for distributed systems

### Option 3: Message Queue (Production-Grade)
```
CRM Service → RabbitMQ/Kafka → Workflow Service
```

**Pros:**
- Fully decoupled
- Guaranteed delivery
- Retry & dead letter queues
- Scalable

**Cons:**
- Requires message broker infrastructure
- More complex setup
- Eventual consistency

---

## Implementation: Option 1 - Direct REST Call

This is the simplest and most practical for your current setup.

### Step 1: Create Workflow Trigger Client in CRM Service

**File: `notify/src/main/java/com/zen/crm/client/WorkflowClient.java`**

```java
package com.zen.crm.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class WorkflowClient {

    private final RestTemplate restTemplate;
    
    @Value("${workflow.service.url:http://localhost:8099}")
    private String workflowServiceUrl;

    public WorkflowClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Trigger workflows for a specific module and event
     */
    public void triggerWorkflows(String tenantId, String moduleType, String triggerType, Map<String, Object> recordData) {
        try {
            String url = workflowServiceUrl + "/api/workflows/trigger";
            
            Map<String, Object> request = new HashMap<>();
            request.put("moduleType", moduleType);
            request.put("triggerType", triggerType);
            request.put("recordData", recordData);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Tenant-ID", tenantId);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            log.info("🔄 Triggering workflows for {}.{} in tenant {}", moduleType, triggerType, tenantId);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Workflows triggered successfully: {}", response.getBody());
            } else {
                log.warn("⚠️ Workflow trigger returned non-success status: {}", response.getStatusCode());
            }
            
        } catch (Exception e) {
            // Don't fail the main operation if workflow trigger fails
            log.error("❌ Failed to trigger workflows: {}", e.getMessage(), e);
        }
    }

    /**
     * Trigger workflows asynchronously (fire and forget)
     */
    public void triggerWorkflowsAsync(String tenantId, String moduleType, String triggerType, Map<String, Object> recordData) {
        // Run in separate thread to not block main operation
        new Thread(() -> triggerWorkflows(tenantId, moduleType, triggerType, recordData)).start();
    }
}
```

### Step 2: Create RestTemplate Bean

**File: `notify/src/main/java/com/zen/crm/config/RestTemplateConfig.java`**

```java
package com.zen.crm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

### Step 3: Use in Lead Service (Example)

**File: `notify/src/main/java/com/zen/crm/service/LeadService.java`**

```java
package com.zen.crm.service;

import com.zen.crm.client.WorkflowClient;
import com.zen.entities.tenant.Lead;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class LeadService {

    private final WorkflowClient workflowClient;
    // ... other dependencies

    public LeadService(WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    public Lead createLead(Lead lead, String tenantId) {
        // 1. Save the lead
        Lead savedLead = leadRepository.save(lead);
        log.info("✅ Lead created: {}", savedLead.getId());

        // 2. Trigger workflows asynchronously
        try {
            Map<String, Object> recordData = new HashMap<>();
            recordData.put("leadId", savedLead.getId());
            recordData.put("leadName", savedLead.getFirstName() + " " + savedLead.getLastName());
            recordData.put("email", savedLead.getEmail());
            recordData.put("phone", savedLead.getPhone());
            recordData.put("status", savedLead.getStatus());
            recordData.put("source", savedLead.getSource());
            recordData.put("assignedTo", savedLead.getAssignedTo());
            
            // Trigger ON_CREATE workflows
            workflowClient.triggerWorkflowsAsync(
                tenantId, 
                "LEAD", 
                "ON_CREATE", 
                recordData
            );
        } catch (Exception e) {
            log.error("Failed to trigger workflows for lead {}", savedLead.getId(), e);
            // Don't fail the lead creation
        }

        return savedLead;
    }

    public Lead updateLead(Long id, Lead lead, String tenantId) {
        // 1. Update the lead
        Lead existingLead = leadRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Lead not found"));
        
        // Track what changed
        Map<String, Object> changes = new HashMap<>();
        if (!existingLead.getStatus().equals(lead.getStatus())) {
            changes.put("oldStatus", existingLead.getStatus());
            changes.put("newStatus", lead.getStatus());
        }
        
        // Update fields
        existingLead.setFirstName(lead.getFirstName());
        existingLead.setLastName(lead.getLastName());
        existingLead.setStatus(lead.getStatus());
        // ... other fields
        
        Lead updatedLead = leadRepository.save(existingLead);
        log.info("✅ Lead updated: {}", updatedLead.getId());

        // 2. Trigger workflows
        try {
            Map<String, Object> recordData = new HashMap<>();
            recordData.put("leadId", updatedLead.getId());
            recordData.put("leadName", updatedLead.getFirstName() + " " + updatedLead.getLastName());
            recordData.put("changes", changes);
            
            // Trigger ON_UPDATE workflows
            workflowClient.triggerWorkflowsAsync(
                tenantId, 
                "LEAD", 
                "ON_UPDATE", 
                recordData
            );
            
            // If status changed, trigger ON_STATUS_CHANGE
            if (changes.containsKey("newStatus")) {
                workflowClient.triggerWorkflowsAsync(
                    tenantId, 
                    "LEAD", 
                    "ON_STATUS_CHANGE", 
                    recordData
                );
            }
        } catch (Exception e) {
            log.error("Failed to trigger workflows for lead {}", updatedLead.getId(), e);
        }

        return updatedLead;
    }
}
```

### Step 4: Add Workflow Trigger Endpoint in Workflow Service

**File: `workflow-service/src/main/java/com/zen/workflow/controller/WorkflowTriggerController.java`**

```java
package com.zen.workflow.controller;

import com.zen.workflow.service.WorkflowTriggerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/workflows")
@CrossOrigin(origins = "*")
public class WorkflowTriggerController {

    @Autowired
    private WorkflowTriggerService triggerService;

    /**
     * Trigger workflows based on module and event type
     * POST /api/workflows/trigger
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerWorkflows(
            @RequestBody Map<String, Object> request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        String moduleType = (String) request.get("moduleType");
        String triggerType = (String) request.get("triggerType");
        Map<String, Object> recordData = (Map<String, Object>) request.get("recordData");
        
        log.info("🎯 Workflow trigger request: module={}, trigger={}, tenant={}", 
            moduleType, triggerType, tenantId);

        try {
            int executedCount = triggerService.triggerWorkflows(
                tenantId, moduleType, triggerType, recordData
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("executedWorkflows", executedCount);
            response.put("message", executedCount + " workflow(s) triggered");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to trigger workflows", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
```

### Step 5: Create Workflow Trigger Service

**File: `workflow-service/src/main/java/com/zen/workflow/service/WorkflowTriggerService.java`**

```java
package com.zen.workflow.service;

import com.zen.workflow.engine.WorkflowExecutionEngine;
import com.zen.workflow.model.ExecutionContext;
import com.zen.entities.tenant.Workflow;
import com.zen.workflow.repository.WorkflowRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class WorkflowTriggerService {

    @Autowired
    private WorkflowRepository workflowRepository;
    
    @Autowired
    private WorkflowExecutionEngine executionEngine;

    /**
     * Find and execute all active workflows matching the trigger
     */
    @Async
    public int triggerWorkflows(String tenantId, String moduleType, String triggerType, Map<String, Object> recordData) {
        
        log.info("🔍 Finding workflows: module={}, trigger={}", moduleType, triggerType);
        
        // Find all active workflows for this module and trigger
        List<Workflow> workflows = workflowRepository
            .findByModuleTypeAndTriggerTypeAndIsActiveTrue(moduleType, triggerType);
        
        log.info("📋 Found {} active workflow(s)", workflows.size());
        
        int executedCount = 0;
        
        for (Workflow workflow : workflows) {
            try {
                log.info("▶️ Executing workflow: {} (ID: {})", workflow.getWorkflowName(), workflow.getId());
                
                ExecutionContext context = new ExecutionContext();
                context.setWorkflowId(workflow.getId());
                context.setTenantId(tenantId);
                context.setTriggerData(recordData);
                
                executionEngine.executeWorkflow(workflow.getId(), context);
                executedCount++;
                
                log.info("✅ Workflow executed: {}", workflow.getWorkflowName());
                
            } catch (Exception e) {
                log.error("❌ Failed to execute workflow {}: {}", workflow.getId(), e.getMessage(), e);
                // Continue with other workflows
            }
        }
        
        return executedCount;
    }
}
```

### Step 6: Configuration

**File: `notify/src/main/resources/application.yml`**

```yaml
workflow:
  service:
    url: http://localhost:8099  # Direct to workflow service
    # OR through API Gateway:
    # url: http://localhost:8080/api/workflows
```

---

## Usage Examples

### Example 1: Lead Created
```java
// In LeadController
@PostMapping
public ResponseEntity<Lead> createLead(@RequestBody Lead lead) {
    String tenantId = getTenantId();
    Lead created = leadService.createLead(lead, tenantId);
    // Workflows automatically triggered
    return ResponseEntity.ok(created);
}
```

### Example 2: Deal Status Changed
```java
// In DealService
public Deal updateDealStatus(Long dealId, String newStatus, String tenantId) {
    Deal deal = dealRepository.findById(dealId).orElseThrow();
    String oldStatus = deal.getStatus();
    deal.setStatus(newStatus);
    Deal updated = dealRepository.save(deal);
    
    // Trigger workflow
    Map<String, Object> data = Map.of(
        "dealId", dealId,
        "oldStatus", oldStatus,
        "newStatus", newStatus,
        "amount", deal.getAmount()
    );
    
    workflowClient.triggerWorkflowsAsync(tenantId, "DEAL", "ON_STATUS_CHANGE", data);
    
    return updated;
}
```

---

## Testing

### 1. Create a Test Workflow
```sql
INSERT INTO tenant_1.workflows (workflow_name, module_type, trigger_type, is_active, created_at)
VALUES ('Welcome New Lead', 'LEAD', 'ON_CREATE', true, NOW());
```

### 2. Create a Lead via API
```bash
curl -X POST http://localhost:8080/crm/leads \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant_1" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "phone": "1234567890"
  }'
```

### 3. Check Workflow Execution
```sql
SELECT * FROM tenant_1.workflow_executions 
WHERE workflow_id = 1 
ORDER BY started_at DESC;
```

---

## Next Steps

1. ✅ Implement WorkflowClient in CRM service
2. ✅ Add trigger calls in Lead/Contact/Deal services
3. ✅ Create WorkflowTriggerController in workflow service
4. ✅ Test with real records
5. 🔄 Add retry mechanism (optional)
6. 🔄 Add circuit breaker (optional)
7. 🔄 Monitor execution metrics

## Advanced: Add Retry & Circuit Breaker

For production, add Resilience4j:

```java
@Retry(name = "workflowTrigger", fallbackMethod = "triggerWorkflowsFallback")
@CircuitBreaker(name = "workflowService", fallbackMethod = "triggerWorkflowsFallback")
public void triggerWorkflows(...) {
    // existing code
}

private void triggerWorkflowsFallback(Exception e) {
    log.warn("Workflow trigger failed, will retry later: {}", e.getMessage());
    // Could save to a retry queue
}
```
