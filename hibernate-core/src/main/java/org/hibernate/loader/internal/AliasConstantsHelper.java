/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.internal;

/**
 * @author Sanne Grinovero
 */
public final class AliasConstantsHelper {

	private static final int MAX_POOL_SIZE = 40;
	private static final String[] pool = initPool( MAX_POOL_SIZE );

	/**
	 * Returns the same as Integer.toString( i ) + '_'
	 * Strings might be returned from a pool of constants, when i
	 * is within the range of expected most commonly requested elements.
	 *
	 * @param i
	 * @return
	 */
	public static String get(final int i) {
		if ( i < MAX_POOL_SIZE && i >= 0 ) {
			return pool[i];
		}
		else {
			return internalAlias( i );
		}
	}

	private static String[] initPool(final int maxPoolSize) {
		String[] pool = new String[maxPoolSize];
		for ( int i = 0; i < maxPoolSize; i++ ) {
			pool[i] = internalAlias( i );
		}
		return pool;
	}

	private static String internalAlias(final int i) {
		return Integer.toString( i ) + '_';
	}

}
