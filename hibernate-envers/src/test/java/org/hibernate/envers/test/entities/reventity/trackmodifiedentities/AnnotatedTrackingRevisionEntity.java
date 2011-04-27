package org.hibernate.envers.test.entities.reventity.trackmodifiedentities;

import org.hibernate.envers.ModifiedEntityNames;
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
@RevisionEntity
public class AnnotatedTrackingRevisionEntity {
    @Id
    @GeneratedValue
    @RevisionNumber
    private int customId;

    @RevisionTimestamp
    private long customTimestamp;

    @ElementCollection
    @JoinTable(name = "REVENTITY", joinColumns = @JoinColumn(name = "REV"))
    @Column(name = "ENTITYNAME")
    @ModifiedEntityNames
    private Set<String> modifiedEntityNames;

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

    public Set<String> getModifiedEntityNames() {
        return modifiedEntityNames;
    }

    public void setModifiedEntityNames(Set<String> modifiedEntityNames) {
        this.modifiedEntityNames = modifiedEntityNames;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnnotatedTrackingRevisionEntity)) return false;

        AnnotatedTrackingRevisionEntity that = (AnnotatedTrackingRevisionEntity) o;

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
        return "AnnotatedTrackingRevisionEntity(customId = " + customId + ", customTimestamp = " + customTimestamp + ")";
    }
}
