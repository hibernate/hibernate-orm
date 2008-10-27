package org.jboss.envers.test.entities.manytomany.unidirectional;

import org.jboss.envers.Versioned;
import org.jboss.envers.test.entities.StrTestEntity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import java.util.Map;

/**
 * Entity with a map from a string to an entity
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class MapUniEntity {
    @Id
    private Integer id;

    @Versioned
    private String data;

    @Versioned
    @ManyToMany
    private Map<String, StrTestEntity> map;

    public MapUniEntity() {
    }

    public MapUniEntity(Integer id, String data) {
        this.id = id;
        this.data = data;
    }

    public MapUniEntity(String data) {
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

    public Map<String, StrTestEntity> getMap() {
        return map;
    }

    public void setMap(Map<String, StrTestEntity> map) {
        this.map = map;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MapUniEntity)) return false;

        MapUniEntity that = (MapUniEntity) o;

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
        return "MapUniEntity(id = " + id + ", data = " + data + ")";
    }
}