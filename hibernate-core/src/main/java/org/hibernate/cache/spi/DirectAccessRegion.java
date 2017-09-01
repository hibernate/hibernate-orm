/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi;

/**
 * Specialized Region whose data is accessed directly - not requiring
 * key wrapping, e.g.
 *
 * @author Steve Ebersole
 */
public interface DirectAccessRegion extends Region {
	interface DataAccess {
		Object getFromCache(Object key);
		void addToCache(Object key, Object value);
		void removeFromCache(Object key);
		void clearCache();
	}

	DataAccess getAccess();
//	Object get(Object key, SharedSessionContractImplementor session);
//	void put(Object key, Object value, SharedSessionContractImplementor session);
//
//	/**
//	 * Forcibly evict an item from the cache immediately without regard for transaction
//	 * isolation.  This behavior is exactly Hibernate legacy behavior, but it is also required
//	 * by JPA - so we cannot remove it.
//	 *
//	 * @param key The key of the item to remove
//	 */
//	void evict(Object key);
//
//	/**
//	 * Forcibly evict all items from the cache immediately without regard for transaction
//	 * isolation.  This behavior is exactly Hibernate legacy behavior, but it is also required
//	 * by JPA - so we cannot remove it.
//	 */
//	void evictAll();
}
