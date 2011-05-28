package org.hibernate.envers.test.entities.reventity.trackmodifiedentities;

import org.hibernate.envers.ModifiedEntityTypes;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import javax.persistence.*;
import java.util.Set;

/**
 * Sample revision entity that uses {@link ModifiedEntityTypes} annotation.
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@RevisionEntity
public class AnnotatedTrackingRevisionEntity {
    @Id
    @GeneratedValue
    @RevisionNumber
    private int customId;

    @RevisionTimestamp
    private long customTimestamp;

    @ElementCollection
    @JoinTable(name = "REVCHANGES", joinColumns = @JoinColumn(name = "REV"))
    @Column(name = "ENTITYTYPE")
    @ModifiedEntityTypes
    private Set<String> entityTypes;

    public int getCustomId() {
        return customId;
    }

    public void setCustomId(int customId) {
        this.customId = customId;
    }

    public long getCustomTimestamp() {
        return customTimestamp;
    }

    public void setCustomTimestamp(long customTimestamp) {
        this.customTimestamp = customTimestamp;
    }

    public Set<String> getEntityTypes() {
        return entityTypes;
    }

    public void setEntityTypes(Set<String> entityTypes) {
        this.entityTypes = entityTypes;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnnotatedTrackingRevisionEntity)) return false;

        AnnotatedTrackingRevisionEntity that = (AnnotatedTrackingRevisionEntity) o;

        if (customId != that.customId) return false;
        if (customTimestamp != that.customTimestamp) return false;
        if (entityTypes != null ? !entityTypes.equals(that.entityTypes) : that.entityTypes != null) return false;

        return true;
    }

    public int hashCode() {
        int result = customId;
        result = 31 * result + (int) (customTimestamp ^ (customTimestamp >>> 32));
        result = 31 * result + (entityTypes != null ? entityTypes.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AnnotatedTrackingRevisionEntity(customId = " + customId + ", customTimestamp = " + customTimestamp + ", entityTypes=" + entityTypes + ")";
    }
}
