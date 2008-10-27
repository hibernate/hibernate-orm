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
public class CustomBoxedRevEntity {
    @Id
    @GeneratedValue
    @RevisionNumber
    private Integer customId;

    @RevisionTimestamp
    private Long customTimestamp;

    public Integer getCustomId() {
        return customId;
    }

    public void setCustomId(Integer customId) {
        this.customId = customId;
    }

    public Long getCustomTimestamp() {
        return customTimestamp;
    }

    public void setCustomTimestamp(Long customTimestamp) {
        this.customTimestamp = customTimestamp;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomBoxedRevEntity)) return false;

        CustomBoxedRevEntity that = (CustomBoxedRevEntity) o;

        if (customId != null ? !customId.equals(that.customId) : that.customId != null) return false;
        if (customTimestamp != null ? !customTimestamp.equals(that.customTimestamp) : that.customTimestamp != null)
            return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (customId != null ? customId.hashCode() : 0);
        result = 31 * result + (customTimestamp != null ? customTimestamp.hashCode() : 0);
        return result;
    }
}