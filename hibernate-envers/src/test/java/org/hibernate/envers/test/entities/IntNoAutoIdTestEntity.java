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
package org.hibernate.envers.test.entities;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class IntNoAutoIdTestEntity {
    @Id
    private Integer id;

    @Audited
    private Integer number;

    public IntNoAutoIdTestEntity() {
    }

    public IntNoAutoIdTestEntity(Integer number, Integer id) {
        this.id = id;
        this.number = number;
    }

    public IntNoAutoIdTestEntity(Integer number) {
        this.number = number;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IntNoAutoIdTestEntity)) return false;

        IntNoAutoIdTestEntity that = (IntNoAutoIdTestEntity) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        //noinspection RedundantIfStatement
        if (number != null ? !number.equals(that.number) : that.number != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (id != null ? id.hashCode() : 0);
        result = 31 * result + (number != null ? number.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "INATE(id = " + id + ", number = " + number + ")";
    }
}