package org.jboss.envers.test.integration.onetoone.bidirectional;

import org.jboss.envers.Versioned;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class BiRefIngEntity {
    @Id
    private Integer id;

    @Versioned
    private String data;

    @Versioned
    @OneToOne
    private BiRefEdEntity reference;

    public BiRefIngEntity() {
    }

    public BiRefIngEntity(Integer id, String data) {
        this.id = id;
        this.data = data;
    }

    public BiRefIngEntity(Integer id, String data, BiRefEdEntity reference) {
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

    public BiRefEdEntity getReference() {
        return reference;
    }

    public void setReference(BiRefEdEntity reference) {
        this.reference = reference;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BiRefIngEntity)) return false;

        BiRefIngEntity that = (BiRefIngEntity) o;

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
