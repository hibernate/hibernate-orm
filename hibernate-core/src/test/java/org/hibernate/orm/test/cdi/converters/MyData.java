/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.converters;

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
