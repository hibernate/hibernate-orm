package org.jboss.envers.test.entities.manytomany;

import org.jboss.envers.Versioned;

import javax.persistence.*;
import java.util.Map;
import java.util.HashMap;

/**
 * Entity owning the many-to-many relation
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class MapOwningEntity {
    @Id
    private Integer id;

    @Versioned
    private String data;

    @Versioned
    @ManyToMany
    private Map<String, MapOwnedEntity> references = new HashMap<String, MapOwnedEntity>();

    public MapOwningEntity() { }

    public MapOwningEntity(Integer id, String data) {
        this.id = id;
        this.data = data;
    }

    public MapOwningEntity(String data) {
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

    public Map<String, MapOwnedEntity> getReferences() {
        return references;
    }

    public void setReferences(Map<String, MapOwnedEntity> references) {
        this.references = references;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MapOwningEntity)) return false;

        MapOwningEntity that = (MapOwningEntity) o;

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
        return "MapOwningEntity(id = " + id + ", data = " + data + ")";
    }
}