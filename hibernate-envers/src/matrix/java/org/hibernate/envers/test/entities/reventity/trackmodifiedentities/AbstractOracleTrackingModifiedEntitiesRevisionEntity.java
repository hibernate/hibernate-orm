package org.hibernate.envers.test.entities.reventity.trackmodifiedentities;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.ModifiedEntityNames;
import org.hibernate.envers.test.entities.reventity.AbstractOracleRevisionEntity;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@MappedSuperclass
public abstract class AbstractOracleTrackingModifiedEntitiesRevisionEntity extends AbstractOracleRevisionEntity {
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "REVCHANGES", joinColumns = @JoinColumn(name = "REV"))
    @Column(name = "ENTITYNAME")
    @Fetch(FetchMode.JOIN)
    @ModifiedEntityNames
    private Set<String> modifiedEntityNames = new HashSet<String>();

    public Set<String> getModifiedEntityNames() {
        return modifiedEntityNames;
    }

    public void setModifiedEntityNames(Set<String> modifiedEntityNames) {
        this.modifiedEntityNames = modifiedEntityNames;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractOracleTrackingModifiedEntitiesRevisionEntity)) return false;
        if (!super.equals(o)) return false;

        AbstractOracleTrackingModifiedEntitiesRevisionEntity that = (AbstractOracleTrackingModifiedEntitiesRevisionEntity) o;

        if (modifiedEntityNames != null ? !modifiedEntityNames.equals(that.modifiedEntityNames)
                : that.modifiedEntityNames != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (modifiedEntityNames != null ? modifiedEntityNames.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "AbstractOracleTrackingModifiedEntitiesRevisionEntity(" + super.toString() + ", modifiedEntityNames = " + modifiedEntityNames + ")";
    }
}
