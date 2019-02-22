/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.basic;

import java.util.Objects;

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
	private Integer numVal;

	public IntNoAutoIdTestEntity() {
	}

	public IntNoAutoIdTestEntity(Integer numVal, Integer id) {
		this.id = id;
		this.numVal = numVal;
	}

	public IntNoAutoIdTestEntity(Integer numVal) {
		this.numVal = numVal;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getNumVal() {
		return numVal;
	}

	public void setNumVal(Integer numVal) {
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
		IntNoAutoIdTestEntity that = (IntNoAutoIdTestEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( numVal, that.numVal );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, numVal );
	}

	@Override
	public String toString() {
		return "IntNoAutoIdTestEntity{" +
				"id=" + id +
				", numVal=" + numVal +
				'}';
	}
}