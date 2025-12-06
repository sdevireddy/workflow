package com.zen.workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lead Assignment Service
 * Supports multiple assignment strategies inspired by Salesforce, HubSpot, Zoho
 */
@Slf4j
@Service
public class LeadAssignmentService {

    // Track last assigned user for round-robin
    private final Map<String, Integer> roundRobinCounters = new ConcurrentHashMap<>();
    
    // Track user workload
    private final Map<Long, Integer> userWorkload = new ConcurrentHashMap<>();

    /**
     * Assign lead based on strategy
     */
    public Long assignLead(Map<String, Object> lead, String strategy, Map<String, Object> config) {
        log.info("Assigning lead using strategy: {}", strategy);
        
        switch (strategy.toUpperCase()) {
            case "ROUND_ROBIN":
                return assignRoundRobin(lead, config);
            
            case "WORKLOAD_BASED":
                return assignByWorkload(lead, config);
            
            case "TERRITORY":
                return assignByTerritory(lead, config);
            
            case "SKILL_BASED":
                return assignBySkill(lead, config);
            
            case "LEAD_SOURCE":
                return assignByLeadSource(lead, config);
            
            case "LEAD_VALUE":
                return assignByLeadValue(lead, config);
            
            case "AVAILABILITY":
                return assignByAvailability(lead, config);
            
            case "PERFORMANCE":
                return assignByPerformance(lead, config);
            
            case "CUSTOM_RULES":
                return assignByCustomRules(lead, config);
            
            default:
                log.warn("Unknown strategy: {}, using round-robin", strategy);
                return assignRoundRobin(lead, config);
        }
    }

    /**
     * 1. ROUND ROBIN ASSIGNMENT
     * Distributes leads evenly among team members
     * Used by: Salesforce, HubSpot, Pipedrive
     */
    private Long assignRoundRobin(Map<String, Object> lead, Map<String, Object> config) {
        List<Long> userIds = (List<Long>) config.get("userIds");
        String teamKey = (String) config.getOrDefault("teamKey", "default");
        
        if (userIds == null || userIds.isEmpty()) {
            log.error("No users available for round-robin assignment");
            return null;
        }
        
        // Get current counter for this team
        int counter = roundRobinCounters.getOrDefault(teamKey, 0);
        
        // Get next user
        Long assignedUserId = userIds.get(counter % userIds.size());
        
        // Increment counter
        roundRobinCounters.put(teamKey, counter + 1);
        
        log.info("Round-robin assigned lead to user: {}", assignedUserId);
        return assignedUserId;
    }

    /**
     * 2. WORKLOAD-BASED ASSIGNMENT
     * Assigns to user with least current workload
     * Used by: Salesforce (Load Balancing), Zoho
     */
    private Long assignByWorkload(Map<String, Object> lead, Map<String, Object> config) {
        List<Long> userIds = (List<Long>) config.get("userIds");
        
        if (userIds == null || userIds.isEmpty()) {
            return null;
        }
        
        // Find user with minimum workload
        Long assignedUserId = userIds.stream()
            .min(Comparator.comparingInt(userId -> userWorkload.getOrDefault(userId, 0)))
            .orElse(userIds.get(0));
        
        // Increment workload
        userWorkload.merge(assignedUserId, 1, Integer::sum);
        
        log.info("Workload-based assigned lead to user: {} (current load: {})", 
            assignedUserId, userWorkload.get(assignedUserId));
        return assignedUserId;
    }

    /**
     * 3. TERRITORY-BASED ASSIGNMENT
     * Assigns based on geographic territory
     * Used by: Salesforce Territory Management, Dynamics 365
     */
    private Long assignByTerritory(Map<String, Object> lead, Map<String, Object> config) {
        String leadCountry = (String) lead.get("country");
        String leadState = (String) lead.get("state");
        String leadCity = (String) lead.get("city");
        String leadZipCode = (String) lead.get("zipCode");
        
        Map<String, Long> territoryMapping = (Map<String, Long>) config.get("territoryMapping");
        
        if (territoryMapping == null) {
            log.warn("No territory mapping configured");
            return null;
        }
        
        // Try to match by zip code (most specific)
        if (leadZipCode != null && territoryMapping.containsKey("zip_" + leadZipCode)) {
            return territoryMapping.get("zip_" + leadZipCode);
        }
        
        // Try to match by city
        if (leadCity != null && territoryMapping.containsKey("city_" + leadCity)) {
            return territoryMapping.get("city_" + leadCity);
        }
        
        // Try to match by state
        if (leadState != null && territoryMapping.containsKey("state_" + leadState)) {
            return territoryMapping.get("state_" + leadState);
        }
        
        // Try to match by country
        if (leadCountry != null && territoryMapping.containsKey("country_" + leadCountry)) {
            return territoryMapping.get("country_" + leadCountry);
        }
        
        // Default territory
        return territoryMapping.get("default");
    }

    /**
     * 4. SKILL-BASED ASSIGNMENT
     * Assigns based on rep skills and lead requirements
     * Used by: Salesforce Einstein, HubSpot
     */
    private Long assignBySkill(Map<String, Object> lead, Map<String, Object> config) {
        String leadIndustry = (String) lead.get("industry");
        String leadProduct = (String) lead.get("productInterest");
        
        Map<String, List<Long>> skillMapping = (Map<String, List<Long>>) config.get("skillMapping");
        
        if (skillMapping == null) {
            return null;
        }
        
        // Try to match by product interest
        if (leadProduct != null && skillMapping.containsKey("product_" + leadProduct)) {
            List<Long> specialists = skillMapping.get("product_" + leadProduct);
            return getNextAvailableUser(specialists);
        }
        
        // Try to match by industry
        if (leadIndustry != null && skillMapping.containsKey("industry_" + leadIndustry)) {
            List<Long> specialists = skillMapping.get("industry_" + leadIndustry);
            return getNextAvailableUser(specialists);
        }
        
        // Default to general pool
        List<Long> generalPool = skillMapping.get("general");
        return getNextAvailableUser(generalPool);
    }

    /**
     * 5. LEAD SOURCE ASSIGNMENT
     * Assigns based on where lead came from
     * Used by: HubSpot, Pipedrive
     */
    private Long assignByLeadSource(Map<String, Object> lead, Map<String, Object> config) {
        String leadSource = (String) lead.get("source");
        
        Map<String, Long> sourceMapping = (Map<String, Long>) config.get("sourceMapping");
        
        if (sourceMapping == null || leadSource == null) {
            return null;
        }
        
        // Direct mapping
        if (sourceMapping.containsKey(leadSource)) {
            return sourceMapping.get(leadSource);
        }
        
        // Category mapping (e.g., all social media to one rep)
        if (isWebSource(leadSource) && sourceMapping.containsKey("web_team")) {
            return sourceMapping.get("web_team");
        }
        
        if (isSocialSource(leadSource) && sourceMapping.containsKey("social_team")) {
            return sourceMapping.get("social_team");
        }
        
        // Default
        return sourceMapping.get("default");
    }

    /**
     * 6. LEAD VALUE ASSIGNMENT
     * Assigns high-value leads to senior reps
     * Used by: Salesforce, Zoho
     */
    private Long assignByLeadValue(Map<String, Object> lead, Map<String, Object> config) {
        Double estimatedValue = getLeadValue(lead);
        
        List<Long> seniorReps = (List<Long>) config.get("seniorReps");
        List<Long> juniorReps = (List<Long>) config.get("juniorReps");
        Double highValueThreshold = (Double) config.getOrDefault("highValueThreshold", 10000.0);
        
        if (estimatedValue >= highValueThreshold && seniorReps != null && !seniorReps.isEmpty()) {
            log.info("High-value lead (${}) assigned to senior rep", estimatedValue);
            return getNextAvailableUser(seniorReps);
        } else if (juniorReps != null && !juniorReps.isEmpty()) {
            return getNextAvailableUser(juniorReps);
        }
        
        return null;
    }

    /**
     * 7. AVAILABILITY-BASED ASSIGNMENT
     * Assigns to currently available/online reps
     * Used by: Freshsales, Pipedrive
     */
    private Long assignByAvailability(Map<String, Object> lead, Map<String, Object> config) {
        List<Long> allUsers = (List<Long>) config.get("userIds");
        List<Long> onlineUsers = (List<Long>) config.get("onlineUsers");
        
        // Prefer online users
        if (onlineUsers != null && !onlineUsers.isEmpty()) {
            return getNextAvailableUser(onlineUsers);
        }
        
        // Fallback to all users
        return getNextAvailableUser(allUsers);
    }

    /**
     * 8. PERFORMANCE-BASED ASSIGNMENT
     * Assigns more leads to high-performing reps
     * Used by: Salesforce Einstein, HubSpot
     */
    private Long assignByPerformance(Map<String, Object> lead, Map<String, Object> config) {
        Map<Long, Double> userPerformance = (Map<Long, Double>) config.get("userPerformance");
        
        if (userPerformance == null || userPerformance.isEmpty()) {
            return null;
        }
        
        // Weighted random selection based on performance
        double totalPerformance = userPerformance.values().stream()
            .mapToDouble(Double::doubleValue)
            .sum();
        
        double random = Math.random() * totalPerformance;
        double cumulative = 0.0;
        
        for (Map.Entry<Long, Double> entry : userPerformance.entrySet()) {
            cumulative += entry.getValue();
            if (random <= cumulative) {
                log.info("Performance-based assigned to user: {} (performance: {})", 
                    entry.getKey(), entry.getValue());
                return entry.getKey();
            }
        }
        
        return userPerformance.keySet().iterator().next();
    }

    /**
     * 9. CUSTOM RULES ASSIGNMENT
     * Complex multi-criteria assignment
     * Used by: Salesforce Flow, Zoho Blueprint
     */
    private Long assignByCustomRules(Map<String, Object> lead, Map<String, Object> config) {
        List<Map<String, Object>> rules = (List<Map<String, Object>>) config.get("rules");
        
        if (rules == null) {
            return null;
        }
        
        // Evaluate rules in order
        for (Map<String, Object> rule : rules) {
            if (evaluateRule(lead, rule)) {
                Long assignedUser = ((Number) rule.get("assignTo")).longValue();
                log.info("Custom rule matched, assigned to user: {}", assignedUser);
                return assignedUser;
            }
        }
        
        // No rule matched, use default
        return ((Number) config.get("defaultUser")).longValue();
    }

    /**
     * Helper: Evaluate custom rule
     */
    private boolean evaluateRule(Map<String, Object> lead, Map<String, Object> rule) {
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) rule.get("conditions");
        
        if (conditions == null) {
            return false;
        }
        
        // All conditions must match (AND logic)
        for (Map<String, Object> condition : conditions) {
            String field = (String) condition.get("field");
            String operator = (String) condition.get("operator");
            Object value = condition.get("value");
            
            Object leadValue = lead.get(field);
            
            if (!evaluateCondition(leadValue, operator, value)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Helper: Evaluate single condition
     */
    private boolean evaluateCondition(Object actual, String operator, Object expected) {
        if (actual == null) {
            return "is_null".equals(operator);
        }
        
        switch (operator) {
            case "equals":
                return actual.equals(expected);
            case "not_equals":
                return !actual.equals(expected);
            case "contains":
                return actual.toString().contains(expected.toString());
            case "greater_than":
                return Double.parseDouble(actual.toString()) > Double.parseDouble(expected.toString());
            case "less_than":
                return Double.parseDouble(actual.toString()) < Double.parseDouble(expected.toString());
            default:
                return false;
        }
    }

    /**
     * Helper: Get next available user from list
     */
    private Long getNextAvailableUser(List<Long> users) {
        if (users == null || users.isEmpty()) {
            return null;
        }
        
        // Simple round-robin within the list
        int index = new Random().nextInt(users.size());
        return users.get(index);
    }

    /**
     * Helper: Calculate lead value
     */
    private Double getLeadValue(Map<String, Object> lead) {
        Object value = lead.get("estimatedValue");
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        
        // Estimate based on company size
        Object companySize = lead.get("companySize");
        if (companySize instanceof Number) {
            int size = ((Number) companySize).intValue();
            return size * 100.0; // $100 per employee estimate
        }
        
        return 0.0;
    }

    /**
     * Helper: Check if source is web-based
     */
    private boolean isWebSource(String source) {
        return source != null && (
            source.equalsIgnoreCase("Website") ||
            source.equalsIgnoreCase("Web Form") ||
            source.equalsIgnoreCase("Landing Page") ||
            source.equalsIgnoreCase("Organic Search")
        );
    }

    /**
     * Helper: Check if source is social media
     */
    private boolean isSocialSource(String source) {
        return source != null && (
            source.contains("Facebook") ||
            source.contains("LinkedIn") ||
            source.contains("Twitter") ||
            source.contains("Instagram") ||
            source.equalsIgnoreCase("Social Media")
        );
    }

    /**
     * Decrease workload when lead is closed/converted
     */
    public void decreaseWorkload(Long userId) {
        userWorkload.computeIfPresent(userId, (k, v) -> Math.max(0, v - 1));
    }

    /**
     * Reset round-robin counter
     */
    public void resetRoundRobin(String teamKey) {
        roundRobinCounters.put(teamKey, 0);
    }

    /**
     * Get current workload for user
     */
    public int getUserWorkload(Long userId) {
        return userWorkload.getOrDefault(userId, 0);
    }
}
