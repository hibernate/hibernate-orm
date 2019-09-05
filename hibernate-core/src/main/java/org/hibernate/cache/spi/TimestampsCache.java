/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi;

import java.util.Collection;

import org.hibernate.cache.CacheException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Wrapper for a {@link TimestampsRegion} adding handling of stale results
 *
 * @author Steve Ebersole
 */
public interface TimestampsCache {
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
	 */
	boolean isUpToDate(
			String[] spaces,
			Long timestamp,
			SharedSessionContractImplementor session);

	/**
	 * Perform an up-to-date check for the given set of query spaces as
	 * part of verifying the validity of cached query results.
	 */
	boolean isUpToDate(
			Collection<String> spaces,
			Long timestamp,
			SharedSessionContractImplementor session);

	default void clear() throws CacheException {
		getRegion().clear();
	}

	default void destroy() {
		// nothing to do - the region itself is destroyed
	}

}
