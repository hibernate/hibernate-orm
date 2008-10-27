package org.jboss.envers.test.integration.onetomany;

import org.jboss.envers.Versioned;

import javax.persistence.*;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class RefIngMapKeyEntity {
    @Id
    @GeneratedValue
    private Integer id;

    @Versioned
    @ManyToOne
    private RefEdMapKeyEntity reference;

    @Versioned
    private String data;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public RefEdMapKeyEntity getReference() {
        return reference;
    }

    public void setReference(RefEdMapKeyEntity reference) {
        this.reference = reference;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RefIngMapKeyEntity)) return false;

        RefIngMapKeyEntity that = (RefIngMapKeyEntity) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    public int hashCode() {
        return (id != null ? id.hashCode() : 0);
    }

    public String toString() {
        return "RingMKE(id = " + id + ", data = " + data + ", reference = " + reference + ")";
    }
}