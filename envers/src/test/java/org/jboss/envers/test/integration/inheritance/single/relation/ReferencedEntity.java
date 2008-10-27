package org.jboss.envers.test.integration.inheritance.single.relation;

import org.jboss.envers.Versioned;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import java.util.Set;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Versioned
public class ReferencedEntity {
    @Id
    @GeneratedValue
    private Integer id;

    @OneToMany(mappedBy = "referenced")
    private Set<ParentIngEntity> referencing;

    public Set<ParentIngEntity> getReferencing() {
        return referencing;
    }

    public void setReferencing(Set<ParentIngEntity> referencing) {
        this.referencing = referencing;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReferencedEntity)) return false;

        ReferencedEntity that = (ReferencedEntity) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    public int hashCode() {
        return id;
    }
}
