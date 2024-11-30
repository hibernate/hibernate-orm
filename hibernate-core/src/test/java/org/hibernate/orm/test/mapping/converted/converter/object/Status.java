/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.object;

import org.hibernate.annotations.Imported;

/**
 * @author Marco Belladelli
 */
@Imported
public class Status {
	public static Status ONE = new Status( 1 );
	public static Status TWO = new Status( 2 );

	private final int value;

	public Status(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static Status from(int value) {
		return new Status( value );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final Status status = (Status) o;
		return value == status.value;
	}

	@Override
	public int hashCode() {
		return value;
	}

	@Override
	public String toString() {
		return "Status{" + "value=" + value + '}';
	}
}
