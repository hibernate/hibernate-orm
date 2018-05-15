/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.internal;

/**
 * @author Steve Ebersole
 */
public class Helper {
	public static <T> T firstNonNull(T v1, T v2) {
		return v1 != null ? v1 : v2;
	}

	@SafeVarargs
	public static <T> T firstNonNull(T... values) {
		if ( values != null ) {
			for ( T value : values ) {
				if ( value != null ) {
					return value;
				}
			}
		}

		return null;
	}

	private Helper() {
	}
}
