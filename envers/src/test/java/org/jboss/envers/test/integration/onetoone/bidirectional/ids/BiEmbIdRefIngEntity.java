package org.jboss.envers.test.integration.onetoone.bidirectional.ids;

import org.jboss.envers.Versioned;
import org.jboss.envers.test.entities.ids.EmbId;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.EmbeddedId;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class BiEmbIdRefIngEntity {
    @EmbeddedId
    private EmbId id;

    @Versioned
    private String data;

    @Versioned
    @OneToOne
    private BiEmbIdRefEdEntity reference;

    public BiEmbIdRefIngEntity() {
    }

    public BiEmbIdRefIngEntity(EmbId id, String data) {
        this.id = id;
        this.data = data;
    }

    public BiEmbIdRefIngEntity(EmbId id, String data, BiEmbIdRefEdEntity reference) {
        this.id = id;
        this.data = data;
        this.reference = reference;
    }

    public EmbId getId() {
        return id;
    }

    public void setId(EmbId id) {
        this.id = id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public BiEmbIdRefEdEntity getReference() {
        return reference;
    }

    public void setReference(BiEmbIdRefEdEntity reference) {
        this.reference = reference;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BiEmbIdRefIngEntity)) return false;

        BiEmbIdRefIngEntity that = (BiEmbIdRefIngEntity) o;

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