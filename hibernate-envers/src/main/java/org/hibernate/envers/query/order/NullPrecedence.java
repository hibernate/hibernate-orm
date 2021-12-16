/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.order;

/**
 * Defines the possible null handling modes.
 *
 * @author Chris Cranford
 */
public enum NullPrecedence {
	/**
	 * Null values will be rendered before non-null values.
	 */
	FIRST,

	/**
	 * Null values will be rendered after non-null values.
	 */
	LAST
}
