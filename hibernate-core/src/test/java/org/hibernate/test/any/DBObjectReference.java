package org.hibernate.test.any;

/**
 * Entity with any to java class mapped as two entity 
 * 
 * @author Nikolay Shestakov
 */
public class DBObjectReference {
    private Long     id;
    private DBObject ref;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DBObject getRef() {
        return ref;
    }

    public void setRef(DBObject ref) {
        this.ref = ref;
    }
}
