/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.compare;


/**
 * @author Gavin King
 */
public final class EqualsHelper {

	public static boolean equals(final Object x, final Object y) {
		return x == y || ( x != null && y != null && x.equals( y ) );
	}
	
	private EqualsHelper() {}

}
