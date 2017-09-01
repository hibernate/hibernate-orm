/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi;

import java.util.Set;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Wrapper for a {@link TimestampsRegion} adding handling of stale results
 *
 * @author Steve Ebersole
 */
public interface TimestampsCache {
	TimestampsRegion getRegion();

	/**
	 * Perform pre-invalidation of the passed spaces (table names)
	 * using the passed region for storing update-timestamps
	 */
	void preInvalidate(
			String[] spaces,
			SharedSessionContractImplementor session);

	/**
	 * Perform invalidation of the passed spaces (table names)
	 * using the passed region for storing update-timestamps
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
			Set<String> spaces,
			Long timestamp,
			SharedSessionContractImplementor session);
}
