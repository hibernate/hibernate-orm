/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
