package com.zen.workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.*;
import java.util.*;

/**
 * Service for dynamic entity operations in workflows
 * Handles CRUD operations on any entity type dynamically
 */
@Slf4j
@Service
public class DynamicEntityService {

    @Autowired
    private EntityManager entityManager;

    /**
     * Query records dynamically
     */
    public List<Object> queryRecords(String entityName, Map<String, Object> criteria, Integer limit) {
        try {
            Class<?> entityClass = getEntityClass(entityName);
            
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Object> query = cb.createQuery(Object.class);
            Root<?> root = query.from(entityClass);
            
            // Build predicates from criteria
            List<Predicate> predicates = new ArrayList<>();
            if (criteria != null && !criteria.isEmpty()) {
                for (Map.Entry<String, Object> entry : criteria.entrySet()) {
                    predicates.add(cb.equal(root.get(entry.getKey()), entry.getValue()));
                }
            }
            
            if (!predicates.isEmpty()) {
                query.where(predicates.toArray(new Predicate[0]));
            }
            
            Query typedQuery = entityManager.createQuery(query);
            if (limit != null && limit > 0) {
                typedQuery.setMaxResults(limit);
            }
            
            return typedQuery.getResultList();
            
        } catch (Exception e) {
            log.error("Query failed for entity: {}", entityName, e);
            throw new RuntimeException("Query failed: " + e.getMessage(), e);
        }
    }

    /**
     * Create a new record
     */
    public Object createRecord(String entityName, Map<String, Object> fields) {
        try {
            Class<?> entityClass = getEntityClass(entityName);
            Object entity = entityClass.getDeclaredConstructor().newInstance();
            
            // Set fields
            setFields(entity, fields);
            
            entityManager.persist(entity);
            entityManager.flush();
            
            log.info("Created {} record with ID: {}", entityName, getEntityId(entity));
            return entity;
            
        } catch (Exception e) {
            log.error("Create failed for entity: {}", entityName, e);
            throw new RuntimeException("Create failed: " + e.getMessage(), e);
        }
    }

    /**
     * Update an existing record
     */
    public Object updateRecord(String entityName, Long id, Map<String, Object> fields) {
        try {
            Class<?> entityClass = getEntityClass(entityName);
            Object entity = entityManager.find(entityClass, id);
            
            if (entity == null) {
                throw new RuntimeException("Record not found: " + entityName + " with ID " + id);
            }
            
            // Update fields
            setFields(entity, fields);
            
            entityManager.merge(entity);
            entityManager.flush();
            
            log.info("Updated {} record with ID: {}", entityName, id);
            return entity;
            
        } catch (Exception e) {
            log.error("Update failed for entity: {} with ID: {}", entityName, id, e);
            throw new RuntimeException("Update failed: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a record
     */
    public void deleteRecord(String entityName, Long id) {
        try {
            Class<?> entityClass = getEntityClass(entityName);
            Object entity = entityManager.find(entityClass, id);
            
            if (entity == null) {
                throw new RuntimeException("Record not found: " + entityName + " with ID " + id);
            }
            
            entityManager.remove(entity);
            entityManager.flush();
            
            log.info("Deleted {} record with ID: {}", entityName, id);
            
        } catch (Exception e) {
            log.error("Delete failed for entity: {} with ID: {}", entityName, id, e);
            throw new RuntimeException("Delete failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get a single record by ID
     */
    public Object getRecord(String entityName, Long id) {
        try {
            Class<?> entityClass = getEntityClass(entityName);
            Object entity = entityManager.find(entityClass, id);
            
            if (entity == null) {
                throw new RuntimeException("Record not found: " + entityName + " with ID " + id);
            }
            
            return entity;
            
        } catch (Exception e) {
            log.error("Get failed for entity: {} with ID: {}", entityName, id, e);
            throw new RuntimeException("Get failed: " + e.getMessage(), e);
        }
    }

    /**
     * Search records with flexible criteria
     */
    public List<Object> searchRecords(String entityName, String searchField, String searchValue, Integer limit) {
        try {
            Class<?> entityClass = getEntityClass(entityName);
            
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Object> query = cb.createQuery(Object.class);
            Root<?> root = query.from(entityClass);
            
            // Build LIKE predicate
            Predicate predicate = cb.like(
                cb.lower(root.get(searchField).as(String.class)),
                "%" + searchValue.toLowerCase() + "%"
            );
            
            query.where(predicate);
            
            Query typedQuery = entityManager.createQuery(query);
            if (limit != null && limit > 0) {
                typedQuery.setMaxResults(limit);
            }
            
            return typedQuery.getResultList();
            
        } catch (Exception e) {
            log.error("Search failed for entity: {}", entityName, e);
            throw new RuntimeException("Search failed: " + e.getMessage(), e);
        }
    }

    /**
     * Clone a record
     */
    public Object cloneRecord(String entityName, Long id, Map<String, Object> overrideFields) {
        try {
            Class<?> entityClass = getEntityClass(entityName);
            Object original = entityManager.find(entityClass, id);
            
            if (original == null) {
                throw new RuntimeException("Record not found: " + entityName + " with ID " + id);
            }
            
            // Create new instance
            Object clone = entityClass.getDeclaredConstructor().newInstance();
            
            // Copy all fields from original
            copyFields(original, clone);
            
            // Override specific fields
            if (overrideFields != null && !overrideFields.isEmpty()) {
                setFields(clone, overrideFields);
            }
            
            // Clear ID to create new record
            setField(clone, "id", null);
            
            entityManager.persist(clone);
            entityManager.flush();
            
            log.info("Cloned {} record from ID: {} to new ID: {}", entityName, id, getEntityId(clone));
            return clone;
            
        } catch (Exception e) {
            log.error("Clone failed for entity: {} with ID: {}", entityName, id, e);
            throw new RuntimeException("Clone failed: " + e.getMessage(), e);
        }
    }

    /**
     * Bulk create records
     */
    public List<Object> createMultiple(String entityName, List<Map<String, Object>> recordsList) {
        List<Object> created = new ArrayList<>();
        
        try {
            for (Map<String, Object> fields : recordsList) {
                Object entity = createRecord(entityName, fields);
                created.add(entity);
            }
            
            log.info("Created {} {} records", created.size(), entityName);
            return created;
            
        } catch (Exception e) {
            log.error("Bulk create failed for entity: {}", entityName, e);
            throw new RuntimeException("Bulk create failed: " + e.getMessage(), e);
        }
    }

    /**
     * Bulk update records
     */
    public List<Object> updateMultiple(String entityName, List<Long> ids, Map<String, Object> fields) {
        List<Object> updated = new ArrayList<>();
        
        try {
            for (Long id : ids) {
                Object entity = updateRecord(entityName, id, fields);
                updated.add(entity);
            }
            
            log.info("Updated {} {} records", updated.size(), entityName);
            return updated;
            
        } catch (Exception e) {
            log.error("Bulk update failed for entity: {}", entityName, e);
            throw new RuntimeException("Bulk update failed: " + e.getMessage(), e);
        }
    }

    /**
     * Bulk delete records
     */
    public void deleteMultiple(String entityName, List<Long> ids) {
        try {
            for (Long id : ids) {
                deleteRecord(entityName, id);
            }
            
            log.info("Deleted {} {} records", ids.size(), entityName);
            
        } catch (Exception e) {
            log.error("Bulk delete failed for entity: {}", entityName, e);
            throw new RuntimeException("Bulk delete failed: " + e.getMessage(), e);
        }
    }

    // Helper methods

    private Class<?> getEntityClass(String entityName) throws ClassNotFoundException {
        // Map entity names to actual classes
        Map<String, String> entityMap = new HashMap<>();
        entityMap.put("Lead", "com.zen.crm.entity.Lead");
        entityMap.put("Contact", "com.zen.crm.entity.Contact");
        entityMap.put("Account", "com.zen.crm.entity.Account");
        entityMap.put("Deal", "com.zen.crm.entity.Deal");
        entityMap.put("Task", "com.zen.crm.entity.Task");
        entityMap.put("Activity", "com.zen.crm.entity.Activity");
        // Add more mappings as needed
        
        String className = entityMap.get(entityName);
        if (className == null) {
            throw new ClassNotFoundException("Unknown entity: " + entityName);
        }
        
        return Class.forName(className);
    }

    private void setFields(Object entity, Map<String, Object> fields) throws Exception {
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            setField(entity, entry.getKey(), entry.getValue());
        }
    }

    private void setField(Object entity, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = findField(entity.getClass(), fieldName);
        if (field != null) {
            field.setAccessible(true);
            field.set(entity, value);
        }
    }

    private Object getField(Object entity, String fieldName) throws Exception {
        java.lang.reflect.Field field = findField(entity.getClass(), fieldName);
        if (field != null) {
            field.setAccessible(true);
            return field.get(entity);
        }
        return null;
    }

    private java.lang.reflect.Field findField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return findField(clazz.getSuperclass(), fieldName);
            }
            return null;
        }
    }

    private void copyFields(Object source, Object target) throws Exception {
        Class<?> clazz = source.getClass();
        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            Object value = field.get(source);
            field.set(target, value);
        }
    }

    private Object getEntityId(Object entity) throws Exception {
        return getField(entity, "id");
    }
}
