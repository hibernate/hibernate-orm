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
package org.hibernate.envers.test.integration.data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class SerializableTestEntity {
    @Id
    @GeneratedValue
    private Integer id;

    @Audited
    private SerObject obj;

    public SerializableTestEntity() {
    }

    public SerializableTestEntity(SerObject obj) {
        this.obj = obj;
    }

    public SerializableTestEntity(Integer id, SerObject obj) {
        this.obj = obj;
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public SerObject getObj() {
        return obj;
    }

    public void setObj(SerObject obj) {
        this.obj = obj;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SerializableTestEntity)) return false;

        SerializableTestEntity that = (SerializableTestEntity) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (obj != null ? !obj.equals(that.obj) : that.obj != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (id != null ? id.hashCode() : 0);
        result = 31 * result + (obj != null ? obj.hashCode() : 0);
        return result;
    }
}