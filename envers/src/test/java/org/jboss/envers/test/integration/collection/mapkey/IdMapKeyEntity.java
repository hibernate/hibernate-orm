package org.jboss.envers.test.integration.collection.mapkey;

import org.jboss.envers.Versioned;
import org.jboss.envers.test.entities.StrTestEntity;

import javax.persistence.*;
import java.util.Map;
import java.util.HashMap;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class IdMapKeyEntity {
    @Id
    @GeneratedValue
    private Integer id;

    @Versioned
    @ManyToMany
    @MapKey
    private Map<Integer, StrTestEntity> idmap;

    public IdMapKeyEntity() {
        idmap = new HashMap<Integer, StrTestEntity>();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Map<Integer, StrTestEntity> getIdmap() {
        return idmap;
    }

    public void setIdmap(Map<Integer, StrTestEntity> idmap) {
        this.idmap = idmap;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdMapKeyEntity)) return false;

        IdMapKeyEntity that = (IdMapKeyEntity) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    public int hashCode() {
        return (id != null ? id.hashCode() : 0);
    }

    public String toString() {
        return "IMKE(id = " + id + ", idmap = " + idmap + ")";
    }
}