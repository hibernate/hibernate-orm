package org.hibernate.envers;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Extension of {@link DefaultRevisionEntity} that allows tracking entity types changed in each revision. This revision
 * entity is implicitly used when <code>org.hibernate.envers.track_entities_changed_in_revision</code> parameter
 * is set to <code>true</code>.
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@MappedSuperclass
public class DefaultTrackingModifiedTypesRevisionEntity extends DefaultRevisionEntity {
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "REVENTITY", joinColumns = @JoinColumn(name = "REV"))
    @Column(name = "ENTITYTYPE")
    @Fetch(FetchMode.JOIN)
    @ModifiedEntityTypes
    private Set<String> modifiedEntityTypes = new HashSet<String>();

    public Set<String> getModifiedEntityTypes() {
        return modifiedEntityTypes;
    }

    public void setModifiedEntityTypes(Set<String> modifiedEntityTypes) {
        this.modifiedEntityTypes = modifiedEntityTypes;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DefaultTrackingModifiedTypesRevisionEntity)) return false;
        if (!super.equals(o)) return false;

        DefaultTrackingModifiedTypesRevisionEntity that = (DefaultTrackingModifiedTypesRevisionEntity) o;

        if (modifiedEntityTypes != null ? !modifiedEntityTypes.equals(that.modifiedEntityTypes)
                                        : that.modifiedEntityTypes != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (modifiedEntityTypes != null ? modifiedEntityTypes.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "DefaultTrackingModifiedTypesRevisionEntity(" + super.toString() + ", modifiedEntityTypes = " + modifiedEntityTypes + ")";
    }
}
