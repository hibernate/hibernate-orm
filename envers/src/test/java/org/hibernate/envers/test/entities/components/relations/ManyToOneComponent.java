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

import org.hibernate.envers.test.entities.StrTestEntity;

import javax.persistence.ManyToOne;
import javax.persistence.Embeddable;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Embeddable
public class ManyToOneComponent {
	@ManyToOne
    private StrTestEntity entity;

    private String data;

	public ManyToOneComponent(StrTestEntity entity, String data) {
        this.entity = entity;
        this.data = data;
    }

    public ManyToOneComponent() {
    }

	public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

	public StrTestEntity getEntity() {
		return entity;
	}

	public void setEntity(StrTestEntity entity) {
		this.entity = entity;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ManyToOneComponent that = (ManyToOneComponent) o;

		if (data != null ? !data.equals(that.data) : that.data != null) return false;
		if (entity != null ? !entity.equals(that.entity) : that.entity != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = entity != null ? entity.hashCode() : 0;
		result = 31 * result + (data != null ? data.hashCode() : 0);
		return result;
	}

	public String toString() {
        return "ManyToOneComponent(str1 = " + data + ")";
    }
}