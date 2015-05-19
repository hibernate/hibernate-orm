/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.internal;

import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.spi.QueryCache;
import org.hibernate.cache.spi.QueryCacheFactory;
import org.hibernate.cache.spi.UpdateTimestampsCache;

/**
 * Standard Hibernate implementation of the QueryCacheFactory interface.  Returns instances of
 * {@link StandardQueryCache}.
 */
public class StandardQueryCacheFactory implements QueryCacheFactory {
	/**
	 * Singleton access
	 */
	public static final StandardQueryCacheFactory INSTANCE = new StandardQueryCacheFactory();

	@Override
	public QueryCache getQueryCache(
			final String regionName,
			final UpdateTimestampsCache updateTimestampsCache,
			final SessionFactoryOptions settings,
			final Properties props) throws HibernateException {
		return new StandardQueryCache(settings, props, updateTimestampsCache, regionName);
	}
}
