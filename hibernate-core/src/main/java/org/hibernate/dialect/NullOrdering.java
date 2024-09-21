/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

/**
 * The order of null.
 *
 * @author Christian Beikov
 */
public enum NullOrdering {
	/**
	 * Null is treated as the smallest value.
	 */
	SMALLEST,

	/**
	 * Null is treated as the greatest value.
	 */
	GREATEST,

	/**
	 * Null is always ordered first.
	 */
	FIRST,

	/**
	 * Null is always ordered last.
	 */
	LAST;
}
