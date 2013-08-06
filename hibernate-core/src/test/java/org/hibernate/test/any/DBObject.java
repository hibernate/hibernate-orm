package org.hibernate.test.any;

/**
 * Java-class mapped as two entity
 * 
 * @author Nikolay Shestakov
 */
public class DBObject {
    private Long   id;
    private String entityName;

    public DBObject() {
    }

    public DBObject(String entityName) {
        this.entityName = entityName;
    }

    public String getEntityName() {
        return entityName;
    }

    public Long getId() {
        return id;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
