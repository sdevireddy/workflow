# Workflow Service - Enterprise CRM Workflow Automation

## Overview
Production-ready workflow automation service for CRM systems. Competes with Salesforce Flow, HubSpot Workflows, and Zoho Flow.

## Features
-  Visual workflow builder support (backend ready)
-  Multi-step workflows with conditional branching
-  Time-based and scheduled triggers
-  Approval workflows
-  Multi-tenancy support
-  Version control and rollback
-  Comprehensive audit trail
-  Integration with CRM and Communication services

## Tech Stack
- Java 17
- Spring Boot 3.2.0
- MySQL 8.0+
- Spring Data JPA
- Spring Security
- Quartz Scheduler
- Eureka Service Discovery

## Project Structure
```
workflow-service/
 src/main/java/com/zen/workflow/
    WorkflowServiceApplication.java
    config/          # Multi-tenant & security config
    enums/           # Workflow enums
    dto/             # Data transfer objects
    repository/      # JPA repositories
    service/         # Business logic
    controller/      # REST controllers
    engine/          # Workflow execution engine
    exception/       # Exception handling
 src/main/resources/
    application.yml
 pom.xml
```

## Database
- **Database**: crm_db (shared with CRM service)
- **Schemas**: Multi-tenant (tenant_1, tenant_2, etc.)
- **Tables**: 15 workflow tables per tenant schema

## Configuration

### application.yml
```yaml
server:
  port: 8085
  servlet:
    context-path: /workflow-service

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/crm_db
    username: root
    password: root
```

### Environment Variables
```
DB_URL=jdbc:mysql://localhost:3306/crm_db
DB_USER=root
DB_PASSWORD=root
EUREKA_SERVER=http://localhost:8761/eureka/
```

## Build & Run

### Build
```bash
mvn clean install
```

### Run
```bash
mvn spring-boot:run
```

Or:
```bash
java -jar target/workflow-service-1.0.0.jar
```

## API Endpoints

### Workflow Management
```
POST   /api/v1/workflows                    - Create workflow
GET    /api/v1/workflows                    - List workflows
GET    /api/v1/workflows/{id}               - Get workflow
PUT    /api/v1/workflows/{id}               - Update workflow
DELETE /api/v1/workflows/{id}               - Delete workflow
POST   /api/v1/workflows/{id}/activate      - Activate
POST   /api/v1/workflows/{id}/deactivate    - Deactivate
```

### Execution
```
POST   /api/v1/workflows/{id}/execute       - Execute workflow
GET    /api/v1/executions                   - List executions
GET    /api/v1/executions/{id}              - Get details
GET    /api/v1/executions/{id}/logs         - Get logs
POST   /api/v1/executions/{id}/retry        - Retry failed
POST   /api/v1/executions/{id}/cancel       - Cancel
```

### Approvals
```
GET    /api/v1/approvals/pending            - Pending approvals
POST   /api/v1/approvals/{id}/approve       - Approve
POST   /api/v1/approvals/{id}/reject        - Reject
```

## API Usage

### Create Workflow
```bash
curl -X POST http://localhost:8085/workflow-service/api/v1/workflows \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant_1" \
  -d '{
    "workflowName": "Lead Follow-up",
    "workflowKey": "lead_followup",
    "moduleType": "LEAD",
    "triggerType": "FIELD_UPDATE",
    "isActive": true,
    "workflowConfig": {
      "trigger": {
        "conditions": [
          {"field": "status", "operator": "EQUALS", "value": "New"}
        ]
      },
      "nodes": [
        {
          "nodeKey": "N1",
          "nodeType": "WAIT",
          "config": {"duration": 24, "unit": "HOURS"}
        }
      ]
    }
  }'
```

### List Workflows
```bash
curl -X GET http://localhost:8085/workflow-service/api/v1/workflows \
  -H "X-Tenant-ID: tenant_1"
```

### Execute Workflow
```bash
curl -X POST http://localhost:8085/workflow-service/api/v1/workflows/1/execute \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant_1" \
  -d '{
    "entityType": "LEAD",
    "entityId": 123,
    "context": {
      "leadEmail": "john@example.com",
      "leadName": "John Doe"
    }
  }'
```

## Multi-Tenancy

### Tenant Header
All API requests must include tenant header:
```
X-Tenant-ID: tenant_1
```

### Tenant Isolation
- Each tenant has separate database schema
- Workflows are isolated per tenant
- Executions are tenant-specific

## Workflow JSON Structure

```json
{
  "workflowName": "Lead Nurturing",
  "workflowKey": "lead_nurture",
  "moduleType": "LEAD",
  "triggerType": "FIELD_UPDATE",
  "isActive": true,
  "workflowConfig": {
    "trigger": {
      "conditions": [
        {"field": "status", "operator": "EQUALS", "value": "New"}
      ]
    },
    "nodes": [
      {
        "nodeKey": "N1",
        "nodeType": "WAIT",
        "config": {"duration": 24, "unit": "HOURS"},
        "nextNodeId": "N2"
      },
      {
        "nodeKey": "N2",
        "nodeType": "CONDITION",
        "config": {
          "field": "leadScore",
          "operator": "GREATER_THAN",
          "value": 50
        },
        "trueNodeId": "N3",
        "falseNodeId": "N4"
      },
      {
        "nodeKey": "N3",
        "nodeType": "ACTION",
        "actionType": "SEND_EMAIL",
        "config": {
          "templateId": 1,
          "to": "{{lead.email}}",
          "subject": "Welcome!"
        }
      }
    ]
  }
}
```

## Trigger Types
- **FIELD_UPDATE** - When field value changes
- **RECORD_CREATE** - On new record creation
- **RECORD_UPDATE** - On record update
- **TIME_BASED** - After time delay
- **SCHEDULED** - Cron-based scheduling
- **MANUAL** - User-initiated
- **WEBHOOK** - External system trigger

## Action Types
- **SEND_EMAIL** - Send email via communication service
- **SEND_SMS** - Send SMS message
- **SEND_WHATSAPP** - Send WhatsApp message
- **UPDATE_FIELD** - Update record fields
- **CREATE_RECORD** - Create new record
- **DELETE_RECORD** - Delete record
- **WEBHOOK** - Call external API
- **APPROVAL** - Request approval
- **CUSTOM_FUNCTION** - Execute custom code

## Node Types
- **ACTION** - Execute an action
- **CONDITION** - Conditional branching (IF/ELSE)
- **WAIT** - Delay execution
- **APPROVAL** - Request approval
- **LOOP** - Iterate over collection
- **WEBHOOK** - Call external API

## Integration

### With CRM Service
- Trigger workflows on CRM entity changes
- Update CRM records from workflows
- Read CRM data for conditions

### With Communication Service
- Send emails using templates
- Send SMS messages
- Send WhatsApp messages

### With Auth Service
- JWT authentication
- Tenant identification
- User permissions

## Health Check
```bash
curl http://localhost:8085/workflow-service/actuator/health
```

## Swagger UI
```
http://localhost:8085/workflow-service/swagger-ui.html
```

## Monitoring
- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Info: `/actuator/info`

## Dependencies

### Required Services
- MySQL 8.0+
- Eureka Server (port 8761)
- CRM Service (optional)
- Communication Service (optional)

### Required JAR
- zen-entities-1.0.0.jar (contains workflow entities)

## Development

### Add zen-entities Dependency
```xml
<dependency>
    <groupId>com.zen</groupId>
    <artifactId>zen-entities</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Entity Classes Required
- Workflow
- WorkflowExecution
- WorkflowNode
- WorkflowApprovalRequest
- WorkflowExecutionLog

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

## Deployment

### Docker
```bash
docker build -t workflow-service:1.0.0 .
docker run -p 8085:8085 workflow-service:1.0.0
```

### Production
- Configure environment variables
- Enable SSL/TLS
- Set up monitoring and alerts
- Configure backup strategy

## Performance

### Optimization
- Connection pooling (HikariCP)
- Async execution for long-running workflows
- Database indexing
- Query optimization

### Scalability
- Horizontal scaling supported
- Stateless design
- Multi-instance deployment

## Security

### Authentication
- JWT-based authentication
- Tenant isolation
- Role-based access control

### Data Protection
- Tenant data isolation at database level
- SQL injection prevention
- Input validation

## Troubleshooting

### Common Issues

**Issue**: Workflow not executing
- Check if workflow is active
- Verify trigger conditions
- Check execution logs

**Issue**: Tenant not found
- Verify X-Tenant-ID header
- Check tenant schema exists
- Verify database connection

**Issue**: Action failed
- Check communication service is running
- Verify action configuration
- Check execution logs

## Support

### Documentation
- Implementation Guide: `WORKFLOW_SERVICE_IMPLEMENTATION_SUMMARY.md`
- Entity Guide: `WORKFLOW_ENTITIES_FOR_ZEN_ENTITIES.md`
- Complete Code: `WORKFLOW_SERVICE_COMPLETE_CODE.md`

### Logs
```bash
tail -f logs/workflow-service.log
```

## License
Proprietary - Zen CRM

## Version
1.0.0

---

**Enterprise Workflow Automation - Production Ready**
