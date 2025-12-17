# Restart Services Checklist

## Current Status
✅ Workflow Service: Running on port 8099
❌ API Gateway: Needs restart to pick up new configuration

## What Changed
The API Gateway configuration was updated to route `/api/workflows/**` to `http://localhost:8099` (was incorrectly pointing to 8085).

## Steps to Complete Integration

### 1. Restart API Gateway
The gateway needs to be restarted to load the updated configuration.

**If running via Maven:**
```bash
cd api-gateway/api-gateway
# Stop the current process (Ctrl+C)
mvn spring-boot:run
```

**If running via JAR:**
```bash
cd api-gateway/api-gateway
# Stop the current process
# Then restart:
java -jar target/api-gateway-0.0.1-SNAPSHOT.jar
```

**If running via IDE:**
- Stop the running API Gateway application
- Start it again

### 2. Verify Services are Running

Check all required ports:
```bash
netstat -ano | findstr ":8080 :8099"
```

Expected output:
- Port 8080: API Gateway
- Port 8099: Workflow Service

### 3. Test the Integration

Run the test script:
```bash
cd workflow-service
TEST_API_GATEWAY_ROUTING.bat
```

Or test manually:
```bash
# Test direct workflow service
curl http://localhost:8099/api/workflows -H "X-Tenant-ID: tenant_1"

# Test through API Gateway
curl http://localhost:8080/api/workflows -H "X-Tenant-ID: tenant_1"
```

### 4. Test from Frontend

Once the gateway is restarted, your frontend should work:
- Open the workflow builder in your app
- Try creating a new workflow
- The request should go to: `http://localhost:8080/api/workflows`

## Troubleshooting

### Still getting 404 from API Gateway?
1. Verify the gateway restarted successfully
2. Check gateway logs for routing information
3. Verify the configuration file was saved:
   ```bash
   type api-gateway\api-gateway\src\main\resources\application.yml | findstr "8099"
   ```

### Getting 500 errors?
This means routing is working but there's a backend issue:
1. Check workflow service logs
2. Verify database connection
3. Ensure tenant schema exists in MySQL

### CORS errors?
The API Gateway has CORS configured for localhost. If you see CORS errors:
1. Check the gateway's CORS configuration
2. Verify the frontend is running on localhost
3. Check browser console for specific CORS error details

## Complete Service Architecture

```
Frontend (localhost:5173)
    ↓
    POST http://localhost:8080/api/workflows
    ↓
API Gateway (localhost:8080)
    ↓
    Routes /api/workflows/** → http://localhost:8099
    ↓
Workflow Service (localhost:8099)
    ↓
    Accesses tenant database
    ↓
MySQL (localhost:3306)
    - Common schema: crm_common3
    - Tenant schemas: tenant_1, tenant_2, etc.
```

## Next Steps After Restart

1. ✅ Verify API Gateway routes correctly
2. ✅ Test workflow creation from frontend
3. ✅ Test workflow execution
4. ✅ Test workflow templates endpoint
5. ✅ Update any Postman collections to use new URLs
