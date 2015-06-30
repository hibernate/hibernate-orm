/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi.access;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Contract for managing transactional and concurrent access to cached entity
 * data.  The expected call sequences related to various operations are:<ul>
 *     <li><b>INSERTS</b> : {@link #insert} -> {@link #afterInsert}</li>
 *     <li><b>UPDATES</b> : {@link #lockItem} -> {@link #update} -> {@link #afterUpdate}</li>
 *     <li><b>DELETES</b> : {@link #lockItem} -> {@link #remove} -> {@link #unlockItem}</li>
 *     <li><b>LOADS</b> : {@link @putFromLoad}</li>
 * </ul>
 * <p/>
 * There is another usage pattern that is used to invalidate entries
 * after performing "bulk" HQL/SQL operations:
 * {@link #lockRegion} -> {@link #removeAll} -> {@link #unlockRegion}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface EntityRegionAccessStrategy extends RegionAccessStrategy {

	/**
	 * To create instances of keys for this region, Hibernate will invoke this method
	 * exclusively so that generated implementations can generate optimised keys.
	 * @param id the primary identifier of the entity
	 * @param persister the persister for the type for which a key is being generated
	 * @param factory a reference to the current SessionFactory
	 * @param tenantIdentifier the tenant id, or null if multi-tenancy is not being used.
	 * @return a key which can be used to identify this entity on this same region
	 */
	public Object generateCacheKey(Object id, EntityPersister persister, SessionFactoryImplementor factory, String tenantIdentifier);

	/**
	 * Performs reverse operation to {@link #generateCacheKey(Object, EntityPersister, SessionFactoryImplementor, String)}
	 *
	 * @param cacheKey key previously returned from {@link #generateCacheKey(Object, EntityPersister, SessionFactoryImplementor, String)}
	 * @return original id passed to {@link #generateCacheKey(Object, EntityPersister, SessionFactoryImplementor, String)}
	 */
	public Object getCacheKeyId(Object cacheKey);

	/**
	 * Get the wrapped entity cache region
	 *
	 * @return The underlying region
	 */
	public EntityRegion getRegion();

	/**
	 * Called after an item has been inserted (before the transaction completes),
	 * instead of calling evict().
	 * This method is used by "synchronous" concurrency strategies.
	 *
	 * @param key The item key
	 * @param value The item
	 * @param version The item's version value
	 * @return Were the contents of the cache actual changed by this operation?
	 * @throws CacheException Propagated from underlying {@link org.hibernate.cache.spi.Region}
	 */
	public boolean insert(Object key, Object value, Object version) throws CacheException;

	/**
	 * Called after an item has been inserted (after the transaction completes),
	 * instead of calling release().
	 * This method is used by "asynchronous" concurrency strategies.
	 *
	 * @param key The item key
	 * @param value The item
	 * @param version The item's version value
	 * @return Were the contents of the cache actual changed by this operation?
	 * @throws CacheException Propagated from underlying {@link org.hibernate.cache.spi.Region}
	 */
	public boolean afterInsert(Object key, Object value, Object version) throws CacheException;

	/**
	 * Called after an item has been updated (before the transaction completes),
	 * instead of calling evict(). This method is used by "synchronous" concurrency
	 * strategies.
	 *
	 * @param key The item key
	 * @param value The item
	 * @param currentVersion The item's current version value
	 * @param previousVersion The item's previous version value
	 * @return Were the contents of the cache actual changed by this operation?
	 * @throws CacheException Propagated from underlying {@link org.hibernate.cache.spi.Region}
	 */
	public boolean update(Object key, Object value, Object currentVersion, Object previousVersion) throws CacheException;

	/**
	 * Called after an item has been updated (after the transaction completes),
	 * instead of calling release().  This method is used by "asynchronous"
	 * concurrency strategies.
	 *
	 * @param key The item key
	 * @param value The item
	 * @param currentVersion The item's current version value
	 * @param previousVersion The item's previous version value
	 * @param lock The lock previously obtained from {@link #lockItem}
	 * @return Were the contents of the cache actual changed by this operation?
	 * @throws CacheException Propagated from underlying {@link org.hibernate.cache.spi.Region}
	 */
	public boolean afterUpdate(Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock) throws CacheException;
}
