package org.hibernate.envers.test.entities.reventity.trackmodifiedentities;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Revision entity which {@code modifiedEntityTypes} field is manually populated by {@link CustomTrackingRevisionListener}.
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@RevisionEntity(CustomTrackingRevisionListener.class)
public class CustomTrackingRevisionEntity {
    @Id
    @GeneratedValue
    @RevisionNumber
    private int customId;

    @RevisionTimestamp
    private long customTimestamp;

    @OneToMany(mappedBy="revision", cascade={CascadeType.PERSIST, CascadeType.REMOVE})
    private Set<ModifiedEntityTypeEntity> modifiedEntityTypes = new HashSet<ModifiedEntityTypeEntity>();

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

    public Set<ModifiedEntityTypeEntity> getModifiedEntityTypes() {
        return modifiedEntityTypes;
    }

    public void setModifiedEntityTypes(Set<ModifiedEntityTypeEntity> modifiedEntityTypes) {
        this.modifiedEntityTypes = modifiedEntityTypes;
    }

    public void addModifiedEntityType(String entityClassName) {
        modifiedEntityTypes.add(new ModifiedEntityTypeEntity(this, entityClassName));
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
