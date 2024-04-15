/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cdi.converters;

import java.util.Objects;

import org.hibernate.annotations.Immutable;

@Immutable
public class MyData {
	public final String value;

	public MyData(String value) {
		this.value = value;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		MyData myData = (MyData) o;
		return value.equals( myData.value );
	}

	@Override
	public int hashCode() {
		return Objects.hash( value );
	}
}
