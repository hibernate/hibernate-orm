/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.notinsertable.manytoone;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

@Entity
@Audited
public class NotInsertableEntityType {
	public NotInsertableEntityType(Integer typeId, String type) {
		this.typeId = typeId;
		this.type = type;
	}

	public NotInsertableEntityType() {
	}

	@Id
	private Integer typeId;

	@Basic
	private String type;

	public Integer getTypeId() {
		return typeId;
	}

	public void setTypeId(Integer typeId) {
		this.typeId = typeId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		NotInsertableEntityType that = (NotInsertableEntityType) o;

		if ( type != null ? !type.equals( that.type ) : that.type != null ) {
			return false;
		}
		if ( typeId != null ? !typeId.equals( that.typeId ) : that.typeId != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = typeId != null ? typeId.hashCode() : 0;
		result = 31 * result + (type != null ? type.hashCode() : 0);
		return result;
	}
}
