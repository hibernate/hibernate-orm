/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi;

import java.io.Serializable;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.cache.CacheException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Wrapper for a {@link TimestampsRegion} adding handling of stale results
 *
 * @author Steve Ebersole
 */
public interface TimestampsCache extends UpdateTimestampsCache {
	/**
	 * The region used to store all timestamps data
	 */
	TimestampsRegion getRegion();

	/**
	 * Perform pre-invalidation of the passed spaces (table names)
	 * against the timestamps region data
	 */
	void preInvalidate(
			String[] spaces,
			SharedSessionContractImplementor session);

	/**
	 * Perform invalidation of the passed spaces (table names)
	 * against the timestamps region data
	 */
	void invalidate(
			String[] spaces,
			SharedSessionContractImplementor session);

	/**
	 * Perform an up-to-date check for the given set of query spaces as
	 * part of verifying the validity of cached query results.
	 *
	 * @param spaces The spaces to check
	 * @param timestamp The timestamp from the transaction when the query results were cached.
	 * @param session The session whether this check originated.
	 *
	 * @return Whether all those spaces are up-to-date
	 */
	boolean isUpToDate(
			String[] spaces,
			Long timestamp,
			SharedSessionContractImplementor session);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecations


	@Override
	default void preInvalidate(Serializable[] spaces, SharedSessionContractImplementor session) {
		final String[] spaceStrings = new String[ spaces.length ];
		// todo - does this copy work?
		System.arraycopy( spaces, 0, spaceStrings, 0, spaces.length );
		preInvalidate( spaceStrings, session );
	}

	@Override
	default void invalidate(Serializable[] spaces, SharedSessionContractImplementor session) {
		final String[] spaceStrings = new String[ spaces.length ];
		// todo - does this copy work?
		System.arraycopy( spaces, 0, spaceStrings, 0, spaces.length );
		invalidate( spaceStrings, session );
	}

	@Override
	default boolean isUpToDate(
			Set<Serializable> spaces,
			Long timestamp,
			SharedSessionContractImplementor session) {
		final String[] spaceArray = new String[ spaces.size() ];

		spaces.forEach(
				new Consumer<Serializable>() {
					int position = 0;
					@Override
					public void accept(Serializable serializable) {
						spaceArray[position++] = (String) serializable;
					}
				}
		);

		return isUpToDate( spaceArray, timestamp, session );
	}

	@Override
	default void clear() throws CacheException {
		getRegion().clear();
	}

	@Override
	default void destroy() {
		// nothing to do - the region itself is destroyed
	}
}
