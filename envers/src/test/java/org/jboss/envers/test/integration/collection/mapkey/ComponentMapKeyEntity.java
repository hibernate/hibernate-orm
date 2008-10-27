package org.jboss.envers.test.integration.collection.mapkey;

import org.jboss.envers.Versioned;
import org.jboss.envers.test.entities.components.Component1;
import org.jboss.envers.test.entities.components.ComponentTestEntity;

import javax.persistence.*;
import java.util.Map;
import java.util.HashMap;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class ComponentMapKeyEntity {
    @Id
    @GeneratedValue
    private Integer id;

    @Versioned
    @ManyToMany
    @MapKey(name = "comp1")
    private Map<Component1, ComponentTestEntity> idmap;

    public ComponentMapKeyEntity() {
        idmap = new HashMap<Component1, ComponentTestEntity>();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Map<Component1, ComponentTestEntity> getIdmap() {
        return idmap;
    }

    public void setIdmap(Map<Component1, ComponentTestEntity> idmap) {
        this.idmap = idmap;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComponentMapKeyEntity)) return false;

        ComponentMapKeyEntity that = (ComponentMapKeyEntity) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    public int hashCode() {
        return (id != null ? id.hashCode() : 0);
    }

    public String toString() {
        return "CMKE(id = " + id + ", idmap = " + idmap + ")";
    }
}