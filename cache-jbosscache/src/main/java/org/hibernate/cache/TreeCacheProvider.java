//$Id: TreeCacheProvider.java 11398 2007-04-10 14:54:07Z steve.ebersole@jboss.com $
package org.hibernate.cache;

import java.util.Properties;
import javax.transaction.TransactionManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.transaction.TransactionManagerLookup;
import org.hibernate.transaction.TransactionManagerLookupFactory;
import org.hibernate.cfg.Environment;
import org.jboss.cache.PropertyConfigurator;

/**
 * Support for a standalone JBossCache (TreeCache) instance.  The JBossCache is configured
 * via a local config resource.
 *
 * @author Gavin King
 */
public class TreeCacheProvider implements CacheProvider {

	/**
	 * @deprecated use {@link org.hibernate.cfg.Environment#CACHE_PROVIDER_CONFIG}
	 */
	public static final String CONFIG_RESOURCE = "hibernate.cache.tree_cache.config";
	public static final String DEFAULT_CONFIG = "treecache.xml";

	private static final Logger log = LoggerFactory.getLogger( TreeCacheProvider.class );

	private org.jboss.cache.TreeCache cache;
	private TransactionManager transactionManager;

	/**
	 * Construct and configure the Cache representation of a named cache region.
	 *
	 * @param regionName the name of the cache region
	 * @param properties configuration settings
	 * @return The Cache representation of the named cache region.
	 * @throws CacheException Indicates an error building the cache region.
	 */
	public Cache buildCache(String regionName, Properties properties) throws CacheException {
		return new TreeCache(cache, regionName, transactionManager);
	}

	public long nextTimestamp() {
		return System.currentTimeMillis() / 100;
	}

	/**
	 * Prepare the underlying JBossCache TreeCache instance.
	 *
	 * @param properties All current config settings.
	 *
	 * @throws CacheException Indicates a problem preparing cache for use.
	 */
	public void start(Properties properties) {
		String resource = properties.getProperty( Environment.CACHE_PROVIDER_CONFIG );

		if ( resource == null ) {
			resource = properties.getProperty( CONFIG_RESOURCE );
		}
		if ( resource == null ) {
			resource = DEFAULT_CONFIG;
		}
		log.debug( "Configuring TreeCache from resource [" + resource + "]" );
		try {
			cache = new org.jboss.cache.TreeCache();
			PropertyConfigurator config = new PropertyConfigurator();
			config.configure( cache, resource );
			TransactionManagerLookup transactionManagerLookup = TransactionManagerLookupFactory.getTransactionManagerLookup(properties);
			if (transactionManagerLookup!=null) {
				cache.setTransactionManagerLookup( new TransactionManagerLookupAdaptor(transactionManagerLookup, properties) );
				transactionManager = transactionManagerLookup.getTransactionManager(properties);
			}
			cache.start();
		}
		catch (Exception e) {
			throw new CacheException(e);
		}
	}

	public void stop() {
		if (cache!=null) {
			cache.stop();
			cache.destroy();
			cache=null;
		}
	}
	
	public boolean isMinimalPutsEnabledByDefault() {
		return true;
	}

	static final class TransactionManagerLookupAdaptor implements org.jboss.cache.TransactionManagerLookup {
		private final TransactionManagerLookup tml;
		private final Properties props;
		TransactionManagerLookupAdaptor(TransactionManagerLookup tml, Properties props) {
			this.tml=tml;
			this.props=props;
		}
		public TransactionManager getTransactionManager() throws Exception {
			return tml.getTransactionManager(props);
		}
	}

	public org.jboss.cache.TreeCache getUnderlyingCache() {
		return cache;
	}
}
