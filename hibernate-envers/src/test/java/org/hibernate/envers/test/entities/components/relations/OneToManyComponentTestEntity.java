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
package org.hibernate.envers.test.entities.components.relations;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class OneToManyComponentTestEntity {
    @Id
    @GeneratedValue
    private Integer id;

    @Embedded
    @Audited
    private OneToManyComponent comp1;

    public OneToManyComponentTestEntity() {
    }

    public OneToManyComponentTestEntity(Integer id, OneToManyComponent comp1) {
        this.id = id;
        this.comp1 = comp1;
    }

    public OneToManyComponentTestEntity(OneToManyComponent comp1) {
        this.comp1 = comp1;
    }

	public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public OneToManyComponent getComp1() {
        return comp1;
    }

    public void setComp1(OneToManyComponent comp1) {
        this.comp1 = comp1;
    }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		OneToManyComponentTestEntity that = (OneToManyComponentTestEntity) o;

		if (comp1 != null ? !comp1.equals(that.comp1) : that.comp1 != null) return false;
		if (id != null ? !id.equals(that.id) : that.id != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (comp1 != null ? comp1.hashCode() : 0);
		return result;
	}

	public String toString() {
        return "OTMCTE(id = " + id + ", comp1 = " + comp1 + ")";
    }
}