package org.hibernate.envers;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Extension of {@link DefaultRevisionEntity} that allows tracking entity types changed in each revision. This revision
 * entity is implicitly used when one of the following conditions is satisfied:
 * <ul>
 * <li><code>org.hibernate.envers.track_entities_changed_in_revision</code> parameter is set to <code>true</code>.</li>
 * <li>Custom revision entity (annotated with {@link RevisionEntity}) extends {@link DefaultTrackingModifiedTypesRevisionEntity}.</li>
 * <li>Custom revision entity (annotated with {@link RevisionEntity}) encapsulates a field marked with {@link ModifiedEntityNames}.</li>
 * </ul>
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@MappedSuperclass
public class DefaultTrackingModifiedTypesRevisionEntity extends DefaultRevisionEntity {
    @ElementCollection
    @JoinTable(name = "REVENTITY", joinColumns = @JoinColumn(name = "REV"))
    @Column(name = "ENTITYNAME")
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
        if (!(o instanceof DefaultTrackingModifiedTypesRevisionEntity)) return false;
        if (!super.equals(o)) return false;

        DefaultTrackingModifiedTypesRevisionEntity that = (DefaultTrackingModifiedTypesRevisionEntity) o;

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
        return "DefaultTrackingModifiedTypesRevisionEntity(" + super.toString() + ", modifiedEntityNames = " + modifiedEntityNames.toString() + ")";
    }
}
