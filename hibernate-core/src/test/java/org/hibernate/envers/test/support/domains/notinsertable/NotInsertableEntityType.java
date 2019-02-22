/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.notinsertable;

import java.util.Objects;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Audited
public class NotInsertableEntityType {
	public NotInsertableEntityType() {
	}

	public NotInsertableEntityType(Integer typeId, String type) {
		this.typeId = typeId;
		this.type = type;
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
		return Objects.equals( typeId, that.typeId ) &&
				Objects.equals( type, that.type );
	}

	@Override
	public int hashCode() {
		return Objects.hash( typeId, type );
	}
}
