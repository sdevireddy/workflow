# Start Workflow Service - Quick Guide

## Issue Found
The workflow service is **NOT RUNNING**. That's why you're getting 404 errors.

## Service Configuration
- **Port**: 8099 (configured in application.yml)
- **Context Path**: /api/workflows
- **Full URL**: http://localhost:8099/api/workflows

## Steps to Start

### Option 1: Using Maven (Recommended)
```bash
cd workflow-service
mvn spring-boot:run
```

### Option 2: Using JAR
```bash
cd workflow-service
mvn clean package -DskipTests
java -jar target/workflow-service-0.0.1-SNAPSHOT.jar
```

### Option 3: Using IDE
1. Open workflow-service in your IDE
2. Find the main application class (WorkflowServiceApplication.java)
3. Run it as a Spring Boot application

## Verify Service is Running

### Check if port 8099 is listening:
```bash
netstat -ano | findstr :8099
```

### Test the endpoint directly:
```bash
curl http://localhost:8099/api/workflows -H "X-Tenant-ID: tenant_1"
```

### Check health endpoint:
```bash
curl http://localhost:8099/actuator/health
```

## After Starting Workflow Service

1. **Restart API Gateway** (port 8080) to ensure routing is active
2. Test through gateway:
   ```bash
   curl http://localhost:8080/api/workflows -H "X-Tenant-ID: tenant_1"
   ```

## Current Service Status

✅ **API Gateway**: Running on port 8080
✅ **Communication Service**: Running on port 8085  
❌ **Workflow Service**: NOT RUNNING (should be on 8099)

## Configuration Summary

### API Gateway (application.yml)
```yaml
- id: workflow-service
  uri: http://localhost:8099
  predicates:
    - Path=/api/workflows/**
```

### Frontend (workflowService.js & workflowApi.js)
```javascript
const API_BASE_URL = 'http://localhost:8080/api';
```

### Workflow Service (application.yml)
```yaml
server:
  port: 8099
```

## Troubleshooting

### If port 8099 is already in use:
```bash
# Find what's using the port
netstat -ano | findstr :8099

# Kill the process (replace PID with actual process ID)
taskkill /PID <PID> /F
```

### If you get database connection errors:
Check that MySQL is running and the database exists:
- Common schema: crm_common3
- Tenant schemas: tenant_1, tenant_2, etc.

### If you get Eureka connection errors:
The service will still work without Eureka (discovery server). You can ignore these warnings or start the discovery server on port 8767.
