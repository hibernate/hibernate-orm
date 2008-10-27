package org.jboss.envers.test.entities.onetomany;

import org.jboss.envers.Versioned;

import javax.persistence.*;

/**
 * ReferencIng entity
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class SetRefIngEntity {
    @Id
    private Integer id;

    @Versioned
    private String data;

    @Versioned
    @ManyToOne
    private SetRefEdEntity reference;

    public SetRefIngEntity() { }

    public SetRefIngEntity(Integer id, String data, SetRefEdEntity reference) {
        this.id = id;
        this.data = data;
        this.reference = reference;
    }

    public SetRefIngEntity(String data, SetRefEdEntity reference) {
        this.data = data;
        this.reference = reference;
    }

    public SetRefIngEntity(Integer id, String data) {
        this.id = id;
        this.data = data;
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

    public SetRefEdEntity getReference() {
        return reference;
    }

    public void setReference(SetRefEdEntity reference) {
        this.reference = reference;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SetRefIngEntity)) return false;

        SetRefIngEntity that = (SetRefIngEntity) o;

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
        return "SetRefIngEntity(id = " + id + ", data = " + data + ")";
    }
}
