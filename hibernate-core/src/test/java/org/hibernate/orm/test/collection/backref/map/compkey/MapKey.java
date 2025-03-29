/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.backref.map.compkey;
import java.io.Serializable;

/**
 * A composite map key.
 *
 * @author Steve Ebersole
 */
public class MapKey implements Serializable {
	private String role;

	public MapKey() {
	}

	public MapKey(String role) {
		this.role = role;
	}

	public String getRole() {
		return role;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		MapKey mapKey = ( MapKey ) o;

		if ( !role.equals( mapKey.role ) ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		return role.hashCode();
	}
}
