/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
