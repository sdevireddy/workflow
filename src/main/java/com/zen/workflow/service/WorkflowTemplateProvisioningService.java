package com.zen.workflow.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * Service to provision workflow templates from common schema to tenant schemas
 * Similar pattern to TenantModuleProvisioningService in auth-service
 */
@Service
public class WorkflowTemplateProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowTemplateProvisioningService.class);

    private final DataSource dataSource;

    @Value("${app.schema.common:common}")
    private String commonSchemaName;

    public WorkflowTemplateProvisioningService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Copy all workflow templates from common schema to tenant schema
     * This is called during tenant provisioning when workflow module is enabled
     * 
     * @param tenantSchema The tenant schema name
     */
    @Transactional
    public void provisionWorkflowTemplates(String tenantSchema) {
        log.info("🔧 Provisioning workflow templates for tenant: {}", tenantSchema);

        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            // Switch to tenant schema
            jdbcTemplate.execute("USE " + tenantSchema);

            // Check if templates already exist
            Integer existingCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM workflows WHERE is_template = 1",
                    Integer.class
            );

            if (existingCount != null && existingCount > 0) {
                log.info("⏭️ Tenant already has {} workflow templates, checking for missing ones", existingCount);
            } else {
                log.info("📦 Copying workflow templates from common schema...");
            }

            // Copy workflow templates from common.workflow_templates to tenant.workflows
            String copyTemplatesSql = String.format(
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
                    "WHERE is_active = 1 " +
                    "AND NOT EXISTS (" +
                    "    SELECT 1 FROM workflows w " +
                    "    WHERE w.template_key = %s.workflow_templates.template_key " +
                    "    AND w.is_template = 1" +
                    ")",
                    commonSchemaName, commonSchemaName
            );

            int templatesInserted = jdbcTemplate.update(copyTemplatesSql);

            // Verify templates were copied
            Integer totalTemplates = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM workflows WHERE is_template = 1",
                    Integer.class
            );

            log.info("✅ Copied {} new workflow templates. Total templates in tenant: {}",
                    templatesInserted, totalTemplates);

            // Log template categories for verification
            List<Map<String, Object>> categories = jdbcTemplate.queryForList(
                    "SELECT category, COUNT(*) as count " +
                    "FROM workflows WHERE is_template = 1 " +
                    "GROUP BY category"
            );

            log.info("📊 Template categories: {}", categories);

        } catch (Exception e) {
            log.error("❌ Failed to provision workflow templates for tenant {}: {}",
                    tenantSchema, e.getMessage(), e);
            throw new RuntimeException("Failed to provision workflow templates", e);
        }
    }

    /**
     * Copy a single workflow template from common schema to tenant
     * Used when tenant wants to use a specific template
     * 
     * @param tenantSchema The tenant schema name
     * @param templateKey The template key to copy
     * @return The ID of the copied workflow in tenant schema
     */
    @Transactional
    public Long copyTemplateToTenant(String tenantSchema, String templateKey) {
        log.info("📋 Copying workflow template '{}' to tenant: {}", templateKey, tenantSchema);

        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            // Switch to tenant schema
            jdbcTemplate.execute("USE " + tenantSchema);

            // Check if template already exists
            Integer existingCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM workflows WHERE template_key = ? AND is_template = 1",
                    Integer.class, templateKey
            );

            if (existingCount != null && existingCount > 0) {
                log.info("⏭️ Template '{}' already exists in tenant", templateKey);
                return jdbcTemplate.queryForObject(
                        "SELECT id FROM workflows WHERE template_key = ? AND is_template = 1",
                        Long.class, templateKey
                );
            }

            // Copy the specific template
            String copyTemplateSql = String.format(
                    "INSERT INTO workflows (" +
                    "    template_key, name, description, category, icon, " +
                    "    workflow_config, is_template, is_active, is_premium, " +
                    "    created_at, updated_at" +
                    ") " +
                    "SELECT " +
                    "    template_key, template_name, description, category, icon, " +
                    "    template_config, 1, is_active, is_premium, " +
                    "    NOW(), NOW() " +
                    "FROM %s.workflow_templates " +
                    "WHERE template_key = ? AND is_active = 1",
                    commonSchemaName
            );

            int inserted = jdbcTemplate.update(copyTemplateSql, templateKey);

            if (inserted == 0) {
                throw new IllegalArgumentException("Template '" + templateKey + "' not found in common schema");
            }

            // Get the inserted workflow ID
            Long workflowId = jdbcTemplate.queryForObject(
                    "SELECT id FROM workflows WHERE template_key = ? AND is_template = 1",
                    Long.class, templateKey
            );

            log.info("✅ Template '{}' copied successfully with ID: {}", templateKey, workflowId);

            return workflowId;

        } catch (Exception e) {
            log.error("❌ Failed to copy template '{}' to tenant {}: {}",
                    templateKey, tenantSchema, e.getMessage(), e);
            throw new RuntimeException("Failed to copy workflow template", e);
        }
    }

    /**
     * Create a new workflow instance from a template
     * This creates a copy of the template as a regular workflow that can be customized
     * 
     * @param tenantSchema The tenant schema name
     * @param templateKey The template key to use
     * @param workflowName Custom name for the new workflow
     * @param userId The user creating the workflow
     * @return The ID of the new workflow
     */
    @Transactional
    public Long createWorkflowFromTemplate(String tenantSchema, String templateKey, 
                                          String workflowName, Long userId) {
        log.info("🎨 Creating workflow from template '{}' for tenant: {}", templateKey, tenantSchema);

        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            // Switch to tenant schema
            jdbcTemplate.execute("USE " + tenantSchema);

            // First, ensure the template exists in tenant schema
            Integer templateExists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM workflows WHERE template_key = ? AND is_template = 1",
                    Integer.class, templateKey
            );

            if (templateExists == null || templateExists == 0) {
                // Template doesn't exist, copy it from common schema first
                log.info("📥 Template not found in tenant, copying from common schema...");
                copyTemplateToTenant(tenantSchema, templateKey);
            }

            // Get the template
            Map<String, Object> template = jdbcTemplate.queryForMap(
                    "SELECT * FROM workflows WHERE template_key = ? AND is_template = 1",
                    templateKey
            );

            // Create a new workflow instance from the template
            String createWorkflowSql = 
                    "INSERT INTO workflows (" +
                    "    name, description, category, icon, workflow_config, " +
                    "    is_template, is_active, created_by, created_at, updated_at" +
                    ") VALUES (?, ?, ?, ?, ?, 0, 1, ?, NOW(), NOW())";

            jdbcTemplate.update(createWorkflowSql,
                    workflowName != null ? workflowName : template.get("name"),
                    template.get("description"),
                    template.get("category"),
                    template.get("icon"),
                    template.get("workflow_config"),
                    userId
            );

            // Get the new workflow ID
            Long workflowId = jdbcTemplate.queryForObject(
                    "SELECT LAST_INSERT_ID()",
                    Long.class
            );

            log.info("✅ Workflow created from template '{}' with ID: {}", templateKey, workflowId);

            return workflowId;

        } catch (Exception e) {
            log.error("❌ Failed to create workflow from template '{}' for tenant {}: {}",
                    templateKey, tenantSchema, e.getMessage(), e);
            throw new RuntimeException("Failed to create workflow from template", e);
        }
    }

    /**
     * Get all available workflow templates for a tenant
     * Returns templates from tenant schema (which were copied from common)
     * 
     * @param tenantSchema The tenant schema name
     * @return List of available templates
     */
    public List<Map<String, Object>> getAvailableTemplates(String tenantSchema) {
        log.info("📋 Getting available workflow templates for tenant: {}", tenantSchema);

        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            // Switch to tenant schema
            jdbcTemplate.execute("USE " + tenantSchema);

            List<Map<String, Object>> templates = jdbcTemplate.queryForList(
                    "SELECT id, template_key, name, description, category, icon, " +
                    "       is_premium, created_at " +
                    "FROM workflows " +
                    "WHERE is_template = 1 AND is_active = 1 " +
                    "ORDER BY category, name"
            );

            log.info("✅ Found {} workflow templates for tenant", templates.size());

            return templates;

        } catch (Exception e) {
            log.error("❌ Failed to get workflow templates for tenant {}: {}",
                    tenantSchema, e.getMessage(), e);
            throw new RuntimeException("Failed to get workflow templates", e);
        }
    }

    /**
     * Get templates by category
     * 
     * @param tenantSchema The tenant schema name
     * @param category The category (LEAD, DEAL, CONTACT, etc.)
     * @return List of templates in the category
     */
    public List<Map<String, Object>> getTemplatesByCategory(String tenantSchema, String category) {
        log.info("📋 Getting workflow templates for category '{}' in tenant: {}", category, tenantSchema);

        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            // Switch to tenant schema
            jdbcTemplate.execute("USE " + tenantSchema);

            List<Map<String, Object>> templates = jdbcTemplate.queryForList(
                    "SELECT id, template_key, name, description, category, icon, " +
                    "       is_premium, created_at " +
                    "FROM workflows " +
                    "WHERE is_template = 1 AND is_active = 1 AND category = ? " +
                    "ORDER BY name",
                    category
            );

            log.info("✅ Found {} workflow templates in category '{}'", templates.size(), category);

            return templates;

        } catch (Exception e) {
            log.error("❌ Failed to get workflow templates for category '{}' in tenant {}: {}",
                    category, tenantSchema, e.getMessage(), e);
            throw new RuntimeException("Failed to get workflow templates by category", e);
        }
    }
}
