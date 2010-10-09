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
package org.hibernate.envers.test.integration.inheritance.single.childrelation;

import javax.persistence.Basic;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@DiscriminatorValue("2")
@Audited
public class ChildIngEntity extends ParentNotIngEntity {
    @Basic
    private Long number;

    @ManyToOne
    private ReferencedEntity referenced;

    public ChildIngEntity() {
    }

    public ChildIngEntity(String data, Long number) {
        super(data);
        this.number = number;
    }

    public ChildIngEntity(Integer id, String data, Long number) {
        super(id, data);
        this.number = number;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public ReferencedEntity getReferenced() {
        return referenced;
    }

    public void setReferenced(ReferencedEntity referenced) {
        this.referenced = referenced;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChildIngEntity)) return false;
        if (!super.equals(o)) return false;

        ChildIngEntity childEntity = (ChildIngEntity) o;

        if (number != null ? !number.equals(childEntity.number) : childEntity.number != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (number != null ? number.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "ChildIngEntity(id = " + getId() + ", data = " + getData() + ", number = " + number + ")";
    }
}