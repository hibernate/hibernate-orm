/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	 * Strings might be returned from a pool of constants, when `i`
	 * is within the range of expected most commonly requested elements.
	 */
	public static String get(final int i) {
		return i < MAX_POOL_SIZE && i >= 0
				? pool[i]
				: internalAlias( i );
	}

	private static String[] initPool(final int maxPoolSize) {
		final String[] pool = new String[maxPoolSize];
		for ( int i = 0; i < maxPoolSize; i++ ) {
			pool[i] = internalAlias( i );
		}
		return pool;
	}

	private static String internalAlias(final int i) {
		return Integer.toString( i ) + '_';
	}

}
