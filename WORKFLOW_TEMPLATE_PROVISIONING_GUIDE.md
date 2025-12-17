# Workflow Template Provisioning System

## Overview

This system allows tenants to use standard workflow templates that are stored in the common schema. It follows the same pattern as the auth-service's `TenantModuleProvisioningService`.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     COMMON SCHEMA                            │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  workflow_templates                                     │ │
│  │  - Standard templates (20+ templates)                  │ │
│  │  - Categories: LEAD, DEAL, CONTACT, TASK, etc.        │ │
│  │  - Maintained by system admins                         │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ Copy during provisioning
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                     TENANT SCHEMA                            │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  workflows (where is_template = 1)                     │ │
│  │  - Copied templates available to tenant                │ │
│  │  - Tenant can create instances from these              │ │
│  │  - Tenant-specific customizations                      │ │
│  └────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  workflows (where is_template = 0)                     │ │
│  │  - Active workflows created from templates             │ │
│  │  - Fully customizable by tenant                        │ │
│  │  - Can be edited, activated, deactivated               │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Database Schema

### Common Schema Table

```sql
-- common.workflow_templates
CREATE TABLE workflow_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_key VARCHAR(100) NOT NULL UNIQUE,
    template_name VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description TEXT,
    icon VARCHAR(50),
    template_config JSON NOT NULL,
    is_premium BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### Tenant Schema Table

```sql
-- tenant_xxx.workflows
CREATE TABLE workflows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_key VARCHAR(100),  -- Links to common template
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    icon VARCHAR(50),
    workflow_config JSON NOT NULL,
    is_template BOOLEAN DEFAULT FALSE,  -- TRUE if copied from common
    is_active BOOLEAN DEFAULT TRUE,
    is_premium BOOLEAN DEFAULT FALSE,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## How It Works

### 1. Initial Tenant Provisioning

When a tenant enables the workflow module:

```java
// Called by auth-service during tenant creation
workflowTemplateProvisioningService.provisionWorkflowTemplates(tenantSchema);
```

This copies ALL active templates from `common.workflow_templates` to `tenant.workflows` with `is_template = 1`.

### 2. Tenant Views Available Templates

```http
GET /api/workflow-templates
Headers:
  X-Tenant-ID: tenant_abc

Response:
{
  "success": true,
  "templates": [
    {
      "id": 1,
      "template_key": "lead_auto_assign",
      "name": "Lead Auto-Assignment",
      "category": "LEAD",
      "description": "Automatically assign new leads...",
      "icon": "user-plus",
      "is_premium": false
    },
    ...
  ],
  "count": 20
}
```

### 3. Tenant Creates Workflow from Template

```http
POST /api/workflow-templates/lead_auto_assign/create
Headers:
  X-Tenant-ID: tenant_abc
Body:
{
  "name": "My Custom Lead Assignment",
  "userId": 123
}

Response:
{
  "success": true,
  "workflowId": 456,
  "message": "Workflow created successfully from template"
}
```

This creates a NEW workflow in `tenant.workflows` with:
- `is_template = 0` (it's an instance, not a template)
- `template_key` = original template key (for reference)
- `workflow_config` = copy of template config (can be customized)
- `created_by` = userId

### 4. Tenant Customizes the Workflow

The tenant can now edit the workflow using the workflow builder UI:
- Add/remove nodes
- Change conditions
- Modify actions
- Activate/deactivate

## API Endpoints

### Get All Templates

```http
GET /api/workflow-templates
Headers: X-Tenant-ID: {tenantSchema}
```

Returns all available workflow templates for the tenant.

### Get Templates by Category

```http
GET /api/workflow-templates/category/{category}
Headers: X-Tenant-ID: {tenantSchema}
```

Categories: `LEAD`, `DEAL`, `CONTACT`, `TASK`, `ACCOUNT`

### Create Workflow from Template

```http
POST /api/workflow-templates/{templateKey}/create
Headers: X-Tenant-ID: {tenantSchema}
Body: {
  "name": "Custom Workflow Name",
  "userId": 123
}
```

Creates a new workflow instance from the template.

### Copy Specific Template

```http
POST /api/workflow-templates/{templateKey}/copy
Headers: X-Tenant-ID: {tenantSchema}
```

Copies a specific template from common schema (useful if template was added after tenant creation).

### Provision All Templates (Admin)

```http
POST /api/workflow-templates/provision
Headers: X-Tenant-ID: {tenantSchema}
```

Provisions all workflow templates for a tenant (called during setup).

## Integration with Auth Service

### During Tenant Creation

When a tenant is created with the workflow module enabled, the auth-service should call:

```java
// In TenantModuleProvisioningService or TenantRegistrationService
if (modules.contains("WORKFLOW")) {
    // Call workflow-service to provision templates
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Tenant-ID", tenantSchema);
    
    HttpEntity<Void> request = new HttpEntity<>(headers);
    
    restTemplate.postForEntity(
        "http://workflow-service:8080/api/workflow-templates/provision",
        request,
        Map.class
    );
}
```

Or use a direct database approach:

```java
// In TenantModuleProvisioningService.provisionModules()
if (moduleKeys.contains("WORKFLOW")) {
    provisionWorkflowTemplates(schemaName);
}

private void provisionWorkflowTemplates(String tenantSchema) {
    JdbcTemplate tenantJdbc = new JdbcTemplate(
        dataSourceManager.getTenantDataSource(tenantSchema));
    
    String sql = String.format(
        "INSERT IGNORE INTO workflows (" +
        "    template_key, name, description, category, icon, " +
        "    workflow_config, is_template, is_active, is_premium, " +
        "    created_at, updated_at" +
        ") " +
        "SELECT " +
        "    template_key, template_name, description, category, icon, " +
        "    template_config, 1, is_active, is_premium, " +
        "    NOW(), NOW() " +
        "FROM %s.workflow_templates " +
        "WHERE is_active = 1",
        commonSchemaName
    );
    
    int count = tenantJdbc.update(sql);
    log.info("✅ Copied {} workflow templates to tenant {}", count, tenantSchema);
}
```

## Template Categories

### LEAD Workflows
- `lead_auto_assign` - Lead Auto-Assignment
- `lead_nurture` - Lead Nurturing Campaign
- `lead_scoring` - Automatic Lead Scoring
- `inactive_lead_reengagement` - Inactive Lead Re-engagement
- `lead_conversion` - Lead Conversion Workflow

### DEAL Workflows
- `deal_won_notify` - Deal Won Notification
- `deal_approval` - Deal Approval Workflow
- `deal_stage_automation` - Deal Stage Automation
- `deal_lost_followup` - Deal Lost Follow-up

### CONTACT Workflows
- `contact_birthday` - Birthday Greeting Automation
- `contact_anniversary` - Anniversary Reminders
- `contact_engagement` - Contact Engagement Tracking

### TASK Workflows
- `task_reminder` - Task Due Date Reminder
- `task_escalation` - Task Escalation
- `task_auto_assign` - Task Auto-Assignment

### ACCOUNT Workflows
- `account_renewal` - Account Renewal Reminders
- `account_health_check` - Account Health Monitoring
- `account_upsell` - Upsell Opportunity Detection

## Benefits of This Approach

### 1. Centralized Management
- Templates are maintained in one place (common schema)
- Easy to add new templates for all tenants
- Consistent template quality across tenants

### 2. Tenant Flexibility
- Tenants can use templates as-is
- Tenants can customize templates for their needs
- Tenants can create workflows from scratch

### 3. No Stored Procedures
- Uses direct SQL INSERT statements
- Follows the same pattern as auth-service
- Easy to debug and maintain

### 4. Scalability
- Templates are copied once during provisioning
- No runtime dependency on common schema
- Each tenant has their own copy

### 5. Version Control
- Template updates don't affect existing tenant workflows
- Tenants can opt-in to new template versions
- Clear separation between templates and instances

## Testing

### 1. Setup Common Schema

```sql
-- Run this first
SOURCE workflow_templates_common_schema.sql;

-- Verify
USE common;
SELECT COUNT(*) FROM workflow_templates;
```

### 2. Insert Templates

```sql
-- Run this to populate templates
SOURCE V22__insert_standard_workflow_templates.sql;

-- Verify
SELECT category, COUNT(*) as count 
FROM workflow_templates 
GROUP BY category;
```

### 3. Test Provisioning

```java
// In your test
@Test
public void testProvisionWorkflowTemplates() {
    String tenantSchema = "tenant_test";
    
    // Provision templates
    provisioningService.provisionWorkflowTemplates(tenantSchema);
    
    // Verify
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    jdbc.execute("USE " + tenantSchema);
    
    Integer count = jdbc.queryForObject(
        "SELECT COUNT(*) FROM workflows WHERE is_template = 1",
        Integer.class
    );
    
    assertTrue(count > 0);
}
```

### 4. Test Template Usage

```bash
# Get available templates
curl -X GET http://localhost:8080/api/workflow-templates \
  -H "X-Tenant-ID: tenant_abc"

# Create workflow from template
curl -X POST http://localhost:8080/api/workflow-templates/lead_auto_assign/create \
  -H "X-Tenant-ID: tenant_abc" \
  -H "Content-Type: application/json" \
  -d '{"name": "My Lead Assignment", "userId": 123}'
```

## Maintenance

### Adding New Templates

1. Insert into common.workflow_templates:

```sql
INSERT INTO common.workflow_templates (
    template_key, template_name, category, description, 
    icon, template_config, is_premium, is_active
) VALUES (
    'new_template',
    'New Template Name',
    'LEAD',
    'Description...',
    'icon-name',
    '{"nodes": [...]}',
    FALSE,
    TRUE
);
```

2. Existing tenants can copy the new template:

```http
POST /api/workflow-templates/new_template/copy
Headers: X-Tenant-ID: tenant_abc
```

### Updating Templates

Template updates don't affect existing tenant workflows. To update:

1. Update common.workflow_templates
2. Tenants can manually copy the updated template
3. Or create a migration script to update tenant templates

## Summary

This workflow template provisioning system:
- ✅ Follows the same pattern as auth-service module provisioning
- ✅ Uses direct SQL instead of stored procedures
- ✅ Provides centralized template management
- ✅ Gives tenants full flexibility
- ✅ Scales well with multiple tenants
- ✅ Easy to maintain and extend

The system is production-ready and integrates seamlessly with your existing multi-tenant architecture.
