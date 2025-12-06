# 🎯 Lead Assignment Strategies - Complete Guide

## Overview

This document describes all 9 lead assignment strategies supported by the system, inspired by Salesforce, HubSpot, Zoho, Pipedrive, and other leading CRMs.

---

## 📊 Assignment Strategies Comparison

| Strategy | Best For | Fairness | Complexity | Used By |
|----------|----------|----------|------------|---------|
| Round Robin | Equal distribution | ⭐⭐⭐⭐⭐ | ⭐ | Salesforce, HubSpot, Pipedrive |
| Workload-Based | Balanced workload | ⭐⭐⭐⭐ | ⭐⭐ | Salesforce, Zoho |
| Territory | Geographic sales | ⭐⭐⭐ | ⭐⭐⭐ | Salesforce, Dynamics 365 |
| Skill-Based | Specialized sales | ⭐⭐⭐ | ⭐⭐⭐ | Salesforce Einstein, HubSpot |
| Lead Source | Channel-specific | ⭐⭐⭐⭐ | ⭐⭐ | HubSpot, Pipedrive |
| Lead Value | High-value focus | ⭐⭐ | ⭐⭐ | Salesforce, Zoho |
| Availability | Real-time response | ⭐⭐⭐ | ⭐⭐ | Freshsales, Pipedrive |
| Performance | Reward top performers | ⭐⭐ | ⭐⭐⭐ | Salesforce Einstein |
| Custom Rules | Complex scenarios | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | Salesforce Flow, Zoho |

---

## 1. 🔄 Round Robin Assignment

### Description
Distributes leads evenly among team members in a rotating sequence.

### How It Works
```
Lead 1 → Rep A
Lead 2 → Rep B
Lead 3 → Rep C
Lead 4 → Rep A (cycle repeats)
```

### Configuration
```json
{
  "strategy": "ROUND_ROBIN",
  "config": {
    "userIds": [101, 102, 103, 104],
    "teamKey": "sales_team_1"
  }
}
```

### Pros
- ✅ Perfectly fair distribution
- ✅ Simple to understand
- ✅ No configuration needed
- ✅ Works for any team size

### Cons
- ❌ Doesn't consider workload
- ❌ Doesn't consider skills
- ❌ Doesn't consider availability

### Best For
- Teams with similar skill levels
- Equal opportunity distribution
- Simple sales processes

---

## 2. ⚖️ Workload-Based Assignment

### Description
Assigns leads to the rep with the least current workload.

### How It Works
```
Rep A: 5 active leads
Rep B: 3 active leads  ← New lead assigned here
Rep C: 7 active leads
```

### Configuration
```json
{
  "strategy": "WORKLOAD_BASED",
  "config": {
    "userIds": [101, 102, 103],
    "maxWorkload": 20
  }
}
```

### Workload Calculation
- Active leads count
- Open opportunities count
- Pending tasks count
- Custom weight per activity type

### Pros
- ✅ Balanced workload
- ✅ Prevents burnout
- ✅ Maximizes team capacity
- ✅ Fair over time

### Cons
- ❌ Requires workload tracking
- ❌ More complex
- ❌ May not consider lead quality

### Best For
- Teams with varying workloads
- High-volume lead flow
- Preventing rep overload

---

## 3. 🗺️ Territory-Based Assignment

### Description
Assigns leads based on geographic location.

### How It Works
```
Lead from California → West Coast Rep
Lead from New York → East Coast Rep
Lead from Texas → Central Rep
```

### Configuration
```json
{
  "strategy": "TERRITORY",
  "config": {
    "territoryMapping": {
      "zip_94105": 101,
      "city_San Francisco": 101,
      "state_California": 102,
      "state_New York": 103,
      "country_USA": 104,
      "default": 105
    }
  }
}
```

### Matching Priority
1. Zip Code (most specific)
2. City
3. State/Province
4. Country
5. Default

### Pros
- ✅ Local market knowledge
- ✅ Time zone alignment
- ✅ Better customer relationships
- ✅ Reduced travel costs

### Cons
- ❌ Uneven lead distribution
- ❌ Complex setup
- ❌ Requires territory definition

### Best For
- Geographic sales teams
- Field sales
- Regional expertise needed

---

## 4. 🎓 Skill-Based Assignment

### Description
Matches leads to reps based on skills and expertise.

### How It Works
```
SaaS Lead → SaaS Specialist
Healthcare Lead → Healthcare Expert
Enterprise Lead → Enterprise Rep
```

### Configuration
```json
{
  "strategy": "SKILL_BASED",
  "config": {
    "skillMapping": {
      "product_SaaS": [101, 102],
      "product_Hardware": [103],
      "industry_Healthcare": [104, 105],
      "industry_Finance": [106],
      "general": [107, 108, 109]
    }
  }
}
```

### Matching Criteria
- Product interest
- Industry vertical
- Company size
- Technical complexity
- Language preference

### Pros
- ✅ Higher conversion rates
- ✅ Better customer experience
- ✅ Faster sales cycles
- ✅ Specialized expertise

### Cons
- ❌ Uneven distribution
- ❌ Requires skill tracking
- ❌ May create bottlenecks

### Best For
- Complex products
- Multiple industries
- Specialized sales teams

---

## 5. 📱 Lead Source Assignment

### Description
Routes leads based on where they came from.

### How It Works
```
Website Form → Web Team
Social Media → Social Team
Referral → Account Manager
Trade Show → Event Team
```

### Configuration
```json
{
  "strategy": "LEAD_SOURCE",
  "config": {
    "sourceMapping": {
      "Website": 101,
      "Facebook": 102,
      "LinkedIn": 102,
      "Referral": 103,
      "Trade Show": 104,
      "web_team": 105,
      "social_team": 106,
      "default": 107
    }
  }
}
```

### Source Categories
- **Web Sources**: Website, Landing Page, Organic Search
- **Social Sources**: Facebook, LinkedIn, Twitter, Instagram
- **Paid Sources**: Google Ads, Facebook Ads
- **Referral Sources**: Customer Referral, Partner
- **Event Sources**: Trade Show, Webinar, Conference

### Pros
- ✅ Channel expertise
- ✅ Better follow-up
- ✅ Source-specific messaging
- ✅ Easy to implement

### Cons
- ❌ Uneven distribution
- ❌ Requires source tracking
- ❌ May miss opportunities

### Best For
- Multi-channel marketing
- Specialized channel teams
- Source-specific processes

---

## 6. 💰 Lead Value Assignment

### Description
Assigns high-value leads to senior reps, lower-value to junior reps.

### How It Works
```
$50K+ Lead → Senior Rep
$10K-$50K Lead → Mid-Level Rep
<$10K Lead → Junior Rep
```

### Configuration
```json
{
  "strategy": "LEAD_VALUE",
  "config": {
    "seniorReps": [101, 102],
    "midLevelReps": [103, 104, 105],
    "juniorReps": [106, 107, 108],
    "highValueThreshold": 50000,
    "midValueThreshold": 10000
  }
}
```

### Value Calculation
- Estimated deal size
- Company revenue
- Number of employees
- Budget indicated
- Historical data

### Pros
- ✅ Maximize high-value conversions
- ✅ Develop junior reps
- ✅ Efficient resource allocation
- ✅ Better ROI

### Cons
- ❌ May demotivate junior reps
- ❌ Requires value estimation
- ❌ Can be inaccurate

### Best For
- High-value sales
- Tiered sales teams
- Enterprise sales

---

## 7. 🟢 Availability-Based Assignment

### Description
Assigns leads to currently available/online reps.

### How It Works
```
New Lead Arrives
    ↓
Check Online Reps
    ├─ Rep A: Online → Assign
    ├─ Rep B: Offline
    └─ Rep C: In Meeting
```

### Configuration
```json
{
  "strategy": "AVAILABILITY",
  "config": {
    "userIds": [101, 102, 103, 104],
    "onlineUsers": [101, 103],
    "checkAvailability": true,
    "fallbackToOffline": true
  }
}
```

### Availability States
- **Online**: Available now
- **Busy**: In meeting/call
- **Away**: Temporarily unavailable
- **Offline**: Not working

### Pros
- ✅ Immediate response
- ✅ Better lead engagement
- ✅ Higher conversion
- ✅ Real-time routing

### Cons
- ❌ Requires presence tracking
- ❌ Uneven distribution
- ❌ Complex implementation

### Best For
- Inbound sales
- Live chat leads
- Time-sensitive leads

---

## 8. 🏆 Performance-Based Assignment

### Description
Assigns more leads to high-performing reps.

### How It Works
```
Rep A: 80% conversion → 40% of leads
Rep B: 60% conversion → 30% of leads
Rep C: 40% conversion → 20% of leads
Rep D: 20% conversion → 10% of leads
```

### Configuration
```json
{
  "strategy": "PERFORMANCE",
  "config": {
    "userPerformance": {
      "101": 0.80,
      "102": 0.60,
      "103": 0.40,
      "104": 0.20
    },
    "performanceMetric": "conversion_rate",
    "evaluationPeriod": "last_30_days"
  }
}
```

### Performance Metrics
- Conversion rate
- Revenue generated
- Average deal size
- Sales cycle length
- Customer satisfaction

### Pros
- ✅ Rewards top performers
- ✅ Maximizes conversions
- ✅ Motivates team
- ✅ Better ROI

### Cons
- ❌ May demotivate low performers
- ❌ Unfair to new reps
- ❌ Requires performance tracking
- ❌ Can create competition

### Best For
- Competitive teams
- High-performing teams
- Commission-based sales

---

## 9. ⚙️ Custom Rules Assignment

### Description
Complex multi-criteria assignment with custom rules.

### How It Works
```
IF lead.industry = "Healthcare" 
   AND lead.value > $50K 
   AND lead.country = "USA"
THEN assign to Healthcare Enterprise Rep

ELSE IF lead.source = "Website" 
   AND lead.score > 75
THEN assign to Web Team Lead

ELSE assign to Default Rep
```

### Configuration
```json
{
  "strategy": "CUSTOM_RULES",
  "config": {
    "rules": [
      {
        "name": "High-Value Healthcare",
        "conditions": [
          {"field": "industry", "operator": "equals", "value": "Healthcare"},
          {"field": "estimatedValue", "operator": "greater_than", "value": 50000},
          {"field": "country", "operator": "equals", "value": "USA"}
        ],
        "assignTo": 101
      },
      {
        "name": "Hot Web Leads",
        "conditions": [
          {"field": "source", "operator": "equals", "value": "Website"},
          {"field": "score", "operator": "greater_than", "value": 75}
        ],
        "assignTo": 102
      }
    ],
    "defaultUser": 103
  }
}
```

### Rule Operators
- `equals`, `not_equals`
- `contains`, `not_contains`
- `greater_than`, `less_than`
- `greater_than_or_equal`, `less_than_or_equal`
- `in`, `not_in`
- `is_null`, `is_not_null`

### Pros
- ✅ Maximum flexibility
- ✅ Complex scenarios
- ✅ Business-specific logic
- ✅ Highly customizable

### Cons
- ❌ Complex to configure
- ❌ Hard to maintain
- ❌ Requires testing
- ❌ Can be slow

### Best For
- Complex sales processes
- Multiple criteria
- Unique business rules

---

## 🎯 Recommended Combinations

### Scenario 1: Small Team (2-5 reps)
```
Primary: Round Robin
Fallback: Workload-Based
```

### Scenario 2: Geographic Sales
```
Primary: Territory
Secondary: Workload-Based
Fallback: Round Robin
```

### Scenario 3: Specialized Products
```
Primary: Skill-Based
Secondary: Lead Value
Fallback: Round Robin
```

### Scenario 4: High-Volume Inbound
```
Primary: Availability
Secondary: Workload-Based
Fallback: Round Robin
```

### Scenario 5: Enterprise Sales
```
Primary: Lead Value
Secondary: Skill-Based
Tertiary: Territory
Fallback: Custom Rules
```

---

## 📊 Implementation Example

```java
// In your workflow
LeadAssignmentService assignmentService;

Map<String, Object> lead = new HashMap<>();
lead.put("source", "Website");
lead.put("industry", "Healthcare");
lead.put("estimatedValue", 75000);
lead.put("country", "USA");

Map<String, Object> config = new HashMap<>();
config.put("userIds", Arrays.asList(101L, 102L, 103L));

Long assignedUserId = assignmentService.assignLead(
    lead, 
    "ROUND_ROBIN", 
    config
);
```

---

## ✅ Best Practices

1. **Start Simple** - Begin with Round Robin
2. **Monitor Performance** - Track assignment effectiveness
3. **Iterate** - Adjust based on results
4. **Combine Strategies** - Use fallbacks
5. **Test Thoroughly** - Validate before going live
6. **Document Rules** - Keep configuration clear
7. **Review Regularly** - Update as team changes

---

**Status:** ✅ All 9 Strategies Implemented and Ready to Use!
