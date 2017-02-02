/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.envers.test.integration.inheritance.joined.relation;

import javax.persistence.Basic;
import javax.persistence.Entity;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Audited
public class ChildIngEntity extends ParentIngEntity {
	@Basic
	private Long numVal;

	public ChildIngEntity() {
	}

	public ChildIngEntity(Integer id, String data, Long numVal) {
		super( id, data );
		this.numVal = numVal;
	}

	public Long getNumVal() {
		return numVal;
	}

	public void setNumVal(Long numVal) {
		this.numVal = numVal;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ChildIngEntity) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		ChildIngEntity childEntity = (ChildIngEntity) o;

		if ( numVal != null ? !numVal.equals( childEntity.numVal ) : childEntity.numVal != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (numVal != null ? numVal.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "ChildIngEntity(id = " + getId() + ", data = " + getData() + ", numVal = " + numVal + ")";
	}
}