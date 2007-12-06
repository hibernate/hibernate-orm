/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.cache;

import java.util.Properties;

import javax.transaction.TransactionManager;

import org.hibernate.transaction.TransactionManagerLookup;
import org.hibernate.transaction.TransactionManagerLookupFactory;

/**
 * Support for JBossCache (TreeCache), where the cache instance is available
 * via JNDI lookup.
 *
 * @author Steve Ebersole
 */
public class JndiBoundTreeCacheProvider extends AbstractJndiBoundCacheProvider {

	private TransactionManager transactionManager;

	/**
	 * Construct a Cache representing the "region" within in the underlying cache
	 * provider.
	 *
	 * @param regionName the name of the cache region
	 * @param properties configuration settings
	 *
	 * @throws CacheException
	 */
	public Cache buildCache(String regionName, Properties properties) throws CacheException {
		return new TreeCache( getTreeCacheInstance(), regionName, transactionManager );
	}

	public void prepare(Properties properties) throws CacheException {
		TransactionManagerLookup transactionManagerLookup = TransactionManagerLookupFactory.getTransactionManagerLookup(properties);
		if (transactionManagerLookup!=null) {
			transactionManager = transactionManagerLookup.getTransactionManager(properties);
		}
	}
	/**
	 * Generate a timestamp
	 */
	public long nextTimestamp() {
		return System.currentTimeMillis() / 100;
	}

	/**
	 * By default, should minimal-puts mode be enabled when using this cache.
	 * <p/>
	 * Since TreeCache is a clusterable cache and we are only getting a
	 * reference the instance from JNDI, safest to assume a clustered
	 * setup and return true here.
	 *
	 * @return True.
	 */
	public boolean isMinimalPutsEnabledByDefault() {
		return true;
	}

	public org.jboss.cache.TreeCache getTreeCacheInstance() {
		return ( org.jboss.cache.TreeCache ) super.getCache();
	}
}
