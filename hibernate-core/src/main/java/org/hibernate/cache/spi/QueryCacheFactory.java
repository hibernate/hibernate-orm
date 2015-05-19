/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi;

import java.util.Properties;

import org.hibernate.boot.spi.SessionFactoryOptions;

/**
 * Defines a factory for query cache instances.  These factories are responsible for
 * creating individual QueryCache instances.
 *
 * @author Steve Ebersole
 */
public interface QueryCacheFactory {
	/**
	 * Builds a named query cache.
	 *
	 * @param regionName The cache region name
	 * @param updateTimestampsCache The cache of timestamp values to use to perform up-to-date checks.
	 * @param settings The Hibernate SessionFactory settings.
	 * @param props Any properties.
	 *
	 * @return The cache.
	 */
	public QueryCache getQueryCache(
			String regionName,
			UpdateTimestampsCache updateTimestampsCache,
			SessionFactoryOptions settings,
			Properties props);
}
