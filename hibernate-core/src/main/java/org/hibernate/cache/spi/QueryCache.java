/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.cache.CacheException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.Type;

/**
 * Defines the contract for caches capable of storing query results.  These
 * caches should only concern themselves with storing the matching result ids.
 * The transactional semantics are necessarily less strict than the semantics
 * of an item cache.
 * 
 * @author Gavin King
 */
public interface QueryCache {
	/**
	 * Clear items from the query cache.
	 *
	 * @throws CacheException Indicates a problem delegating to the underlying cache.
	 */
	public void clear() throws CacheException;

	/**
	 * Put a result into the query cache.
	 *
	 * @param key The cache key
	 * @param returnTypes The result types
	 * @param result The results to cache
	 * @param isNaturalKeyLookup Was this a natural id lookup?
	 * @param session The originating session
	 *
	 * @return Whether the put actually happened.
	 *
	 * @throws HibernateException Indicates a problem delegating to the underlying cache.
	 */
	public boolean put(QueryKey key, Type[] returnTypes, List result, boolean isNaturalKeyLookup, SessionImplementor session) throws HibernateException;

	/**
	 * Get results from the cache.
	 *
	 * @param key The cache key
	 * @param returnTypes The result types
	 * @param isNaturalKeyLookup Was this a natural id lookup?
	 * @param spaces The query spaces (used in invalidation plus validation checks)
	 * @param session The originating session
	 *
	 * @return The cached results; may be null.
	 *
	 * @throws HibernateException Indicates a problem delegating to the underlying cache.
	 */
	public List get(QueryKey key, Type[] returnTypes, boolean isNaturalKeyLookup, Set<Serializable> spaces, SessionImplementor session) throws HibernateException;

	/**
	 * Destroy the cache.
	 */
	public void destroy();

	/**
	 * The underlying cache factory region being used.
	 *
	 * @return The cache region.
	 */
	public QueryResultsRegion getRegion();

}
