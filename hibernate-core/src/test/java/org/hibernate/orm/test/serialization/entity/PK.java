/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.serialization.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * This class should be in a package that is different from the test
 * so that the test and entity that uses this class for its primary
 * key does not have access to private field, and the protected getter and setter.
 *
 * @author Gail Badner
 */
public class PK implements Serializable {
	private Long id;

	public PK() {
	}

	public PK(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		PK pk = (PK) o;
		return Objects.equals( id, pk.id );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id );
	}
}
