# Workflow UI Implementation Plan

## Overview
This document outlines the complete workflow UI implementation, including structure, components, and integration with backend.

---

## 1. UI Structure & Routes

### Recommended Path in CRM Frontend
```
crm-front/notifications/src/routes/settings/
├── Workflows.jsx                    # Main workflow list page
├── WorkflowBuilder.jsx              # Visual workflow builder
├── WorkflowTemplates.jsx            # Template marketplace
└── WorkflowExecutions.jsx           # Execution history
```

### Route Configuration
```javascript
// In your router configuration
{
  path: '/settings/workflows',
  element: <Workflows />,
  children: [
    { path: 'templates', element: <WorkflowTemplates /> },
    { path: 'builder/:id?', element: <WorkflowBuilder /> },
    { path: 'executions', element: <WorkflowExecutions /> }
  ]
}
```

---

## 2. Workflow Data Structure

### Workflow Object (from backend)
```javascript
{
  id: 1,
  workflowName: "Auto-Assign New Leads",
  workflowKey: "lead_auto_assign_001",
  description: "Automatically assign leads based on territory",
  moduleType: "LEAD",           // LEAD, CONTACT, DEAL, ACCOUNT, TASK
  triggerType: "RECORD_CREATE", // RECORD_CREATE, FIELD_UPDATE, SCHEDULED, MANUAL
  isActive: true,
  version: 1,
  workflowConfig: {             // JSON structure
    trigger: {
      type: "RECORD_CREATE",
      entity: "LEAD"
    },
    nodes: [
      {
        id: "node_1",
        type: "CONDITION",      // CONDITION, ACTION, WAIT, APPROVAL, LOOP
        name: "Check Lead Source",
        position: { x: 100, y: 100 },
        config: {
          field: "leadSource",
          operator: "equals",
          value: "Website"
        },
        trueNode: "node_2",
        falseNode: "node_3"
      },
      {
        id: "node_2",
        type: "ACTION",
        name: "Assign to Web Team",
        position: { x: 100, y: 250 },
        config: {
          action: "UPDATE_FIELD",
          field: "ownerId",
          value: "{{webTeamId}}"
        },
        nextNode: "node_4"
      }
    ]
  },
  createdBy: 1,
  updatedBy: 1,
  createdAt: "2025-12-06T10:00:00",
  updatedAt: "2025-12-06T10:00:00"
}
```

---

## 3. UI Components Structure

### A. Workflows List Page (Workflows.jsx)
```
┌─────────────────────────────────────────────────────┐
│  Workflows                          [+ New Workflow] │
├─────────────────────────────────────────────────────┤
│  [All] [Active] [Inactive] [Templates]              │
├─────────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────────┐  │
│  │ 🔵 Auto-Assign New Leads          [Active] ⚙️│  │
│  │ Trigger: When lead is created                 │  │
│  │ Last run: 2 hours ago | 45 executions        │  │
│  └───────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────┐  │
│  │ 🔴 Lead Nurturing Campaign      [Inactive] ⚙️│  │
│  │ Trigger: When lead status changes             │  │
│  │ Last run: Never | 0 executions               │  │
│  └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

**Features:**
- List all workflows with status indicators
- Filter by module type (Lead, Contact, Deal, etc.)
- Search workflows
- Quick activate/deactivate toggle
- View execution statistics
- Edit/Delete/Clone actions

### B. Workflow Builder (WorkflowBuilder.jsx)
```
┌─────────────────────────────────────────────────────────────┐
│  ← Back to Workflows    Auto-Assign New Leads    [Save] [▶️]│
├──────────────┬──────────────────────────────────────────────┤
│              │                                              │
│  Node Types  │           Canvas Area                       │
│              │                                              │
│  📋 Trigger  │    ┌──────────────┐                         │
│  ❓ Condition│    │   Trigger    │                         │
│  ⚡ Action   │    │ Lead Created │                         │
│  ⏱️ Wait     │    └──────┬───────┘                         │
│  ✅ Approval │           │                                  │
│  🔁 Loop     │           ▼                                  │
│              │    ┌──────────────┐                         │
│              │    │  Condition   │                         │
│              │    │ Lead Source? │                         │
│              │    └──┬────────┬──┘                         │
│              │       │        │                             │
│              │    Yes│        │No                           │
│              │       ▼        ▼                             │
│              │  ┌────────┐ ┌────────┐                      │
│              │  │ Action │ │ Action │                      │
│              │  │Assign  │ │Round   │                      │
│              │  │to Team │ │Robin   │                      │
│              │  └────────┘ └────────┘                      │
└──────────────┴──────────────────────────────────────────────┘
```

**Features:**
- Drag-and-drop node creation
- Visual flow connections
- Node configuration panel (right sidebar)
- Zoom in/out, pan canvas
- Auto-layout option
- Test workflow button
- Version history

### C. Workflow Templates (WorkflowTemplates.jsx)
```
┌─────────────────────────────────────────────────────┐
│  Workflow Templates                                  │
├─────────────────────────────────────────────────────┤
│  [All] [Lead] [Contact] [Deal] [Task]              │
├─────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐                │
│  │ 👤 Auto-     │  │ 📧 Lead      │                │
│  │ Assign Leads │  │ Nurturing    │                │
│  │              │  │              │                │
│  │ ⭐⭐⭐⭐⭐    │  │ ⭐⭐⭐⭐☆    │                │
│  │ 1.2k uses    │  │ 856 uses     │                │
│  │ [Install]    │  │ [Install]    │                │
│  └──────────────┘  └──────────────┘                │
└─────────────────────────────────────────────────────┘
```

**Features:**
- Browse pre-built templates
- Filter by category
- Preview template flow
- Install template to tenant
- Rate and review templates
- Premium templates badge

### D. Workflow Executions (WorkflowExecutions.jsx)
```
┌─────────────────────────────────────────────────────┐
│  Execution History                                   │
├─────────────────────────────────────────────────────┤
│  Workflow: [All Workflows ▼]  Status: [All ▼]      │
├─────────────────────────────────────────────────────┤
│  ✅ #12345 | Auto-Assign Leads | 2 min ago | 1.2s  │
│  ✅ #12344 | Lead Nurturing    | 5 min ago | 0.8s  │
│  ❌ #12343 | Deal Approval     | 10 min ago | 2.1s │
│  ⏳ #12342 | Task Reminder     | Running... | -    │
└─────────────────────────────────────────────────────┘
```

**Features:**
- View all workflow executions
- Filter by workflow, status, date range
- View execution logs
- Retry failed executions
- Cancel running executions
- Export execution data

---

## 4. Component Implementation Order

### Phase 1: Basic Structure (Week 1)
1. ✅ Create route structure
2. ✅ Implement Workflows list page
3. ✅ Add workflow CRUD operations
4. ✅ Integrate with backend API

### Phase 2: Template System (Week 2)
1. ✅ Create WorkflowTemplates page
2. ✅ Fetch templates from common schema
3. ✅ Implement template installation
4. ✅ Template preview modal

### Phase 3: Visual Builder (Week 3-4)
1. ✅ Setup canvas with React Flow or similar
2. ✅ Implement node types (Condition, Action, Wait, etc.)
3. ✅ Add drag-and-drop functionality
4. ✅ Node configuration panel
5. ✅ Save workflow configuration

### Phase 4: Execution & Monitoring (Week 5)
1. ✅ Create WorkflowExecutions page
2. ✅ Real-time execution status
3. ✅ Execution logs viewer
4. ✅ Retry/Cancel functionality

---

## 5. Required Libraries

```json
{
  "dependencies": {
    "react-flow-renderer": "^10.3.17",  // For visual workflow builder
    "dagre": "^0.8.5",                  // For auto-layout
    "react-beautiful-dnd": "^13.1.1",   // For drag-and-drop
    "react-json-view": "^1.21.3",       // For JSON config viewer
    "date-fns": "^2.30.0",              // For date formatting
    "recharts": "^2.10.0"               // For analytics charts
  }
}
```

---

## 6. API Integration

### API Endpoints
```javascript
// Workflows
GET    /api/v1/workflows                    // List all workflows
POST   /api/v1/workflows                    // Create workflow
GET    /api/v1/workflows/{id}               // Get workflow details
PUT    /api/v1/workflows/{id}               // Update workflow
DELETE /api/v1/workflows/{id}               // Delete workflow
POST   /api/v1/workflows/{id}/activate      // Activate workflow
POST   /api/v1/workflows/{id}/deactivate    // Deactivate workflow

// Templates
GET    /api/v1/templates                    // List templates
GET    /api/v1/templates/{id}               // Get template details
POST   /api/v1/templates/{id}/install       // Install template

// Executions
GET    /api/v1/executions                   // List executions
GET    /api/v1/executions/{id}              // Get execution details
GET    /api/v1/executions/{id}/logs         // Get execution logs
POST   /api/v1/executions/{id}/retry        // Retry failed execution
POST   /api/v1/executions/{id}/cancel       // Cancel execution
```

---

## 7. Node Types Configuration

### Condition Node
```javascript
{
  type: "CONDITION",
  config: {
    field: "leadSource",
    operator: "equals",        // equals, not_equals, contains, greater_than, less_than
    value: "Website"
  }
}
```

### Action Node
```javascript
{
  type: "ACTION",
  config: {
    action: "UPDATE_FIELD",    // UPDATE_FIELD, SEND_EMAIL, CREATE_TASK, SEND_SMS
    field: "status",
    value: "Qualified"
  }
}
```

### Wait Node
```javascript
{
  type: "WAIT",
  config: {
    duration: 3,
    unit: "DAYS"              // MINUTES, HOURS, DAYS, WEEKS
  }
}
```

### Approval Node
```javascript
{
  type: "APPROVAL",
  config: {
    approvalType: "SEQUENTIAL", // SEQUENTIAL, PARALLEL, UNANIMOUS
    approvers: ["user_1", "user_2"],
    message: "Please approve this deal"
  }
}
```

---

## 8. UI Design Recommendations

### Colors
- **Active Workflow**: Green (#10B981)
- **Inactive Workflow**: Gray (#6B7280)
- **Running Execution**: Blue (#3B82F6)
- **Success**: Green (#10B981)
- **Failed**: Red (#EF4444)
- **Pending**: Yellow (#F59E0B)

### Icons
- Trigger: ⚡
- Condition: ❓
- Action: ⚡
- Wait: ⏱️
- Approval: ✅
- Loop: 🔁

---

## 9. Next Steps

1. **Provide UI Path**: Tell me where you want to create the workflow UI
2. **Choose Library**: React Flow or custom canvas?
3. **Design System**: Material-UI, Ant Design, or custom?
4. **Start Implementation**: I'll create the components based on your preferences

---

## 10. Example: Minimal Workflow List Component

```jsx
// crm-front/notifications/src/routes/settings/Workflows.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';

const Workflows = () => {
  const [workflows, setWorkflows] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchWorkflows();
  }, []);

  const fetchWorkflows = async () => {
    try {
      const response = await axios.get('/api/v1/workflows', {
        headers: { 'X-Tenant-ID': 'tenant_1' }
      });
      setWorkflows(response.data);
    } catch (error) {
      console.error('Error fetching workflows:', error);
    } finally {
      setLoading(false);
    }
  };

  const toggleActive = async (id, isActive) => {
    const endpoint = isActive ? 'deactivate' : 'activate';
    await axios.post(`/api/v1/workflows/${id}/${endpoint}`, {}, {
      headers: { 'X-Tenant-ID': 'tenant_1' }
    });
    fetchWorkflows();
  };

  return (
    <div className="workflows-page">
      <div className="header">
        <h1>Workflows</h1>
        <button onClick={() => navigate('/settings/workflows/builder')}>
          + New Workflow
        </button>
      </div>

      {loading ? (
        <div>Loading...</div>
      ) : (
        <div className="workflow-list">
          {workflows.map(workflow => (
            <div key={workflow.id} className="workflow-card">
              <div className="workflow-header">
                <h3>{workflow.workflowName}</h3>
                <span className={`status ${workflow.isActive ? 'active' : 'inactive'}`}>
                  {workflow.isActive ? 'Active' : 'Inactive'}
                </span>
              </div>
              <p>{workflow.description}</p>
              <div className="workflow-meta">
                <span>Trigger: {workflow.triggerType}</span>
                <span>Module: {workflow.moduleType}</span>
              </div>
              <div className="workflow-actions">
                <button onClick={() => toggleActive(workflow.id, workflow.isActive)}>
                  {workflow.isActive ? 'Deactivate' : 'Activate'}
                </button>
                <button onClick={() => navigate(`/settings/workflows/builder/${workflow.id}`)}>
                  Edit
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default Workflows;
```

---

**Tell me the UI path and I'll start creating the complete workflow UI implementation!**
