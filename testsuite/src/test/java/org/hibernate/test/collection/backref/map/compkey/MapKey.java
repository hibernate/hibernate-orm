/*
 * Copyright (c) 2007, Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, v. 2.1. This program is distributed in the
 * hope that it will be useful, but WITHOUT A WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received a
 * copy of the GNU Lesser General Public License, v.2.1 along with this
 * distribution; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Red Hat Author(s): Steve Ebersole
 */
package org.hibernate.test.collection.backref.map.compkey;

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
