# Workflow Builder UI - Node Interactions Guide

## Overview

This guide explains how users can interact with workflow nodes after dragging them onto the canvas.

## Node Interactions

### 1. **Adding a Node** (Drag & Drop)

```
Sidebar (Node Palette)          Canvas
┌─────────────────┐            ┌──────────────────────────┐
│ 📧 Send Email   │  ──drag──> │                          │
│ 📞 Send SMS     │            │   [📧 Send Email]        │
│ ⏰ Wait/Delay   │            │                          │
│ ❓ Condition    │            │                          │
└─────────────────┘            └──────────────────────────┘
```

**Implementation:**
```javascript
// When node is dropped on canvas
const handleNodeDrop = (nodeType, position) => {
  const newNode = {
    id: `node_${Date.now()}`,
    type: nodeType,
    position: position,
    data: {
      label: getDefaultLabel(nodeType),
      config: getDefaultConfig(nodeType)
    }
  };
  
  setNodes([...nodes, newNode]);
};
```

---

### 2. **Selecting a Node** (Click)

```
Before Click:                After Click:
┌──────────────┐            ┌──────────────┐
│ Send Email   │            │ Send Email   │ ← Blue border
└──────────────┘            └──────────────┘
                            
                            Properties Panel Opens →
                            ┌─────────────────────┐
                            │ Node Properties     │
                            │ ─────────────────── │
                            │ Type: Send Email    │
                            │ To: [________]      │
                            │ Subject: [_______]  │
                            │ Template: [▼]       │
                            └─────────────────────┘
```

**Implementation:**
```javascript
const handleNodeClick = (event, node) => {
  setSelectedNode(node);
  setShowPropertiesPanel(true);
};

// Visual feedback
<Node
  className={selectedNode?.id === node.id ? 'selected' : ''}
  onClick={(e) => handleNodeClick(e, node)}
/>

// CSS
.selected {
  border: 2px solid #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
}
```

---

### 3. **Deleting a Node** (Multiple Methods)

#### Method A: Delete Button on Node (Hover)

```
Hover over node:
┌──────────────────┐
│ Send Email    [×]│ ← Delete button appears
└──────────────────┘
```

**Implementation:**
```javascript
const WorkflowNode = ({ data, id }) => {
  const [isHovered, setIsHovered] = useState(false);
  
  const handleDelete = () => {
    setNodes(nodes.filter(n => n.id !== id));
    setEdges(edges.filter(e => e.source !== id && e.target !== id));
  };
  
  return (
    <div 
      className="workflow-node"
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      <div className="node-content">
        {data.label}
      </div>
      
      {isHovered && (
        <button 
          className="delete-btn"
          onClick={handleDelete}
        >
          ×
        </button>
      )}
    </div>
  );
};
```

#### Method B: Delete Key (Keyboard)

```javascript
// Listen for Delete/Backspace key
useEffect(() => {
  const handleKeyDown = (e) => {
    if ((e.key === 'Delete' || e.key === 'Backspace') && selectedNode) {
      e.preventDefault();
      deleteNode(selectedNode.id);
    }
  };
  
  window.addEventListener('keydown', handleKeyDown);
  return () => window.removeEventListener('keydown', handleKeyDown);
}, [selectedNode]);

const deleteNode = (nodeId) => {
  setNodes(nodes.filter(n => n.id !== nodeId));
  setEdges(edges.filter(e => e.source !== nodeId && e.target !== nodeId));
  setSelectedNode(null);
};
```

#### Method C: Context Menu (Right Click)

```
Right click on node:
┌──────────────────┐
│ Send Email       │
└──────────────────┘
        ↓
┌──────────────────┐
│ ✏️ Edit          │
│ 📋 Duplicate     │
│ 🗑️ Delete        │
│ ⚙️ Configure     │
└──────────────────┘
```

**Implementation:**
```javascript
const [contextMenu, setContextMenu] = useState(null);

const handleNodeContextMenu = (event, node) => {
  event.preventDefault();
  setContextMenu({
    x: event.clientX,
    y: event.clientY,
    node: node
  });
};

const handleDeleteFromMenu = () => {
  deleteNode(contextMenu.node.id);
  setContextMenu(null);
};

return (
  <>
    <Node onContextMenu={(e) => handleNodeContextMenu(e, node)} />
    
    {contextMenu && (
      <ContextMenu
        x={contextMenu.x}
        y={contextMenu.y}
        onClose={() => setContextMenu(null)}
      >
        <MenuItem onClick={() => handleEdit(contextMenu.node)}>
          ✏️ Edit
        </MenuItem>
        <MenuItem onClick={() => handleDuplicate(contextMenu.node)}>
          📋 Duplicate
        </MenuItem>
        <MenuItem onClick={handleDeleteFromMenu} danger>
          🗑️ Delete
        </MenuItem>
      </ContextMenu>
    )}
  </>
);
```

---

### 4. **Editing/Modifying a Node**

#### Method A: Double Click to Edit

```javascript
const handleNodeDoubleClick = (event, node) => {
  setEditingNode(node);
  setShowConfigModal(true);
};

<Node onDoubleClick={(e) => handleNodeDoubleClick(e, node)} />
```

#### Method B: Properties Panel (Side Panel)

```
Canvas                          Properties Panel
┌──────────────────┐           ┌─────────────────────────┐
│ [📧 Send Email]  │ ←select→  │ Send Email Node         │
└──────────────────┘           │ ─────────────────────── │
                               │ Label:                   │
                               │ [Send Welcome Email___] │
                               │                          │
                               │ Recipient:               │
                               │ ○ Contact Email          │
                               │ ○ Custom Email           │
                               │ [user@example.com_____] │
                               │                          │
                               │ Subject:                 │
                               │ [Welcome to our CRM___] │
                               │                          │
                               │ Template:                │
                               │ [Select Template ▼]     │
                               │                          │
                               │ [Cancel]  [Save Changes] │
                               └─────────────────────────┘
```

**Implementation:**
```javascript
const PropertiesPanel = ({ node, onUpdate, onClose }) => {
  const [config, setConfig] = useState(node.data.config);
  
  const handleSave = () => {
    const updatedNodes = nodes.map(n => 
      n.id === node.id 
        ? { ...n, data: { ...n.data, config } }
        : n
    );
    setNodes(updatedNodes);
    onClose();
  };
  
  return (
    <div className="properties-panel">
      <h3>{node.data.label}</h3>
      
      <div className="form-group">
        <label>Label</label>
        <input
          value={config.label}
          onChange={(e) => setConfig({...config, label: e.target.value})}
        />
      </div>
      
      {/* Node-specific fields */}
      {node.type === 'email' && (
        <>
          <div className="form-group">
            <label>Recipient</label>
            <input
              value={config.to}
              onChange={(e) => setConfig({...config, to: e.target.value})}
            />
          </div>
          
          <div className="form-group">
            <label>Subject</label>
            <input
              value={config.subject}
              onChange={(e) => setConfig({...config, subject: e.target.value})}
            />
          </div>
          
          <div className="form-group">
            <label>Template</label>
            <select
              value={config.templateId}
              onChange={(e) => setConfig({...config, templateId: e.target.value})}
            >
              <option value="">Select Template</option>
              <option value="welcome">Welcome Email</option>
              <option value="followup">Follow-up Email</option>
            </select>
          </div>
        </>
      )}
      
      <div className="button-group">
        <button onClick={onClose}>Cancel</button>
        <button onClick={handleSave} className="primary">Save Changes</button>
      </div>
    </div>
  );
};
```

#### Method C: Inline Editing (Quick Edit)

```
Click on node label to edit:
┌──────────────────┐           ┌──────────────────┐
│ Send Email       │  →click→  │ [Send Email___]  │ ← Editable
└──────────────────┘           └──────────────────┘
```

**Implementation:**
```javascript
const EditableNodeLabel = ({ node, onUpdate }) => {
  const [isEditing, setIsEditing] = useState(false);
  const [label, setLabel] = useState(node.data.label);
  
  const handleSave = () => {
    onUpdate(node.id, { ...node.data, label });
    setIsEditing(false);
  };
  
  if (isEditing) {
    return (
      <input
        value={label}
        onChange={(e) => setLabel(e.target.value)}
        onBlur={handleSave}
        onKeyPress={(e) => e.key === 'Enter' && handleSave()}
        autoFocus
      />
    );
  }
  
  return (
    <div onClick={() => setIsEditing(true)}>
      {label}
    </div>
  );
};
```

---

### 5. **Moving a Node** (Drag)

```javascript
// Using React Flow (recommended)
import ReactFlow, { useNodesState } from 'reactflow';

const WorkflowBuilder = () => {
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  
  return (
    <ReactFlow
      nodes={nodes}
      onNodesChange={onNodesChange} // Handles drag automatically
    />
  );
};

// Or custom implementation
const DraggableNode = ({ node, onMove }) => {
  const [isDragging, setIsDragging] = useState(false);
  const [position, setPosition] = useState(node.position);
  
  const handleMouseDown = (e) => {
    setIsDragging(true);
    // Store initial mouse position
  };
  
  const handleMouseMove = (e) => {
    if (isDragging) {
      const newPosition = {
        x: e.clientX - offset.x,
        y: e.clientY - offset.y
      };
      setPosition(newPosition);
    }
  };
  
  const handleMouseUp = () => {
    setIsDragging(false);
    onMove(node.id, position);
  };
  
  return (
    <div
      style={{
        position: 'absolute',
        left: position.x,
        top: position.y,
        cursor: isDragging ? 'grabbing' : 'grab'
      }}
      onMouseDown={handleMouseDown}
      onMouseMove={handleMouseMove}
      onMouseUp={handleMouseUp}
    >
      {node.data.label}
    </div>
  );
};
```

---

### 6. **Duplicating a Node**

```javascript
const duplicateNode = (nodeId) => {
  const nodeToDuplicate = nodes.find(n => n.id === nodeId);
  
  if (nodeToDuplicate) {
    const newNode = {
      ...nodeToDuplicate,
      id: `node_${Date.now()}`,
      position: {
        x: nodeToDuplicate.position.x + 50,
        y: nodeToDuplicate.position.y + 50
      },
      data: {
        ...nodeToDuplicate.data,
        label: `${nodeToDuplicate.data.label} (Copy)`
      }
    };
    
    setNodes([...nodes, newNode]);
  }
};
```

---

### 7. **Connecting Nodes** (Edges)

```
Drag from output handle to input handle:

[Node A] ○ ─────→ ○ [Node B]
         ↑         ↑
      output    input
```

**Implementation:**
```javascript
import ReactFlow, { addEdge } from 'reactflow';

const WorkflowBuilder = () => {
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  
  const onConnect = useCallback((params) => {
    setEdges((eds) => addEdge(params, eds));
  }, []);
  
  return (
    <ReactFlow
      nodes={nodes}
      edges={edges}
      onConnect={onConnect}
      onEdgesChange={onEdgesChange}
    />
  );
};
```

---

### 8. **Deleting Connections**

```javascript
// Method A: Click on edge to select, then press Delete
const handleEdgeClick = (event, edge) => {
  setSelectedEdge(edge);
};

useEffect(() => {
  const handleKeyDown = (e) => {
    if ((e.key === 'Delete' || e.key === 'Backspace') && selectedEdge) {
      setEdges(edges.filter(e => e.id !== selectedEdge.id));
      setSelectedEdge(null);
    }
  };
  
  window.addEventListener('keydown', handleKeyDown);
  return () => window.removeEventListener('keydown', handleKeyDown);
}, [selectedEdge]);

// Method B: Delete button on edge (hover)
<Edge
  onMouseEnter={() => setHoveredEdge(edge.id)}
  onMouseLeave={() => setHoveredEdge(null)}
>
  {hoveredEdge === edge.id && (
    <button onClick={() => deleteEdge(edge.id)}>×</button>
  )}
</Edge>
```

---

## Complete Example Component

```javascript
import React, { useState, useCallback } from 'react';
import ReactFlow, {
  addEdge,
  useNodesState,
  useEdgesState,
  Background,
  Controls,
  MiniMap
} from 'reactflow';
import 'reactflow/dist/style.css';

const WorkflowBuilder = () => {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const [selectedNode, setSelectedNode] = useState(null);
  const [showProperties, setShowProperties] = useState(false);

  // Add node
  const addNode = (type, position) => {
    const newNode = {
      id: `node_${Date.now()}`,
      type: 'default',
      position,
      data: {
        label: `${type} Node`,
        type: type,
        config: {}
      }
    };
    setNodes([...nodes, newNode]);
  };

  // Delete node
  const deleteNode = (nodeId) => {
    setNodes(nodes.filter(n => n.id !== nodeId));
    setEdges(edges.filter(e => e.source !== nodeId && e.target !== nodeId));
    setSelectedNode(null);
  };

  // Update node
  const updateNode = (nodeId, newData) => {
    setNodes(nodes.map(n => 
      n.id === nodeId ? { ...n, data: { ...n.data, ...newData } } : n
    ));
  };

  // Connect nodes
  const onConnect = useCallback((params) => {
    setEdges((eds) => addEdge(params, eds));
  }, []);

  // Node click
  const onNodeClick = (event, node) => {
    setSelectedNode(node);
    setShowProperties(true);
  };

  // Keyboard shortcuts
  useEffect(() => {
    const handleKeyDown = (e) => {
      if ((e.key === 'Delete' || e.key === 'Backspace') && selectedNode) {
        e.preventDefault();
        deleteNode(selectedNode.id);
      }
    };
    
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [selectedNode]);

  return (
    <div style={{ height: '100vh', display: 'flex' }}>
      {/* Node Palette */}
      <div style={{ width: '200px', borderRight: '1px solid #ddd', padding: '20px' }}>
        <h3>Nodes</h3>
        <div
          draggable
          onDragEnd={(e) => addNode('email', { x: e.clientX, y: e.clientY })}
          style={{ padding: '10px', margin: '5px', border: '1px solid #ccc', cursor: 'grab' }}
        >
          📧 Send Email
        </div>
        <div
          draggable
          onDragEnd={(e) => addNode('sms', { x: e.clientX, y: e.clientY })}
          style={{ padding: '10px', margin: '5px', border: '1px solid #ccc', cursor: 'grab' }}
        >
          📱 Send SMS
        </div>
        <div
          draggable
          onDragEnd={(e) => addNode('delay', { x: e.clientX, y: e.clientY })}
          style={{ padding: '10px', margin: '5px', border: '1px solid #ccc', cursor: 'grab' }}
        >
          ⏰ Delay
        </div>
      </div>

      {/* Canvas */}
      <div style={{ flex: 1 }}>
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          onNodeClick={onNodeClick}
          fitView
        >
          <Background />
          <Controls />
          <MiniMap />
        </ReactFlow>
      </div>

      {/* Properties Panel */}
      {showProperties && selectedNode && (
        <div style={{ width: '300px', borderLeft: '1px solid #ddd', padding: '20px' }}>
          <h3>Properties</h3>
          <button onClick={() => deleteNode(selectedNode.id)}>
            🗑️ Delete Node
          </button>
          <div>
            <label>Label:</label>
            <input
              value={selectedNode.data.label}
              onChange={(e) => updateNode(selectedNode.id, { label: e.target.value })}
            />
          </div>
        </div>
      )}
    </div>
  );
};

export default WorkflowBuilder;
```

---

## Summary of Interactions

| Action | Method | Shortcut |
|--------|--------|----------|
| **Add Node** | Drag from palette | - |
| **Select Node** | Click | - |
| **Delete Node** | Hover + × button | Delete/Backspace |
| **Edit Node** | Double-click or Properties panel | - |
| **Move Node** | Drag | - |
| **Duplicate Node** | Right-click → Duplicate | Ctrl+D |
| **Connect Nodes** | Drag from handle to handle | - |
| **Delete Connection** | Click edge + Delete | Delete |

This provides a complete, intuitive workflow builder experience!
