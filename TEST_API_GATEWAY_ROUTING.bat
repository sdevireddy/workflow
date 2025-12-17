@echo off
echo ========================================
echo Testing Workflow Service API Gateway Routing
echo ========================================
echo.

echo [1/4] Testing Workflow Service Direct (Port 8099)...
curl -X GET http://localhost:8099/api/workflows -H "X-Tenant-ID: tenant_1" -H "Content-Type: application/json" 2>nul
if %errorlevel% equ 0 (
    echo ✓ Workflow service is responding on port 8099
) else (
    echo ✗ Workflow service not responding on port 8099
)
echo.

echo [2/4] Testing Workflow Service Health...
curl -X GET http://localhost:8099/actuator/health 2>nul
echo.
echo.

echo [3/4] Testing API Gateway (Port 8080)...
curl -X GET http://localhost:8080/api/workflows -H "X-Tenant-ID: tenant_1" -H "Content-Type: application/json" 2>nul
if %errorlevel% equ 0 (
    echo ✓ API Gateway is routing to workflow service
) else (
    echo ✗ API Gateway routing failed - Did you restart the gateway?
)
echo.

echo [4/4] Testing Workflow Templates Endpoint...
curl -X GET http://localhost:8080/api/workflows/workflow-templates -H "X-Tenant-ID: tenant_1" -H "Content-Type: application/json" 2>nul
echo.
echo.

echo ========================================
echo Test Complete
echo ========================================
echo.
echo If you see 404 errors from API Gateway:
echo   1. Make sure API Gateway is restarted
echo   2. Check api-gateway/src/main/resources/application.yml
echo   3. Verify workflow-service route points to http://localhost:8099
echo.
echo If you see 500 errors:
echo   1. Check workflow service logs
echo   2. Verify database connection
echo   3. Ensure tenant schema exists (tenant_1)
echo.
pause
