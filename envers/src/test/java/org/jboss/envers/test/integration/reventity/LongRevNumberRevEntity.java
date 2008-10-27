package org.jboss.envers.test.integration.reventity;

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
public class LongRevNumberRevEntity {
    @Id
    @GeneratedValue
    @RevisionNumber
    private long customId;

    @RevisionTimestamp
    private long customTimestamp;

    public long getCustomId() {
        return customId;
    }

    public void setCustomId(long customId) {
        this.customId = customId;
    }

    public long getCustomTimestamp() {
        return customTimestamp;
    }

    public void setCustomTimestamp(long customTimestamp) {
        this.customTimestamp = customTimestamp;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LongRevNumberRevEntity)) return false;

        LongRevNumberRevEntity that = (LongRevNumberRevEntity) o;

        if (customId != that.customId) return false;
        if (customTimestamp != that.customTimestamp) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (int) (customId ^ (customId >>> 32));
        result = 31 * result + (int) (customTimestamp ^ (customTimestamp >>> 32));
        return result;
    }
}