package org.jboss.envers.test.integration.onetoone.unidirectional;

import org.jboss.envers.Versioned;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

/**
 * Unidirectional ReferencIng Entity
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class UniRefIngEntity {
    @Id
    private Integer id;

    @Versioned
    private String data;

    @Versioned
    @OneToOne
    private UniRefEdEntity reference;

    public UniRefIngEntity() {
    }

    public UniRefIngEntity(Integer id, String data, UniRefEdEntity reference) {
        this.id = id;
        this.data = data;
        this.reference = reference;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public UniRefEdEntity getReference() {
        return reference;
    }

    public void setReference(UniRefEdEntity reference) {
        this.reference = reference;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UniRefIngEntity)) return false;

        UniRefIngEntity that = (UniRefIngEntity) o;

        if (data != null ? !data.equals(that.data) : that.data != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (id != null ? id.hashCode() : 0);
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }
}
