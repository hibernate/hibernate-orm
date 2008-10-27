package org.jboss.envers.test.entities.onetomany.ids;

import org.jboss.envers.Versioned;
import org.jboss.envers.test.entities.ids.EmbId;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.EmbeddedId;
import java.util.Set;

/**
 * ReferencEd entity
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class SetRefEdEmbIdEntity {
    @EmbeddedId
    private EmbId id;

    @Versioned
    private String data;

    @Versioned
    @OneToMany(mappedBy="reference")
    private Set<SetRefIngEmbIdEntity> reffering;

    public SetRefEdEmbIdEntity() {
    }

    public SetRefEdEmbIdEntity(EmbId id, String data) {
        this.id = id;
        this.data = data;
    }

    public SetRefEdEmbIdEntity(String data) {
        this.data = data;
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

    public Set<SetRefIngEmbIdEntity> getReffering() {
        return reffering;
    }

    public void setReffering(Set<SetRefIngEmbIdEntity> reffering) {
        this.reffering = reffering;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SetRefEdEmbIdEntity)) return false;

        SetRefEdEmbIdEntity that = (SetRefEdEmbIdEntity) o;

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
        return "SetRefEdEmbIdEntity(id = " + id + ", data = " + data + ")";
    }
}