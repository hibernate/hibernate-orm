package org.hibernate.envers.test.entities.reventity.trackmodifiedentities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * Custom detail of revision entity.
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
public class ModifiedEntityNameEntity {
    @Id
    @GeneratedValue
    private Integer id;

    @ManyToOne
    private CustomTrackingRevisionEntity revision;
    
    private String entityName;

    public ModifiedEntityNameEntity() {
    }

    public ModifiedEntityNameEntity(String entityName) {
        this.entityName = entityName;
    }

    public ModifiedEntityNameEntity(CustomTrackingRevisionEntity revision, String entityName) {
        this.revision = revision;
        this.entityName = entityName;
    }

    public CustomTrackingRevisionEntity getRevision() {
        return revision;
    }

    public void setRevision(CustomTrackingRevisionEntity revision) {
        this.revision = revision;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModifiedEntityNameEntity)) return false;

        ModifiedEntityNameEntity that = (ModifiedEntityNameEntity) o;

        if (entityName != null ? !entityName.equals(that.entityName) : that.entityName != null) return false;

        return true;
    }

    public int hashCode() {
        return entityName != null ? entityName.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "CustomTrackingRevisionEntity(entityName = " + entityName + ")";
    }
}
