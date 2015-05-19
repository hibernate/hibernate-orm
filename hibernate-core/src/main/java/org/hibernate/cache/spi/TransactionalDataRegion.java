/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi;

/**
 * Defines contract for regions which hold transactionally-managed data.
 * <p/>
 * The data is not transactionally managed within the region; merely it is
 * transactionally-managed in relation to its association with a particular
 * {@link org.hibernate.Session}.
 *
 * @author Steve Ebersole
 */
public interface TransactionalDataRegion extends Region {
	/**
	 * Is the underlying cache implementation aware of (and "participating in")
	 * ongoing JTA transactions?
	 * <p/>
	 * Regions which report that they are transaction-aware are considered
	 * "synchronous", in that we assume we can immediately (i.e. synchronously)
	 * write the changes to the cache and that the cache will properly manage
	 * application of the written changes within the bounds of ongoing JTA
	 * transactions.  Conversely, regions reporting false are considered
	 * "asynchronous", where it is assumed that changes must be manually
	 * delayed by Hibernate until we are certain that the current transaction
	 * is successful (i.e. maintaining READ_COMMITTED isolation).
	 *
	 * @return True if transaction aware; false otherwise.
	 */
	public boolean isTransactionAware();

	/**
	 * Get the description of the type of data to be stored here, which would have been given to the RegionFactory
	 * when creating this region
	 *
	 * @return The data descriptor.
	 */
	public CacheDataDescription getCacheDataDescription();
}
