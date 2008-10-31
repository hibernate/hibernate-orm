/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.test.integration.collection.mapkey;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.MapKey;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.entities.components.Component1;
import org.hibernate.envers.test.entities.components.ComponentTestEntity;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class ComponentMapKeyEntity {
    @Id
    @GeneratedValue
    private Integer id;

    @Audited
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