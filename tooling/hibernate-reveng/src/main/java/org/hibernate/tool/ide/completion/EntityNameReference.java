package org.hibernate.tool.ide.completion;

/**
 * Class that represents an alias to some entityname in a HQL statement. e.g. "Product as p" or "Product p"
 * 
 * Should not be used by external clients.
 * 
 * @author leon, Max Rydahl Andersen
 */
public class EntityNameReference {

    private String alias;

    private String entityName;

    public EntityNameReference(String type, String alias) {
        this.entityName = type;
        this.alias = alias;
    }

    /** 
     * 
     * @return The alias, the "p" in "Product as p" 
     */
    public String getAlias() {
        return alias;
    }

    /**
     * 
     * @return the entityname, the "Product" in "Product as b"
     */
    public String getEntityName() {
        return entityName;
    }

    public String toString() {
        return alias + ":" + entityName;
    }



}
