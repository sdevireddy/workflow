-- ============================================================================
-- Workflow Templates Table for Common Schema
-- This table stores standard workflow templates that can be copied to tenants
-- ============================================================================

USE common;

-- Create workflow_templates table in common schema
CREATE TABLE IF NOT EXISTS workflow_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_key VARCHAR(100) NOT NULL UNIQUE COMMENT 'Unique identifier for the template',
    template_name VARCHAR(255) NOT NULL COMMENT 'Display name of the template',
    category VARCHAR(50) NOT NULL COMMENT 'Category: LEAD, DEAL, CONTACT, TASK, ACCOUNT, etc.',
    description TEXT COMMENT 'Description of what the template does',
    icon VARCHAR(50) COMMENT 'Icon name for UI display',
    
    -- Template configuration (JSON)
    template_config JSON NOT NULL COMMENT 'Complete workflow configuration including nodes and connections',
    
    -- Template metadata
    is_premium BOOLEAN DEFAULT FALSE COMMENT 'Whether this is a premium template',
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Whether this template is available',
    
    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_category (category),
    INDEX idx_active (is_active),
    INDEX idx_premium (is_premium)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Standard workflow templates available to all tenants';

-- ============================================================================
-- How this works:
-- ============================================================================
-- 1. Templates are stored in common.workflow_templates
-- 2. When a tenant enables the workflow module, templates are copied to tenant.workflows
-- 3. Tenants can then:
--    - View available templates
--    - Create workflows from templates (creates a copy they can customize)
--    - Templates remain in tenant schema for reference
-- 
-- This follows the same pattern as:
-- - common.modules -> tenant.tenant_modules
-- - common.roles -> tenant.tenant_roles
-- - common.permissions -> tenant.tenant_permissions
-- ============================================================================

-- Verify table structure
DESCRIBE workflow_templates;

-- Check if templates exist
SELECT COUNT(*) as template_count FROM workflow_templates;

-- View templates by category
SELECT category, COUNT(*) as count 
FROM workflow_templates 
WHERE is_active = 1 
GROUP BY category;
