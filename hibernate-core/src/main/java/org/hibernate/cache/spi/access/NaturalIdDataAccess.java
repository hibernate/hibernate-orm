/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi.access;

import org.hibernate.cache.CacheException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Contract for managing transactional and concurrent access to cached naturalId
 * data.  The expected call sequences related to various operations are:<ul>
 *     <li><b>INSERTS</b> : {@link #insert} -> {@link #afterInsert}</li>
 *     <li><b>UPDATES</b> : {@link #lockItem} -> {@link #remove} -> {@link #update} -> {@link #afterUpdate}</li>
 *     <li><b>DELETES</b> : {@link #lockItem} -> {@link #remove} -> {@link #unlockItem}</li>
 *     <li><b>LOADS</b> : {@link #putFromLoad}</li>
 * </ul>
 * Note the special case of <b>UPDATES</b> above.  Because the cache key itself has changed here we need to remove the
 * old entry as well
 * <p/>
 * There is another usage pattern that is used to invalidate entries
 * afterQuery performing "bulk" HQL/SQL operations:
 * {@link #lockRegion} -> {@link #removeAll} -> {@link #unlockRegion}
 * <p/>
 * IMPORTANT : NaturalIds are not versioned so {@code null} will always be passed to the version parameter to:<ul>
 *     <li>{@link CachedDomainDataAccess#putFromLoad(SharedSessionContractImplementor, Object, Object, Object)}</li>
 *     <li>{@link CachedDomainDataAccess#putFromLoad(SharedSessionContractImplementor, Object, Object, Object, boolean)}</li>
 *     <li>{@link CachedDomainDataAccess#lockItem(SharedSessionContractImplementor, Object, Object)}</li>
 * </ul>
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Eric Dalquist
 */
public interface NaturalIdDataAccess extends CachedDomainDataAccess {


	/**
	 * To create instances of NaturalIdCacheKey for this region, Hibernate will invoke this method
	 * exclusively so that generated implementations can generate optimised keys.
	 * @param naturalIdValues the sequence of values which unequivocally identifies a cached element on this region
	 * @param rootEntityDescriptor the persister of the element being cached
	 *
	 * @return a key which can be used to identify an element unequivocally on this same region
	 */
	Object generateCacheKey(
			Object[] naturalIdValues,
			EntityPersister rootEntityDescriptor,
			SharedSessionContractImplementor session);

	/**
	 * Performs reverse operation to {@link #generateCacheKey}, returning
	 * the original naturalIdValues.
	 * @param cacheKey key returned from {@link #generateCacheKey}
	 *
	 * @return the sequence of values which unequivocally identifies a cached element on this region
	 */
	Object[] getNaturalIdValues(Object cacheKey);

	/**
	 * Called afterQuery an item has been inserted (beforeQuery the transaction completes),
	 * instead of calling evict().
	 * This method is used by "synchronous" concurrency strategies.
	 *
	 * @param session Current session
	 * @param key The item key
	 * @param value The item
	 *
	 * @return Were the contents of the cache actually changed by this operation?
	 *
	 * @throws CacheException Propagated from underlying cache provider
	 */
	boolean insert(SharedSessionContractImplementor session, Object key, Object value);

	/**
	 * Called afterQuery an item has been inserted (afterQuery the transaction completes),
	 * instead of calling release().
	 * This method is used by "asynchronous" concurrency strategies.
	 *
	 * @param session Current session
	 * @param key The item key
	 * @param value The item
	 *
	 * @return Were the contents of the cache actually changed by this operation?
	 *
	 * @throws CacheException Propagated from underlying cache provider
	 */
	boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value);

	/**
	 * Called afterQuery an item has been updated (beforeQuery the transaction completes),
	 * instead of calling evict(). This method is used by "synchronous" concurrency
	 * strategies.
	 *
	 * @param session Current session
	 * @param key The item key
	 * @param value The item
	 *
	 * @return Were the contents of the cache actually changed by this operation?
	 *
	 * @throws CacheException Propagated from underlying cache provider
	 */
	boolean update(SharedSessionContractImplementor session, Object key, Object value);

	/**
	 * Called afterQuery an item has been updated (afterQuery the transaction completes),
	 * instead of calling release().  This method is used by "asynchronous"
	 * concurrency strategies.
	 *
	 * @param session Current session
	 * @param key The item key
	 * @param value The item
	 * @param lock The lock previously obtained from {@link #lockItem}
	 *
	 * @return Were the contents of the cache actually changed by this operation?
	 *
	 * @throws CacheException Propagated from underlying cache provider
	 */
	boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, SoftLock lock);
}
