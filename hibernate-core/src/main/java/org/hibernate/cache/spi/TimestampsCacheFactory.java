/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi;

/**
 * Responsible for building the {@link TimestampsCache} to use for
 * managing query results with respect to staleness of the underlying
 * tables (sometimes called "query spaces" or "table spaces").
 * <p>
 * An implementation may be selected using the configuration property
 * {@link org.hibernate.cfg.AvailableSettings#QUERY_CACHE_FACTORY}.
 *
 * @author Steve Ebersole
 */
public interface TimestampsCacheFactory {
	/**
	 * Build the {@link TimestampsCache}.
	 */
	TimestampsCache buildTimestampsCache(CacheImplementor cacheManager, TimestampsRegion timestampsRegion);
}
