/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cache.spi.access;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.NaturalIdRegion;

/**
 * Contract for managing transactional and concurrent access to cached naturalId
 * data.  The expected call sequences related to various operations are:<ul>
 *     <li><b>INSERTS</b> : {@link #insert} -> {@link #afterInsert}</li>
 *     <li><b>UPDATES</b> : {@link #lockItem} -> {@link #remove} -> {@link #update} -> {@link #afterUpdate}</li>
 *     <li><b>DELETES</b> : {@link #lockItem} -> {@link #remove} -> {@link #unlockItem}</li>
 *     <li><b>LOADS</b> : {@link @putFromLoad}</li>
 * </ul>
 * Note the special case of <b>UPDATES</b> above.  Because the cache key itself has changed here we need to remove the
 * old entry as well as
 * <p/>
 * There is another usage pattern that is used to invalidate entries
 * after performing "bulk" HQL/SQL operations:
 * {@link #lockRegion} -> {@link #removeAll} -> {@link #unlockRegion}
 * <p/>
 * IMPORTANT : NaturalIds are not versioned so {@code null} will always be passed to the version parameter to:<ul>
 *     <li>{@link #putFromLoad(Object, Object, long, Object)}</li>
 *     <li>{@link #putFromLoad(Object, Object, long, Object, boolean)}</li>
 *     <li>{@link #lockItem(Object, Object)}</li>
 * </ul>
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Eric Dalquist
 */
public interface NaturalIdRegionAccessStrategy extends RegionAccessStrategy {

	/**
	 * Get the wrapped naturalId cache region
	 *
	 * @return The underlying region
	 */
	public NaturalIdRegion getRegion();

	/**
	 * Called after an item has been inserted (before the transaction completes),
	 * instead of calling evict().
	 * This method is used by "synchronous" concurrency strategies.
	 *
	 * @param key The item key
	 * @param value The item
	 * @return Were the contents of the cache actual changed by this operation?
	 * @throws CacheException Propogated from underlying {@link org.hibernate.cache.spi.Region}
	 */
	public boolean insert(Object key, Object value) throws CacheException;

	/**
	 * Called after an item has been inserted (after the transaction completes),
	 * instead of calling release().
	 * This method is used by "asynchronous" concurrency strategies.
	 *
	 * @param key The item key
	 * @param value The item
	 * @return Were the contents of the cache actual changed by this operation?
	 * @throws CacheException Propogated from underlying {@link org.hibernate.cache.spi.Region}
	 */
	public boolean afterInsert(Object key, Object value) throws CacheException;

	/**
	 * Called after an item has been updated (before the transaction completes),
	 * instead of calling evict(). This method is used by "synchronous" concurrency
	 * strategies.
	 *
	 * @param key The item key
	 * @param value The item
	 * @return Were the contents of the cache actual changed by this operation?
	 * @throws CacheException Propogated from underlying {@link org.hibernate.cache.spi.Region}
	 */
	public boolean update(Object key, Object value) throws CacheException;

	/**
	 * Called after an item has been updated (after the transaction completes),
	 * instead of calling release().  This method is used by "asynchronous"
	 * concurrency strategies.
	 *
	 * @param key The item key
	 * @param value The item
	 * @param lock The lock previously obtained from {@link #lockItem}
	 * @return Were the contents of the cache actual changed by this operation?
	 * @throws CacheException Propogated from underlying {@link org.hibernate.cache.spi.Region}
	 */
	public boolean afterUpdate(Object key, Object value, SoftLock lock) throws CacheException;
}
