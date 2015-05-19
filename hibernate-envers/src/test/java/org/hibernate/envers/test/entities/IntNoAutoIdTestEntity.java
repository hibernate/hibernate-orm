/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof IntNoAutoIdTestEntity) ) {
			return false;
		}

		IntNoAutoIdTestEntity that = (IntNoAutoIdTestEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		//noinspection RedundantIfStatement
		if ( numVal != null ? !numVal.equals( that.numVal ) : that.numVal != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (numVal != null ? numVal.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "INATE(id = " + id + ", numVal = " + numVal + ")";
	}
}