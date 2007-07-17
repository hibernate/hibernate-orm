// $Id: JndiBoundTreeCacheProvider.java 6079 2005-03-16 06:01:18Z oneovthafew $
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
