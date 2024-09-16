/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.type.contributor.usertype;

import java.io.Serializable;

/**
 * @author Christian Beikov
 */
public class StringWrapper implements Serializable {

	private final String value;

	public StringWrapper(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		StringWrapper that = (StringWrapper) o;

		return value.equals( that.value );
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}
}
