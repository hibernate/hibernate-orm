/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.tools;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class ArgumentsTools {
	public static void checkNotNull(Object o, String paramName) {
		if ( o == null ) {
			throw new IllegalArgumentException( paramName + " cannot be null." );
		}
	}

	public static void checkPositive(Number i, String paramName) {
		if ( i.longValue() <= 0L ) {
			throw new IllegalArgumentException( paramName + " has to be greater than 0." );
		}
	}
}
