/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.envers.test.support.domains.inheritance.joined;

import java.util.Objects;

import javax.persistence.Basic;
import javax.persistence.Entity;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Audited
public class ChildEntity extends ParentEntity {
	@Basic
	private Long numVal;

	public ChildEntity() {
	}

	public ChildEntity(Integer id, String data, Long numVal) {
		super( id, data );
		this.numVal = numVal;
	}

	public Long getNumVal() {
		return numVal;
	}

	public void setNumVal(Long numVal) {
		this.numVal = numVal;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}
		ChildEntity that = (ChildEntity) o;
		return Objects.equals( numVal, that.numVal );
	}

	@Override
	public int hashCode() {
		return Objects.hash( super.hashCode(), numVal );
	}

	@Override
	public String toString() {
		return "ChildEntity(id = " + getId() + ", data = " + getData() + ", numVal = " + numVal + ")";
	}
}