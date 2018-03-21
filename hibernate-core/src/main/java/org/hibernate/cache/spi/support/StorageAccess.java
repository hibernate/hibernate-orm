/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi.support;

/**
 * @author Steve Ebersole
 */
public interface StorageAccess {
	default boolean contains(Object key) {
		return getFromCache( key ) != null;
	}

	Object getFromCache(Object key);

	void putIntoCache(Object key, Object value);

	void removeFromCache(Object key);

	void clearCache();

	void release();

}
