/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.testing.cache;

import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cache.ReadWriteCache;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.tm.ConnectionProviderImpl;
import org.hibernate.testing.tm.TransactionManagerLookupImpl;
import org.hibernate.transaction.JDBCTransactionFactory;

/**
 * Common requirement testing for each {@link org.hibernate.cache.CacheProvider} impl.
 *
 * @author Steve Ebersole
 */
public abstract class BaseCacheProviderTestCase extends BaseCacheTestCase {

	// note that a lot of the fucntionality here is intended to be used
	// in creating specific tests for each CacheProvider that would extend
	// from a base test case (this) for common requirement testing...

	public BaseCacheProviderTestCase(String x) {
		super( x );
	}

	@Override
	protected void configCache(final Configuration cfg) {
		cfg.setProperty( Environment.CACHE_PROVIDER, getCacheProvider().getName() );

		if ( getConfigResourceKey() != null ) {
			cfg.setProperty( getConfigResourceKey(), getConfigResourceLocation() );
		}
	}

	/**
	 * The cache provider to be tested.
	 *
	 * @return The cache provider.
	 */
	protected abstract Class getCacheProvider();

	@Override
	protected Map getMapFromCachedEntry(final Object entry) {
		final Map map;
		if (entry instanceof ReadWriteCache.Item) {
			map = (Map)((ReadWriteCache.Item)entry).getValue();
		} else {
			map = (Map)entry;
		}
		return map;
	}
}
