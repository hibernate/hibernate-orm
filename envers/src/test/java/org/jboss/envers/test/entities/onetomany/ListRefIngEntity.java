package org.jboss.envers.test.entities.onetomany;

import org.jboss.envers.Versioned;

import javax.persistence.*;

/**
 * ReferencIng entity
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class ListRefIngEntity {
    @Id
    private Integer id;

    @Versioned
    private String data;

    @Versioned
    @ManyToOne
    private ListRefEdEntity reference;

    public ListRefIngEntity() { }

    public ListRefIngEntity(Integer id, String data, ListRefEdEntity reference) {
        this.id = id;
        this.data = data;
        this.reference = reference;
    }

    public ListRefIngEntity(String data, ListRefEdEntity reference) {
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

    public ListRefEdEntity getReference() {
        return reference;
    }

    public void setReference(ListRefEdEntity reference) {
        this.reference = reference;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ListRefIngEntity)) return false;

        ListRefIngEntity that = (ListRefIngEntity) o;

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

    public String toString() {
        return "ListRefIngEntity(id = " + id + ", data = " + data + ")";
    }
}