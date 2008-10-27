package org.jboss.envers.test.integration.manytomany.ternary;

import org.jboss.envers.Versioned;
import org.jboss.envers.test.entities.IntTestEntity;
import org.jboss.envers.test.entities.StrTestEntity;
import org.hibernate.annotations.MapKeyManyToMany;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.ManyToMany;
import java.util.Map;
import java.util.HashMap;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class TernaryMapEntity {
    @Id
    @GeneratedValue
    private Integer id;

    @Versioned
    @ManyToMany
    @MapKeyManyToMany
    private Map<IntTestEntity, StrTestEntity> map;

    public TernaryMapEntity() {
        map = new HashMap<IntTestEntity, StrTestEntity>();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Map<IntTestEntity, StrTestEntity> getMap() {
        return map;
    }

    public void setMap(Map<IntTestEntity, StrTestEntity> map) {
        this.map = map;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TernaryMapEntity)) return false;

        TernaryMapEntity that = (TernaryMapEntity) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    public int hashCode() {
        return (id != null ? id.hashCode() : 0);
    }

    public String toString() {
        return "TME(id = " + id + ", map = " + map + ")";
    }
}