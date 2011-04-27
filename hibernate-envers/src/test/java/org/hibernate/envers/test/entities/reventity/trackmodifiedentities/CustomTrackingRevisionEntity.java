package org.hibernate.envers.test.entities.reventity.trackmodifiedentities;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@RevisionEntity(CustomEntityTrackingRevisionListener.class)
public class CustomTrackingRevisionEntity {
    @Id
    @GeneratedValue
    @RevisionNumber
    private int customId;

    @RevisionTimestamp
    private long customTimestamp;

    @OneToMany(mappedBy="revision", cascade={CascadeType.PERSIST, CascadeType.REMOVE})
    private Set<ModifiedEntityNameEntity> modifiedEntityNames = new HashSet<ModifiedEntityNameEntity>();

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

    public Set<ModifiedEntityNameEntity> getModifiedEntityNames() {
        return modifiedEntityNames;
    }

    public void setModifiedEntityNames(Set<ModifiedEntityNameEntity> modifiedEntityNames) {
        this.modifiedEntityNames = modifiedEntityNames;
    }

    public void addModifiedEntityName(String entityName) {
        modifiedEntityNames.add(new ModifiedEntityNameEntity(this, entityName));
    }

    public void removeModifiedEntityName(String entityName) {
        modifiedEntityNames.remove(new ModifiedEntityNameEntity(this, entityName));
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomTrackingRevisionEntity)) return false;

        CustomTrackingRevisionEntity that = (CustomTrackingRevisionEntity) o;

        if (customId != that.customId) return false;
        if (customTimestamp != that.customTimestamp) return false;

        return true;
    }

    public int hashCode() {
        int result = customId;
        result = 31 * result + (int) (customTimestamp ^ (customTimestamp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "CustomTrackingRevisionEntity(customId = " + customId + ", customTimestamp = " + customTimestamp + ")";
    }
}
