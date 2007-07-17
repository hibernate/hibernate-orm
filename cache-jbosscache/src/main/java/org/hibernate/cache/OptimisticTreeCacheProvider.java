//$Id: OptimisticTreeCacheProvider.java 9895 2006-05-05 19:27:17Z epbernard $
package org.hibernate.cache;

import java.util.Properties;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.transaction.TransactionManagerLookup;
import org.hibernate.transaction.TransactionManagerLookupFactory;
import org.jboss.cache.PropertyConfigurator;

/**
 * Support for a standalone JBossCache TreeCache instance utilizing TreeCache's
 * optimistic locking capabilities.  This capability was added in JBossCache
 * version 1.3.0; as such this provider will only work with that version or
 * higher.
 * <p/>
 * The TreeCache instance is configured via a local config resource.  The
 * resource to be used for configuration can be controlled by specifying a value
 * for the {@link #CONFIG_RESOURCE} config property.
 *
 * @author Steve Ebersole
 */
public class OptimisticTreeCacheProvider implements CacheProvider {

	/**
	 * @deprecated use {@link Environment.CACHE_PROVIDER_CONFIG}
	 */
	public static final String CONFIG_RESOURCE = "hibernate.cache.opt_tree_cache.config";
	public static final String DEFAULT_CONFIG = "treecache.xml";

	private static final String NODE_LOCKING_SCHEME = "OPTIMISTIC";
	private static final Log log = LogFactory.getLog( OptimisticTreeCacheProvider.class );

	private org.jboss.cache.TreeCache cache;

	/**
	 * Construct and configure the Cache representation of a named cache region.
	 *
	 * @param regionName the name of the cache region
	 * @param properties configuration settings
	 * @return The Cache representation of the named cache region.
	 * @throws CacheException
	 *          Indicates an error building the cache region.
	 */
	public Cache buildCache(String regionName, Properties properties) throws CacheException {
		return new OptimisticTreeCache( cache, regionName );
	}

	public long nextTimestamp() {
		return System.currentTimeMillis() / 100;
	}

	/**
	 * Prepare the underlying JBossCache TreeCache instance.
	 *
	 * @param properties All current config settings.
	 * @throws CacheException
	 *          Indicates a problem preparing cache for use.
	 */
	public void start(Properties properties) {
		String resource = properties.getProperty( Environment.CACHE_PROVIDER_CONFIG );
		if (resource == null) {
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
			TransactionManagerLookup transactionManagerLookup =
					TransactionManagerLookupFactory.getTransactionManagerLookup( properties );
			if ( transactionManagerLookup == null ) {
				throw new CacheException(
						"JBossCache only supports optimisitc locking with a configured " +
						"TransactionManagerLookup (" + Environment.TRANSACTION_MANAGER_STRATEGY + ")"
				);
			}
			cache.setTransactionManagerLookup(
					new TransactionManagerLookupAdaptor(
							transactionManagerLookup,
							properties
					)
			);
			if ( ! NODE_LOCKING_SCHEME.equalsIgnoreCase( cache.getNodeLockingScheme() ) ) {
				log.info( "Overriding node-locking-scheme to : " + NODE_LOCKING_SCHEME );
				cache.setNodeLockingScheme( NODE_LOCKING_SCHEME );
			}
			cache.start();
		}
		catch ( Exception e ) {
			throw new CacheException( e );
		}
	}

	public void stop() {
		if ( cache != null ) {
			cache.stop();
			cache.destroy();
			cache = null;
		}
	}

	public boolean isMinimalPutsEnabledByDefault() {
		return true;
	}

	static final class TransactionManagerLookupAdaptor implements org.jboss.cache.TransactionManagerLookup {
		private final TransactionManagerLookup tml;
		private final Properties props;

		TransactionManagerLookupAdaptor(TransactionManagerLookup tml, Properties props) {
			this.tml = tml;
			this.props = props;
		}

		public TransactionManager getTransactionManager() throws Exception {
			return tml.getTransactionManager( props );
		}
	}

	public org.jboss.cache.TreeCache getUnderlyingCache() {
		return cache;
	}
}
