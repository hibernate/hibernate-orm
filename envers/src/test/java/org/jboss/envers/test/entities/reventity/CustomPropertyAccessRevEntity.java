package org.jboss.envers.test.entities.reventity;

import org.jboss.envers.RevisionNumber;
import org.jboss.envers.RevisionTimestamp;
import org.jboss.envers.RevisionEntity;

import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Entity;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@RevisionEntity
public class CustomPropertyAccessRevEntity {
    private int customId;

    private long customTimestamp;

    @Id
    @GeneratedValue
    @RevisionNumber
    public int getCustomId() {
        return customId;
    }

    public void setCustomId(int customId) {
        this.customId = customId;
    }

    @RevisionTimestamp
    public long getCustomTimestamp() {
        return customTimestamp;
    }

    public void setCustomTimestamp(long customTimestamp) {
        this.customTimestamp = customTimestamp;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomPropertyAccessRevEntity)) return false;

        CustomPropertyAccessRevEntity that = (CustomPropertyAccessRevEntity) o;

        if (customId != that.customId) return false;
        if (customTimestamp != that.customTimestamp) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = customId;
        result = 31 * result + (int) (customTimestamp ^ (customTimestamp >>> 32));
        return result;
    }
}