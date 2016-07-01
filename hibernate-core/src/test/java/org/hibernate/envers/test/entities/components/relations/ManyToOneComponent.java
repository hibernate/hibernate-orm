/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.components.relations;

import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.envers.test.entities.StrTestEntity;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Embeddable
@Table(name = "ManyToOneCompEmb")
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
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		ManyToOneComponent that = (ManyToOneComponent) o;

		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
			return false;
		}
		if ( entity != null ? !entity.equals( that.entity ) : that.entity != null ) {
			return false;
		}

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