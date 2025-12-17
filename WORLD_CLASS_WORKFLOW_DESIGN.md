# 🎨 World-Class Workflow Design System

## Overview

This document outlines a comprehensive, modern workflow builder design inspired by industry leaders like Zapier, Make.com, n8n, and Salesforce Flow Builder.

## 🎯 Design Principles

### 1. **Visual Clarity**
- Clean, uncluttered interface
- Clear visual hierarchy
- Consistent color coding
- Intuitive iconography

### 2. **Ease of Use**
- Drag-and-drop simplicity
- Inline editing
- Smart defaults
- Contextual help

### 3. **Power & Flexibility**
- Advanced conditions
- Custom expressions
- Error handling
- Version control

### 4. **Performance**
- Fast rendering
- Smooth animations
- Responsive interactions
- Optimized for large workflows

---

## 🎨 Visual Design System

### Color Palette

```javascript
const workflowColors = {
  // Node Categories
  trigger: {
    primary: '#3b82f6',    // Blue
    light: '#dbeafe',
    dark: '#1e40af'
  },
  action: {
    primary: '#22c55e',    // Green
    light: '#dcfce7',
    dark: '#15803d'
  },
  condition: {
    primary: '#eab308',    // Yellow
    light: '#fef9c3',
    dark: '#a16207'
  },
  communication: {
    primary: '#8b5cf6',    // Purple
    light: '#ede9fe',
    dark: '#6d28d9'
  },
  data: {
    primary: '#06b6d4',    // Cyan
    light: '#cffafe',
    dark: '#0e7490'
  },
  error: {
    primary: '#ef4444',    // Red
    light: '#fee2e2',
    dark: '#b91c1c'
  },
  
  // UI Elements
  canvas: '#f9fafb',
  grid: '#e5e7eb',
  border: '#d1d5db',
  text: '#111827',
  textLight: '#6b7280'
};
```


### Node Design Specifications

#### Standard Node (200px × 80px)
```
┌────────────────────────────────────┐
│ 🔵 TRIGGER                    [×][⎘]│  ← Header (40px)
├────────────────────────────────────┤
│ Lead Created                       │  ← Content (40px)
│ ○                                  │  ← Connection handle
└────────────────────────────────────┘
```

#### Condition Node (220px × 100px)
```
┌────────────────────────────────────┐
│ ⚠️ CONDITION                  [×][⎘]│
├────────────────────────────────────┤
│ Lead Score > 80                    │
│                                    │
│ ○ True          False ○            │  ← Dual handles
└────────────────────────────────────┘
```

#### Communication Node (200px × 90px)
```
┌────────────────────────────────────┐
│ 📧 EMAIL                      [×][⎘]│
├────────────────────────────────────┤
│ Send Welcome Email                 │
│ To: {{contact.email}}              │
│ ○                                  │
└────────────────────────────────────┘
```

---

## 🏗️ Component Architecture

### 1. Node Library (Left Sidebar - 280px)

```jsx
<NodeLibrary>
  <SearchBar placeholder="Search nodes..." />
  
  <CategorySection title="Triggers" icon="⚡" color="blue">
    <NodeItem type="trigger" subtype="record_created">
      <Icon>📝</Icon>
      <Label>Record Created</Label>
      <Description>When a new record is created</Description>
    </NodeItem>
    <NodeItem type="trigger" subtype="record_updated">
      <Icon>✏️</Icon>
      <Label>Record Updated</Label>
    </NodeItem>
    <NodeItem type="trigger" subtype="scheduled">
      <Icon>⏰</Icon>
      <Label>Scheduled</Label>
    </NodeItem>
  </CategorySection>
  
  <CategorySection title="Actions" icon="⚡" color="green">
    <NodeItem type="action" subtype="create_record">
      <Icon>➕</Icon>
      <Label>Create Record</Label>
    </NodeItem>
    <NodeItem type="action" subtype="update_record">
      <Icon>✏️</Icon>
      <Label>Update Record</Label>
    </NodeItem>
    <NodeItem type="action" subtype="delete_record">
      <Icon>🗑️</Icon>
      <Label>Delete Record</Label>
    </NodeItem>
  </CategorySection>
  
  <CategorySection title="Logic" icon="🧠" color="yellow">
    <NodeItem type="condition" subtype="if_else">
      <Icon>❓</Icon>
      <Label>Condition</Label>
    </NodeItem>
    <NodeItem type="condition" subtype="switch">
      <Icon>🔀</Icon>
      <Label>Switch</Label>
    </NodeItem>
  </CategorySection>
  
  <CategorySection title="Communication" icon="📢" color="purple">
    <NodeItem type="communication" subtype="email">
      <Icon>📧</Icon>
      <Label>Send Email</Label>
    </NodeItem>
    <NodeItem type="communication" subtype="sms">
      <Icon>📱</Icon>
      <Label>Send SMS</Label>
    </NodeItem>
    <NodeItem type="communication" subtype="whatsapp">
      <Icon>💬</Icon>
      <Label>WhatsApp</Label>
    </NodeItem>
  </CategorySection>
  
  <CategorySection title="Data" icon="💾" color="cyan">
    <NodeItem type="data" subtype="transform">
      <Icon>🔄</Icon>
      <Label>Transform Data</Label>
    </NodeItem>
    <NodeItem type="data" subtype="filter">
      <Icon>🔍</Icon>
      <Label>Filter Records</Label>
    </NodeItem>
  </CategorySection>
</NodeLibrary>
```


### 2. Canvas Area (Center - Flexible)

```jsx
<WorkflowCanvas>
  <Toolbar>
    <ToolbarLeft>
      <BackButton />
      <WorkflowName editable />
      <StatusBadge active={isActive} />
    </ToolbarLeft>
    
    <ToolbarCenter>
      <ZoomControls />
      <FitViewButton />
      <UndoButton />
      <RedoButton />
    </ToolbarCenter>
    
    <ToolbarRight>
      <TestButton />
      <SaveButton />
      <PublishButton />
      <MoreMenu />
    </ToolbarRight>
  </Toolbar>
  
  <ReactFlowCanvas
    nodes={nodes}
    edges={edges}
    snapToGrid
    gridSize={15}
  >
    <Background variant="dots" />
    <MiniMap />
    <Controls />
  </ReactFlowCanvas>
  
  <FloatingActionButton>
    <AddNodeButton />
    <TemplatesButton />
    <HelpButton />
  </FloatingActionButton>
</WorkflowCanvas>
```

### 3. Properties Panel (Right Sidebar - 360px)

```jsx
<PropertiesPanel node={selectedNode}>
  <PanelHeader>
    <NodeIcon type={node.type} />
    <NodeTitle editable>{node.data.label}</NodeTitle>
    <CloseButton />
  </PanelHeader>
  
  <PanelTabs>
    <Tab active>Configuration</Tab>
    <Tab>Advanced</Tab>
    <Tab>Testing</Tab>
  </PanelTabs>
  
  <PanelContent>
    {/* Dynamic based on node type */}
    {node.type === 'trigger' && <TriggerConfig />}
    {node.type === 'action' && <ActionConfig />}
    {node.type === 'condition' && <ConditionConfig />}
    {node.type === 'communication' && <CommunicationConfig />}
  </PanelContent>
  
  <PanelFooter>
    <DeleteButton />
    <DuplicateButton />
    <SaveButton primary />
  </PanelFooter>
</PropertiesPanel>
```

---

## 🎭 Node Types & Configurations

### 1. Trigger Nodes

#### Record Created Trigger
```jsx
<TriggerConfig>
  <FormGroup>
    <Label>Module</Label>
    <Select>
      <Option value="LEAD">Leads</Option>
      <Option value="CONTACT">Contacts</Option>
      <Option value="ACCOUNT">Accounts</Option>
      <Option value="DEAL">Deals</Option>
    </Select>
  </FormGroup>
  
  <FormGroup>
    <Label>Conditions (Optional)</Label>
    <ConditionBuilder>
      <Condition>
        <FieldSelect placeholder="Select field" />
        <OperatorSelect>
          <Option value="equals">Equals</Option>
          <Option value="contains">Contains</Option>
          <Option value="greater_than">Greater than</Option>
        </OperatorSelect>
        <ValueInput placeholder="Value" />
      </Condition>
      <AddConditionButton />
    </ConditionBuilder>
  </FormGroup>
</TriggerConfig>
```

#### Scheduled Trigger
```jsx
<TriggerConfig>
  <FormGroup>
    <Label>Schedule Type</Label>
    <RadioGroup>
      <Radio value="daily">Daily</Radio>
      <Radio value="weekly">Weekly</Radio>
      <Radio value="monthly">Monthly</Radio>
      <Radio value="custom">Custom (Cron)</Radio>
    </RadioGroup>
  </FormGroup>
  
  <FormGroup>
    <Label>Time</Label>
    <TimePicker />
  </FormGroup>
  
  <FormGroup>
    <Label>Timezone</Label>
    <Select>
      <Option value="UTC">UTC</Option>
      <Option value="America/New_York">Eastern Time</Option>
      <Option value="Asia/Kolkata">India Standard Time</Option>
    </Select>
  </FormGroup>
</TriggerConfig>
```


### 2. Action Nodes

#### Create/Update Record Action
```jsx
<ActionConfig>
  <FormGroup>
    <Label>Action Type</Label>
    <Select>
      <Option value="create">Create New Record</Option>
      <Option value="update">Update Existing Record</Option>
    </Select>
  </FormGroup>
  
  <FormGroup>
    <Label>Module</Label>
    <Select>
      <Option value="LEAD">Lead</Option>
      <Option value="CONTACT">Contact</Option>
      <Option value="ACCOUNT">Account</Option>
    </Select>
  </FormGroup>
  
  <FormGroup>
    <Label>Field Mapping</Label>
    <FieldMapper>
      <FieldRow>
        <FieldName>First Name</FieldName>
        <FieldValue>
          <Input placeholder="Enter value or {{variable}}" />
          <VariablePickerButton />
        </FieldValue>
      </FieldRow>
      <FieldRow>
        <FieldName>Email</FieldName>
        <FieldValue>
          <Input value="{{trigger.email}}" />
        </FieldValue>
      </FieldRow>
      <FieldRow>
        <FieldName>Lead Source</FieldName>
        <FieldValue>
          <Select>
            <Option value="Website">Website</Option>
            <Option value="Referral">Referral</Option>
          </Select>
        </FieldValue>
      </FieldRow>
      <AddFieldButton />
    </FieldMapper>
  </FormGroup>
</ActionConfig>
```

#### Assign Task Action
```jsx
<ActionConfig>
  <FormGroup>
    <Label>Task Title</Label>
    <Input placeholder="Follow up with {{contact.name}}" />
  </FormGroup>
  
  <FormGroup>
    <Label>Assign To</Label>
    <UserPicker>
      <Option value="owner">Record Owner</Option>
      <Option value="specific">Specific User</Option>
      <Option value="team">Team</Option>
    </UserPicker>
  </FormGroup>
  
  <FormGroup>
    <Label>Due Date</Label>
    <DatePicker>
      <Option value="immediate">Immediately</Option>
      <Option value="1_day">1 Day Later</Option>
      <Option value="3_days">3 Days Later</Option>
      <Option value="custom">Custom</Option>
    </DatePicker>
  </FormGroup>
  
  <FormGroup>
    <Label>Priority</Label>
    <Select>
      <Option value="high">High</Option>
      <Option value="medium">Medium</Option>
      <Option value="low">Low</Option>
    </Select>
  </FormGroup>
</ActionConfig>
```

### 3. Condition Nodes

#### If/Else Condition
```jsx
<ConditionConfig>
  <FormGroup>
    <Label>Condition Logic</Label>
    <RadioGroup>
      <Radio value="all">All conditions must be true (AND)</Radio>
      <Radio value="any">Any condition must be true (OR)</Radio>
      <Radio value="custom">Custom logic</Radio>
    </RadioGroup>
  </FormGroup>
  
  <FormGroup>
    <Label>Conditions</Label>
    <ConditionBuilder>
      <ConditionRow>
        <FieldSelect>
          <Option value="lead.score">Lead Score</Option>
          <Option value="lead.status">Lead Status</Option>
          <Option value="lead.source">Lead Source</Option>
        </FieldSelect>
        <OperatorSelect>
          <Option value="equals">Equals</Option>
          <Option value="not_equals">Not Equals</Option>
          <Option value="greater_than">Greater Than</Option>
          <Option value="less_than">Less Than</Option>
          <Option value="contains">Contains</Option>
          <Option value="is_empty">Is Empty</Option>
        </OperatorSelect>
        <ValueInput placeholder="Value" />
        <RemoveButton />
      </ConditionRow>
      
      <ConditionRow>
        <FieldSelect value="lead.status" />
        <OperatorSelect value="equals" />
        <ValueInput value="Hot" />
        <RemoveButton />
      </ConditionRow>
      
      <AddConditionButton />
    </ConditionBuilder>
  </FormGroup>
  
  <FormGroup>
    <Label>Custom Logic (Optional)</Label>
    <Input placeholder="1 AND (2 OR 3)" />
    <HelpText>Use condition numbers with AND/OR operators</HelpText>
  </FormGroup>
</ConditionConfig>
```


### 4. Communication Nodes

#### Email Node
```jsx
<CommunicationConfig>
  <FormGroup>
    <Label>Recipient</Label>
    <RecipientBuilder>
      <RadioGroup>
        <Radio value="record">From Record</Radio>
        <Radio value="user">Specific User</Radio>
        <Radio value="custom">Custom Email</Radio>
      </RadioGroup>
      
      {recipientType === 'record' && (
        <FieldSelect>
          <Option value="{{trigger.email}}">Trigger Email</Option>
          <Option value="{{contact.email}}">Contact Email</Option>
          <Option value="{{account.email}}">Account Email</Option>
        </FieldSelect>
      )}
      
      {recipientType === 'custom' && (
        <Input placeholder="email@example.com" />
      )}
    </RecipientBuilder>
  </FormGroup>
  
  <FormGroup>
    <Label>Subject</Label>
    <Input placeholder="Welcome to our CRM, {{contact.firstName}}!" />
    <VariablePickerButton />
  </FormGroup>
  
  <FormGroup>
    <Label>Email Template</Label>
    <TemplateSelect>
      <Option value="welcome">Welcome Email</Option>
      <Option value="followup">Follow-up Email</Option>
      <Option value="custom">Custom</Option>
    </TemplateSelect>
  </FormGroup>
  
  <FormGroup>
    <Label>Body</Label>
    <RichTextEditor>
      <Toolbar>
        <BoldButton />
        <ItalicButton />
        <LinkButton />
        <VariableButton />
      </Toolbar>
      <Editor placeholder="Compose your email..." />
    </RichTextEditor>
  </FormGroup>
  
  <FormGroup>
    <Label>Attachments (Optional)</Label>
    <FileUploader />
  </FormGroup>
</CommunicationConfig>
```

#### SMS Node
```jsx
<CommunicationConfig>
  <FormGroup>
    <Label>Phone Number</Label>
    <PhoneInput>
      <RadioGroup>
        <Radio value="record">From Record</Radio>
        <Radio value="custom">Custom Number</Radio>
      </RadioGroup>
      <Input placeholder="+1 (555) 123-4567" />
    </PhoneInput>
  </FormGroup>
  
  <FormGroup>
    <Label>Message</Label>
    <Textarea 
      placeholder="Hi {{contact.firstName}}, ..."
      maxLength={160}
    />
    <CharacterCount>120/160 characters</CharacterCount>
  </FormGroup>
  
  <FormGroup>
    <Label>SMS Provider</Label>
    <Select>
      <Option value="twilio">Twilio</Option>
      <Option value="default">Default Provider</Option>
    </Select>
  </FormGroup>
</CommunicationConfig>
```

### 5. Data Transformation Nodes

#### Transform Data Node
```jsx
<DataConfig>
  <FormGroup>
    <Label>Transformation Type</Label>
    <Select>
      <Option value="map">Map Fields</Option>
      <Option value="filter">Filter Records</Option>
      <Option value="aggregate">Aggregate Data</Option>
      <Option value="formula">Apply Formula</Option>
    </Select>
  </FormGroup>
  
  <FormGroup>
    <Label>Formula</Label>
    <FormulaBuilder>
      <FormulaInput placeholder="{{lead.score}} * 1.5" />
      <FunctionLibrary>
        <FunctionCategory title="Math">
          <Function name="SUM" />
          <Function name="AVG" />
          <Function name="MAX" />
          <Function name="MIN" />
        </FunctionCategory>
        <FunctionCategory title="Text">
          <Function name="CONCAT" />
          <Function name="UPPER" />
          <Function name="LOWER" />
          <Function name="TRIM" />
        </FunctionCategory>
        <FunctionCategory title="Date">
          <Function name="NOW" />
          <Function name="TODAY" />
          <Function name="DATEADD" />
          <Function name="DATEDIFF" />
        </FunctionCategory>
      </FunctionLibrary>
    </FormulaBuilder>
  </FormGroup>
  
  <FormGroup>
    <Label>Output Variable</Label>
    <Input placeholder="transformedScore" />
  </FormGroup>
</DataConfig>
```

---

## 🎨 Advanced UI Components

### Variable Picker
```jsx
<VariablePicker>
  <SearchBar placeholder="Search variables..." />
  
  <VariableCategory title="Trigger Data">
    <Variable path="trigger.id" type="number" />
    <Variable path="trigger.name" type="string" />
    <Variable path="trigger.email" type="string" />
    <Variable path="trigger.createdAt" type="datetime" />
  </VariableCategory>
  
  <VariableCategory title="Previous Steps">
    <Variable path="step1.result" type="object" />
    <Variable path="step2.recordId" type="number" />
  </VariableCategory>
  
  <VariableCategory title="System Variables">
    <Variable path="$currentUser.id" type="number" />
    <Variable path="$currentUser.name" type="string" />
    <Variable path="$now" type="datetime" />
    <Variable path="$today" type="date" />
  </VariableCategory>
</VariablePicker>
```


### Condition Builder
```jsx
<ConditionBuilder>
  <ConditionGroup logic="AND">
    <Condition>
      <FieldSelect 
        value="lead.score"
        options={availableFields}
      />
      <OperatorSelect 
        value="greater_than"
        options={numericOperators}
      />
      <ValueInput 
        value="80"
        type="number"
      />
      <RemoveButton />
    </Condition>
    
    <LogicToggle value="AND" onChange={toggleLogic} />
    
    <Condition>
      <FieldSelect value="lead.status" />
      <OperatorSelect value="equals" />
      <ValueSelect>
        <Option value="Hot">Hot</Option>
        <Option value="Warm">Warm</Option>
        <Option value="Cold">Cold</Option>
      </ValueSelect>
      <RemoveButton />
    </Condition>
    
    <AddConditionButton />
    <AddGroupButton />
  </ConditionGroup>
</ConditionBuilder>
```

### Field Mapper
```jsx
<FieldMapper>
  <MappingHeader>
    <SourceColumn>Source Field</SourceColumn>
    <ArrowColumn>→</ArrowColumn>
    <TargetColumn>Target Field</TargetColumn>
  </MappingHeader>
  
  <MappingRow>
    <SourceField>
      <VariableInput value="{{trigger.firstName}}" />
    </SourceField>
    <Arrow>→</Arrow>
    <TargetField>
      <FieldSelect value="contact.firstName" />
    </TargetField>
    <RemoveButton />
  </MappingRow>
  
  <MappingRow>
    <SourceField>
      <VariableInput value="{{trigger.email}}" />
    </SourceField>
    <Arrow>→</Arrow>
    <TargetField>
      <FieldSelect value="contact.email" />
    </TargetField>
    <RemoveButton />
  </MappingRow>
  
  <AddMappingButton />
  <AutoMapButton />
</FieldMapper>
```

---

## 🎬 Interactions & Animations

### Node Hover Effects
```css
.workflow-node {
  transition: all 0.2s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.workflow-node:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.workflow-node.selected {
  border: 2px solid #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
}

.workflow-node.error {
  border-color: #ef4444;
  animation: shake 0.5s;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-5px); }
  75% { transform: translateX(5px); }
}
```

### Edge Animations
```css
.react-flow__edge-path {
  stroke-width: 2;
  transition: stroke-width 0.2s;
}

.react-flow__edge:hover .react-flow__edge-path {
  stroke-width: 3;
}

.react-flow__edge.animated .react-flow__edge-path {
  stroke-dasharray: 5;
  animation: dashdraw 0.5s linear infinite;
}

@keyframes dashdraw {
  to {
    stroke-dashoffset: -10;
  }
}

.react-flow__edge.success {
  stroke: #22c55e;
}

.react-flow__edge.error {
  stroke: #ef4444;
  stroke-dasharray: 5, 5;
}
```

### Drag & Drop Feedback
```jsx
const NodeLibraryItem = ({ node }) => {
  const [isDragging, setIsDragging] = useState(false);
  
  return (
    <div
      draggable
      onDragStart={(e) => {
        setIsDragging(true);
        e.dataTransfer.setData('application/reactflow', JSON.stringify(node));
        e.dataTransfer.effectAllowed = 'move';
      }}
      onDragEnd={() => setIsDragging(false)}
      className={`
        node-library-item
        ${isDragging ? 'opacity-50 scale-95' : 'opacity-100 scale-100'}
        transition-all duration-200
        cursor-grab active:cursor-grabbing
      `}
    >
      <NodeIcon type={node.type} />
      <NodeLabel>{node.label}</NodeLabel>
    </div>
  );
};
```

---

## 📱 Responsive Design

### Mobile Layout (< 768px)
```jsx
<MobileWorkflowBuilder>
  <MobileHeader>
    <BackButton />
    <WorkflowName />
    <MenuButton />
  </MobileHeader>
  
  <MobileCanvas>
    {/* Simplified canvas with touch gestures */}
    <TouchCanvas
      onPinch={handleZoom}
      onPan={handlePan}
      onTap={handleNodeSelect}
    />
  </MobileCanvas>
  
  <MobileBottomSheet>
    {showNodeLibrary && <NodeLibrary />}
    {showProperties && <PropertiesPanel />}
  </MobileBottomSheet>
  
  <MobileFAB>
    <AddNodeButton />
  </MobileFAB>
</MobileWorkflowBuilder>
```

### Tablet Layout (768px - 1024px)
```jsx
<TabletWorkflowBuilder>
  <CollapsibleSidebar>
    <NodeLibrary />
  </CollapsibleSidebar>
  
  <MainCanvas />
  
  <ModalPropertiesPanel>
    {/* Opens as modal instead of sidebar */}
  </ModalPropertiesPanel>
</TabletWorkflowBuilder>
```


---

## 🧪 Testing & Debugging Features

### Test Mode
```jsx
<TestPanel>
  <TestHeader>
    <Title>Test Workflow</Title>
    <CloseButton />
  </TestHeader>
  
  <TestInputs>
    <FormGroup>
      <Label>Test Data</Label>
      <JSONEditor
        value={testData}
        onChange={setTestData}
        placeholder={{
          "lead": {
            "firstName": "John",
            "lastName": "Doe",
            "email": "john@example.com",
            "score": 85
          }
        }}
      />
    </FormGroup>
  </TestInputs>
  
  <TestActions>
    <RunTestButton onClick={runTest} />
    <ClearButton onClick={clearResults} />
  </TestActions>
  
  <TestResults>
    <ExecutionTimeline>
      <TimelineItem status="success">
        <NodeName>Trigger: Lead Created</NodeName>
        <Duration>0.1s</Duration>
        <Output>{JSON.stringify(output)}</Output>
      </TimelineItem>
      
      <TimelineItem status="success">
        <NodeName>Condition: Score > 80</NodeName>
        <Duration>0.05s</Duration>
        <Result>True</Result>
      </TimelineItem>
      
      <TimelineItem status="success">
        <NodeName>Action: Send Email</NodeName>
        <Duration>0.3s</Duration>
        <Output>Email sent to john@example.com</Output>
      </TimelineItem>
    </ExecutionTimeline>
  </TestResults>
</TestPanel>
```

### Debug Mode
```jsx
<DebugPanel>
  <DebugControls>
    <PlayButton />
    <StepButton />
    <PauseButton />
    <StopButton />
  </DebugControls>
  
  <BreakpointsList>
    <Breakpoint nodeId="node-1" enabled />
    <Breakpoint nodeId="node-3" enabled />
  </BreakpointsList>
  
  <VariableInspector>
    <Variable name="trigger.data" value={...} />
    <Variable name="step1.result" value={...} />
    <Variable name="currentNode" value="node-2" />
  </VariableInspector>
  
  <ExecutionLog>
    <LogEntry level="info">Workflow started</LogEntry>
    <LogEntry level="success">Node 1 executed successfully</LogEntry>
    <LogEntry level="warning">Condition evaluated to false</LogEntry>
    <LogEntry level="error">Email send failed</LogEntry>
  </ExecutionLog>
</DebugPanel>
```

---

## 📊 Analytics & Monitoring

### Workflow Analytics Dashboard
```jsx
<AnalyticsDashboard>
  <MetricsGrid>
    <MetricCard>
      <MetricLabel>Total Executions</MetricLabel>
      <MetricValue>1,234</MetricValue>
      <MetricTrend>+12% from last week</MetricTrend>
    </MetricCard>
    
    <MetricCard>
      <MetricLabel>Success Rate</MetricLabel>
      <MetricValue>94.5%</MetricValue>
      <MetricTrend>+2.3% from last week</MetricTrend>
    </MetricCard>
    
    <MetricCard>
      <MetricLabel>Avg. Execution Time</MetricLabel>
      <MetricValue>2.3s</MetricValue>
      <MetricTrend>-0.5s from last week</MetricTrend>
    </MetricCard>
    
    <MetricCard>
      <MetricLabel>Error Rate</MetricLabel>
      <MetricValue>5.5%</MetricValue>
      <MetricTrend status="warning">+1.2% from last week</MetricTrend>
    </MetricCard>
  </MetricsGrid>
  
  <ExecutionChart>
    <LineChart
      data={executionData}
      xAxis="date"
      yAxis="count"
      series={['success', 'failed']}
    />
  </ExecutionChart>
  
  <NodePerformance>
    <Table>
      <TableHeader>
        <Column>Node</Column>
        <Column>Executions</Column>
        <Column>Avg. Time</Column>
        <Column>Error Rate</Column>
      </TableHeader>
      <TableBody>
        <Row>
          <Cell>Send Email</Cell>
          <Cell>1,234</Cell>
          <Cell>0.3s</Cell>
          <Cell>2.1%</Cell>
        </Row>
        <Row>
          <Cell>Update Record</Cell>
          <Cell>1,234</Cell>
          <Cell>0.15s</Cell>
          <Cell>0.5%</Cell>
        </Row>
      </TableBody>
    </Table>
  </NodePerformance>
</AnalyticsDashboard>
```

---

## 🎯 Best Practices & Patterns

### 1. Error Handling Pattern
```
┌─────────────┐
│   Action    │
└──────┬──────┘
       │
       ├─── Success ──→ [Next Node]
       │
       └─── Error ──→ [Error Handler]
                      └──→ [Send Alert]
                      └──→ [Log Error]
                      └──→ [Retry Logic]
```

### 2. Approval Workflow Pattern
```
┌─────────────┐
│   Trigger   │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Condition  │ (Amount > $10,000?)
└──────┬──────┘
       │
       ├─── Yes ──→ [Request Approval]
       │            └──→ [Wait for Response]
       │                 ├─── Approved ──→ [Process]
       │                 └─── Rejected ──→ [Notify]
       │
       └─── No ──→ [Auto-Approve]
                   └──→ [Process]
```

### 3. Lead Nurturing Pattern
```
┌─────────────┐
│ Lead Created│
└──────┬──────┘
       │
       ▼
┌─────────────┐
│Send Welcome │
│   Email     │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Wait 2 Days│
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Condition  │ (Opened Email?)
└──────┬──────┘
       │
       ├─── Yes ──→ [Send Follow-up]
       │            └──→ [Assign to Sales]
       │
       └─── No ──→ [Wait 3 Days]
                   └──→ [Send Reminder]
```


### 4. Data Enrichment Pattern
```
┌─────────────┐
│Contact Added│
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Condition  │ (Has Company?)
└──────┬──────┘
       │
       ├─── Yes ──→ [Enrich Company Data]
       │            └──→ [Update Record]
       │
       └─── No ──→ [Skip Enrichment]
```

### 5. Multi-Channel Communication Pattern
```
┌─────────────┐
│Deal Closed  │
└──────┬──────┘
       │
       ├──→ [Send Email Confirmation]
       │
       ├──→ [Send SMS Notification]
       │
       ├──→ [Create Task for Onboarding]
       │
       └──→ [Update CRM Status]
```

---

## 🚀 Performance Optimization

### Lazy Loading Nodes
```jsx
const nodeTypes = {
  trigger: lazy(() => import('./nodes/TriggerNode')),
  action: lazy(() => import('./nodes/ActionNode')),
  condition: lazy(() => import('./nodes/ConditionNode')),
  // ... other node types
};

<Suspense fallback={<NodeSkeleton />}>
  <ReactFlow nodeTypes={nodeTypes} />
</Suspense>
```

### Virtual Scrolling for Large Workflows
```jsx
import { FixedSizeList } from 'react-window';

<NodeLibrary>
  <FixedSizeList
    height={600}
    itemCount={nodeCategories.length}
    itemSize={80}
  >
    {({ index, style }) => (
      <NodeCategory style={style} data={nodeCategories[index]} />
    )}
  </FixedSizeList>
</NodeLibrary>
```

### Debounced Auto-Save
```jsx
const debouncedSave = useMemo(
  () => debounce((workflow) => {
    workflowService.saveWorkflow(workflow);
    toast.success('Workflow saved');
  }, 2000),
  []
);

useEffect(() => {
  if (nodes.length > 0 || edges.length > 0) {
    debouncedSave({ nodes, edges, name: workflowName });
  }
}, [nodes, edges, workflowName]);
```

---

## 🎨 Theme Customization

### Light Theme
```javascript
const lightTheme = {
  canvas: '#f9fafb',
  grid: '#e5e7eb',
  node: {
    background: '#ffffff',
    border: '#d1d5db',
    text: '#111827',
    shadow: '0 2px 4px rgba(0, 0, 0, 0.1)'
  },
  edge: {
    stroke: '#9ca3af',
    strokeActive: '#3b82f6'
  },
  sidebar: {
    background: '#ffffff',
    border: '#e5e7eb'
  }
};
```

### Dark Theme
```javascript
const darkTheme = {
  canvas: '#111827',
  grid: '#374151',
  node: {
    background: '#1f2937',
    border: '#4b5563',
    text: '#f9fafb',
    shadow: '0 2px 4px rgba(0, 0, 0, 0.3)'
  },
  edge: {
    stroke: '#6b7280',
    strokeActive: '#60a5fa'
  },
  sidebar: {
    background: '#1f2937',
    border: '#374151'
  }
};
```

---

## 📚 Template Library

### Pre-built Workflow Templates
```jsx
<TemplateGallery>
  <TemplateCard>
    <TemplateImage src="lead-nurturing.png" />
    <TemplateName>Lead Nurturing Campaign</TemplateName>
    <TemplateDescription>
      Automatically nurture leads with a series of emails
    </TemplateDescription>
    <TemplateStats>
      <Stat>
        <Icon>👥</Icon>
        <Value>1,234</Value>
        <Label>Users</Label>
      </Stat>
      <Stat>
        <Icon>⭐</Icon>
        <Value>4.8</Value>
        <Label>Rating</Label>
      </Stat>
    </TemplateStats>
    <UseTemplateButton />
  </TemplateCard>
  
  <TemplateCard>
    <TemplateImage src="deal-approval.png" />
    <TemplateName>Deal Approval Process</TemplateName>
    <TemplateDescription>
      Route high-value deals for manager approval
    </TemplateDescription>
    <UseTemplateButton />
  </TemplateCard>
  
  <TemplateCard>
    <TemplateImage src="task-automation.png" />
    <TemplateName>Task Automation</TemplateName>
    <TemplateDescription>
      Automatically create and assign tasks
    </TemplateDescription>
    <UseTemplateButton />
  </TemplateCard>
</TemplateGallery>
```

### Template Categories
- **Sales Automation**
  - Lead Assignment
  - Deal Progression
  - Quote Generation
  - Follow-up Reminders

- **Marketing Automation**
  - Email Campaigns
  - Lead Scoring
  - Segmentation
  - Drip Campaigns

- **Customer Service**
  - Ticket Routing
  - SLA Management
  - Escalation Rules
  - Customer Feedback

- **Operations**
  - Data Validation
  - Record Cleanup
  - Reporting
  - Integration Sync

---

## 🔐 Security & Permissions

### Permission Levels
```jsx
<PermissionMatrix>
  <Permission role="Admin">
    <Action>Create Workflow</Action>
    <Action>Edit Workflow</Action>
    <Action>Delete Workflow</Action>
    <Action>Activate/Deactivate</Action>
    <Action>View Analytics</Action>
  </Permission>
  
  <Permission role="Manager">
    <Action>Create Workflow</Action>
    <Action>Edit Own Workflows</Action>
    <Action>View Analytics</Action>
  </Permission>
  
  <Permission role="User">
    <Action>View Workflows</Action>
    <Action>Execute Manual Workflows</Action>
  </Permission>
</PermissionMatrix>
```

### Audit Trail
```jsx
<AuditLog>
  <LogEntry>
    <Timestamp>2024-12-10 10:30 AM</Timestamp>
    <User>John Doe</User>
    <Action>Created workflow "Lead Nurturing"</Action>
  </LogEntry>
  
  <LogEntry>
    <Timestamp>2024-12-10 11:15 AM</Timestamp>
    <User>Jane Smith</User>
    <Action>Activated workflow "Lead Nurturing"</Action>
  </LogEntry>
  
  <LogEntry>
    <Timestamp>2024-12-10 02:45 PM</Timestamp>
    <User>John Doe</User>
    <Action>Modified node "Send Email"</Action>
  </LogEntry>
</AuditLog>
```

---

## 📱 Mobile App Design

### Mobile Workflow Builder
```jsx
<MobileWorkflowApp>
  <MobileHeader>
    <BackButton />
    <Title>Lead Nurturing</Title>
    <MoreButton />
  </MobileHeader>
  
  <MobileCanvas>
    {/* Vertical flow for mobile */}
    <VerticalFlow>
      <MobileNode type="trigger">
        <NodeIcon>⚡</NodeIcon>
        <NodeLabel>Lead Created</NodeLabel>
        <EditButton />
      </MobileNode>
      
      <FlowArrow />
      
      <MobileNode type="action">
        <NodeIcon>📧</NodeIcon>
        <NodeLabel>Send Welcome Email</NodeLabel>
        <EditButton />
      </MobileNode>
      
      <FlowArrow />
      
      <MobileNode type="delay">
        <NodeIcon>⏰</NodeIcon>
        <NodeLabel>Wait 2 Days</NodeLabel>
        <EditButton />
      </MobileNode>
    </VerticalFlow>
  </MobileCanvas>
  
  <MobileBottomNav>
    <NavItem icon="➕" label="Add Node" />
    <NavItem icon="▶️" label="Test" />
    <NavItem icon="💾" label="Save" />
  </MobileBottomNav>
</MobileWorkflowApp>
```


---

## 🎓 User Onboarding

### First-Time User Experience
```jsx
<OnboardingTour>
  <Step number={1}>
    <Spotlight target=".node-library">
      <Title>Welcome to Workflow Builder!</Title>
      <Description>
        Drag nodes from this library to create your workflow
      </Description>
      <NextButton />
    </Spotlight>
  </Step>
  
  <Step number={2}>
    <Spotlight target=".canvas">
      <Title>Canvas Area</Title>
      <Description>
        Drop nodes here and connect them to build your automation
      </Description>
      <PrevButton />
      <NextButton />
    </Spotlight>
  </Step>
  
  <Step number={3}>
    <Spotlight target=".properties-panel">
      <Title>Properties Panel</Title>
      <Description>
        Click any node to configure its settings here
      </Description>
      <PrevButton />
      <NextButton />
    </Spotlight>
  </Step>
  
  <Step number={4}>
    <Spotlight target=".toolbar">
      <Title>Toolbar</Title>
      <Description>
        Save, test, and activate your workflow from here
      </Description>
      <PrevButton />
      <FinishButton />
    </Spotlight>
  </Step>
</OnboardingTour>
```

### Interactive Tutorial
```jsx
<InteractiveTutorial>
  <TutorialStep>
    <Instruction>
      Let's create your first workflow! Drag the "Lead Created" trigger to the canvas.
    </Instruction>
    <Validation>
      {nodes.some(n => n.type === 'trigger') && <SuccessMessage />}
    </Validation>
  </TutorialStep>
  
  <TutorialStep>
    <Instruction>
      Great! Now add a "Send Email" action below the trigger.
    </Instruction>
    <Validation>
      {nodes.some(n => n.type === 'communication') && <SuccessMessage />}
    </Validation>
  </TutorialStep>
  
  <TutorialStep>
    <Instruction>
      Connect the trigger to the email action by dragging from the bottom circle.
    </Instruction>
    <Validation>
      {edges.length > 0 && <SuccessMessage />}
    </Validation>
  </TutorialStep>
  
  <TutorialStep>
    <Instruction>
      Perfect! Click the email node to configure it.
    </Instruction>
    <CompletionMessage>
      🎉 You've created your first workflow!
    </CompletionMessage>
  </TutorialStep>
</InteractiveTutorial>
```

---

## 🔄 Version Control & History

### Version History Panel
```jsx
<VersionHistory>
  <VersionList>
    <VersionItem current>
      <VersionNumber>v3</VersionNumber>
      <VersionDate>Dec 10, 2024 - 2:30 PM</VersionDate>
      <VersionAuthor>John Doe</VersionAuthor>
      <VersionChanges>
        <Change>Added email notification</Change>
        <Change>Updated condition logic</Change>
      </VersionChanges>
      <VersionActions>
        <ViewButton />
        <RestoreButton disabled />
      </VersionActions>
    </VersionItem>
    
    <VersionItem>
      <VersionNumber>v2</VersionNumber>
      <VersionDate>Dec 9, 2024 - 4:15 PM</VersionDate>
      <VersionAuthor>Jane Smith</VersionAuthor>
      <VersionChanges>
        <Change>Modified trigger conditions</Change>
      </VersionChanges>
      <VersionActions>
        <ViewButton />
        <RestoreButton />
      </VersionActions>
    </VersionItem>
    
    <VersionItem>
      <VersionNumber>v1</VersionNumber>
      <VersionDate>Dec 8, 2024 - 10:00 AM</VersionDate>
      <VersionAuthor>John Doe</VersionAuthor>
      <VersionChanges>
        <Change>Initial version</Change>
      </VersionChanges>
      <VersionActions>
        <ViewButton />
        <RestoreButton />
      </VersionActions>
    </VersionItem>
  </VersionList>
</VersionHistory>
```

### Compare Versions
```jsx
<VersionComparison>
  <ComparisonHeader>
    <VersionSelector>
      <Select value="v2">Version 2</Select>
    </VersionSelector>
    <CompareIcon>⇄</CompareIcon>
    <VersionSelector>
      <Select value="v3">Version 3 (Current)</Select>
    </VersionSelector>
  </ComparisonHeader>
  
  <ComparisonView>
    <SideBySide>
      <VersionCanvas version="v2">
        {/* Workflow v2 visualization */}
      </VersionCanvas>
      
      <VersionCanvas version="v3">
        {/* Workflow v3 visualization */}
      </VersionCanvas>
    </SideBySide>
    
    <ChangesList>
      <Change type="added">
        <Icon>+</Icon>
        <Description>Added "Send Email" node</Description>
      </Change>
      <Change type="modified">
        <Icon>~</Icon>
        <Description>Modified condition in "Check Score" node</Description>
      </Change>
      <Change type="removed">
        <Icon>-</Icon>
        <Description>Removed "Wait 1 Day" node</Description>
      </Change>
    </ChangesList>
  </ComparisonView>
</VersionComparison>
```

---

## 🌐 Internationalization (i18n)

### Multi-Language Support
```jsx
const translations = {
  en: {
    workflow: {
      builder: {
        title: 'Workflow Builder',
        save: 'Save',
        test: 'Test',
        activate: 'Activate',
        nodes: {
          trigger: 'Trigger',
          action: 'Action',
          condition: 'Condition',
          communication: 'Communication'
        }
      }
    }
  },
  es: {
    workflow: {
      builder: {
        title: 'Constructor de Flujo de Trabajo',
        save: 'Guardar',
        test: 'Probar',
        activate: 'Activar',
        nodes: {
          trigger: 'Disparador',
          action: 'Acción',
          condition: 'Condición',
          communication: 'Comunicación'
        }
      }
    }
  },
  fr: {
    workflow: {
      builder: {
        title: 'Constructeur de Flux de Travail',
        save: 'Enregistrer',
        test: 'Tester',
        activate: 'Activer',
        nodes: {
          trigger: 'Déclencheur',
          action: 'Action',
          condition: 'Condition',
          communication: 'Communication'
        }
      }
    }
  }
};
```

---

## 🎯 Accessibility (a11y)

### Keyboard Navigation
```jsx
const WorkflowBuilder = () => {
  useEffect(() => {
    const handleKeyboard = (e) => {
      // Delete selected node
      if (e.key === 'Delete' && selectedNode) {
        deleteNode(selectedNode.id);
      }
      
      // Duplicate node
      if (e.ctrlKey && e.key === 'd' && selectedNode) {
        e.preventDefault();
        duplicateNode(selectedNode.id);
      }
      
      // Save workflow
      if (e.ctrlKey && e.key === 's') {
        e.preventDefault();
        saveWorkflow();
      }
      
      // Undo
      if (e.ctrlKey && e.key === 'z') {
        e.preventDefault();
        undo();
      }
      
      // Redo
      if (e.ctrlKey && e.key === 'y') {
        e.preventDefault();
        redo();
      }
      
      // Select all
      if (e.ctrlKey && e.key === 'a') {
        e.preventDefault();
        selectAllNodes();
      }
    };
    
    window.addEventListener('keydown', handleKeyboard);
    return () => window.removeEventListener('keydown', handleKeyboard);
  }, [selectedNode]);
  
  return <WorkflowCanvas />;
};
```

### Screen Reader Support
```jsx
<Node
  role="button"
  aria-label={`${node.type} node: ${node.data.label}`}
  aria-describedby={`node-description-${node.id}`}
  tabIndex={0}
>
  <NodeContent />
  <div id={`node-description-${node.id}`} className="sr-only">
    {getNodeDescription(node)}
  </div>
</Node>
```

### Focus Management
```css
.workflow-node:focus {
  outline: 2px solid #3b82f6;
  outline-offset: 2px;
}

.workflow-node:focus:not(:focus-visible) {
  outline: none;
}

.workflow-node:focus-visible {
  outline: 2px solid #3b82f6;
  outline-offset: 2px;
}
```

---

## 📦 Export & Import

### Export Workflow
```jsx
<ExportDialog>
  <ExportOptions>
    <RadioGroup>
      <Radio value="json">JSON Format</Radio>
      <Radio value="yaml">YAML Format</Radio>
      <Radio value="image">Image (PNG)</Radio>
      <Radio value="pdf">PDF Document</Radio>
    </RadioGroup>
  </ExportOptions>
  
  <ExportSettings>
    <Checkbox>Include version history</Checkbox>
    <Checkbox>Include execution logs</Checkbox>
    <Checkbox>Include analytics data</Checkbox>
  </ExportSettings>
  
  <ExportActions>
    <CancelButton />
    <ExportButton />
  </ExportActions>
</ExportDialog>
```

### Import Workflow
```jsx
<ImportDialog>
  <FileUploader
    accept=".json,.yaml,.yml"
    onUpload={handleImport}
  >
    <UploadIcon />
    <UploadText>
      Drag and drop workflow file or click to browse
    </UploadText>
  </FileUploader>
  
  <ImportPreview>
    <PreviewTitle>Workflow Preview</PreviewTitle>
    <PreviewCanvas>
      {/* Show workflow structure */}
    </PreviewCanvas>
    <PreviewDetails>
      <Detail>
        <Label>Name:</Label>
        <Value>{importedWorkflow.name}</Value>
      </Detail>
      <Detail>
        <Label>Nodes:</Label>
        <Value>{importedWorkflow.nodes.length}</Value>
      </Detail>
      <Detail>
        <Label>Version:</Label>
        <Value>{importedWorkflow.version}</Value>
      </Detail>
    </PreviewDetails>
  </ImportPreview>
  
  <ImportActions>
    <CancelButton />
    <ImportButton />
  </ImportActions>
</ImportDialog>
```

---

## 🎉 Summary

This comprehensive workflow design system provides:

✅ **Modern UI/UX** - Clean, intuitive interface inspired by industry leaders
✅ **Rich Node Library** - 10+ node types covering all automation needs
✅ **Advanced Features** - Conditions, loops, error handling, approvals
✅ **Testing & Debugging** - Built-in test mode and execution logs
✅ **Analytics** - Performance metrics and monitoring
✅ **Mobile Support** - Responsive design for all devices
✅ **Accessibility** - Keyboard navigation and screen reader support
✅ **Internationalization** - Multi-language support
✅ **Version Control** - Track changes and restore previous versions
✅ **Templates** - Pre-built workflows for common use cases

This design will make your workflow builder competitive with Zapier, Make.com, and other industry leaders! 🚀

