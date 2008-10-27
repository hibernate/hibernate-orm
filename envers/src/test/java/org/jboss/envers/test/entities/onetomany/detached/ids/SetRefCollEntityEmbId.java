package org.jboss.envers.test.entities.onetomany.detached.ids;

import org.jboss.envers.Versioned;
import org.jboss.envers.test.entities.ids.EmbId;
import org.jboss.envers.test.entities.ids.EmbIdTestEntity;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.EmbeddedId;
import java.util.Set;

/**
 * Set collection of references entity
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class SetRefCollEntityEmbId {
    @EmbeddedId
    private EmbId id;

    @Versioned
    private String data;

    @Versioned
    @OneToMany
    private Set<EmbIdTestEntity> collection;

    public SetRefCollEntityEmbId() {
    }

    public SetRefCollEntityEmbId(EmbId id, String data) {
        this.id = id;
        this.data = data;
    }

    public SetRefCollEntityEmbId(String data) {
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

    public Set<EmbIdTestEntity> getCollection() {
        return collection;
    }

    public void setCollection(Set<EmbIdTestEntity> collection) {
        this.collection = collection;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SetRefCollEntityEmbId)) return false;

        SetRefCollEntityEmbId that = (SetRefCollEntityEmbId) o;

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
        return "SetRefCollEntityEmbId(id = " + id + ", data = " + data + ")";
    }
}