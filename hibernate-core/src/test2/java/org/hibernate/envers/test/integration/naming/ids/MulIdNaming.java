/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.naming.ids;

import java.io.Serializable;
import javax.persistence.Column;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class MulIdNaming implements Serializable {
	@Column(name = "ID_1")
	private Integer id1;

	@Column(name = "ID_2")
	private Integer id2;

	public MulIdNaming() {
	}

	public MulIdNaming(Integer id1, Integer id2) {
		this.id1 = id1;
		this.id2 = id2;
	}

	public Integer getId1() {
		return id1;
	}

	public void setId1(Integer id1) {
		this.id1 = id1;
	}

	public Integer getId2() {
		return id2;
	}

	public void setId2(Integer id2) {
		this.id2 = id2;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof MulIdNaming) ) {
			return false;
		}

		MulIdNaming mulId = (MulIdNaming) o;

		if ( id1 != null ? !id1.equals( mulId.id1 ) : mulId.id1 != null ) {
			return false;
		}
		if ( id2 != null ? !id2.equals( mulId.id2 ) : mulId.id2 != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id1 != null ? id1.hashCode() : 0);
		result = 31 * result + (id2 != null ? id2.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "MulIdNaming(" + id1 + ", " + id2 + ")";
	}
}