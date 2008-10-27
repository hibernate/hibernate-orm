package org.jboss.envers.test.integration.superclass;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class SubclassEntity extends SuperclassOfEntity {
    @Id
    @GeneratedValue
    private Integer id;

    public SubclassEntity() {
    }

    public SubclassEntity(Integer id, String str) {
        super(str);
        this.id = id;
    }

    public SubclassEntity(String str) {
        super(str);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubclassEntity)) return false;
        if (!super.equals(o)) return false;

        SubclassEntity that = (SubclassEntity) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }
}
