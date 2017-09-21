/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.jcache;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.jcache.time.Timestamper;
import org.hibernate.cache.spi.CacheTransactionContext;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.NotYetImplementedFor6Exception;

import org.jboss.logging.Logger;

/**
 * @author Alex Snaps
 */
public class JCacheRegionFactory implements RegionFactory, ServiceRegistryAwareService {

	private static final String PROP_PREFIX = "hibernate.javax.cache";

	public static final String PROVIDER = PROP_PREFIX + ".provider";
	public static final String CONFIG_URI = PROP_PREFIX + ".uri";

	private static final JCacheMessageLogger LOG = Logger.getMessageLogger(
			JCacheMessageLogger.class,
			JCacheRegionFactory.class.getName()
	);

	private final AtomicBoolean started = new AtomicBoolean( false );
	private volatile CacheManager cacheManager;
	private SessionFactoryOptions options;
	private ServiceRegistryImplementor serviceRegistry;



	@Override
	public boolean isMinimalPutsEnabledByDefault() {
		return true;
	}

	@Override
	public AccessType getDefaultAccessType() {
		return AccessType.READ_WRITE;
	}

	@Override
	public long nextTimestamp() {
		return nextTS();
	}

	@Override
	public CacheTransactionContext createTransactionContext(SharedSessionContractImplementor session) {
		return new CacheTransactionContextImpl( this );
	}

	@Override
	public DomainDataRegion buildDomainDataRegion(
			DomainDataRegionConfig regionConfig,
			DomainDataRegionBuildingContext buildingContext) {
		return new DomainDataRegionImpl( regionConfig, this, buildingContext );
	}

	@Override
	public QueryResultsRegion buildQueryResultsRegion(
			String regionName,
			SessionFactoryImplementor sessionFactory) {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public TimestampsRegion buildTimestampsRegion(
			String regionName,
			SessionFactoryImplementor sessionFactory) {
		throw new NotYetImplementedFor6Exception(  );
	}

	boolean isStarted() {
		return started.get() && cacheManager != null;
	}

	protected Cache<Object, Object> getOrCreateCache(String regionName, Properties properties, CacheDataDescription metadata) {
		checkStatus();
		final Cache<Object, Object> cache = cacheManager.getCache( regionName );
		if ( cache == null ) {
			try {
				return cacheManager.createCache( regionName, newDefaultConfig( properties, metadata ) );
			}
			catch ( CacheException e ) {
				final Cache<Object, Object> existing = cacheManager.getCache( regionName );
				if ( existing != null ) {
					return existing;
				}
				throw e;
			}
		}
		return cache;
	}

	protected Configuration<Object, Object> newDefaultConfig(Properties properties, CacheDataDescription metadata) {
		return new MutableConfiguration<Object, Object>();
	}

	CacheManager getCacheManager() {
		return cacheManager;
	}

	static long nextTS() {
		return Timestamper.next();
	}

	static int timeOut() {
		return (int) TimeUnit.SECONDS.toMillis( 60 ) * Timestamper.ONE_MS;
	}

	private void checkStatus() {
		if(!isStarted()) {
			throw new IllegalStateException("JCacheRegionFactory not yet started!");
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Start up
	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void start() {
		final ConfigurationService configService = serviceRegistry.getService( ConfigurationService.class );

		if ( started.compareAndSet( false, true ) ) {
			synchronized ( this ) {
				this.options = options;
				try {
					final CachingProvider cachingProvider;
					final String provider = configService.getSetting(
							PROVIDER,
							StandardConverters.STRING
					);

					if ( provider != null ) {
						cachingProvider = Caching.getCachingProvider( provider );
					}
					else {
						cachingProvider = Caching.getCachingProvider();
					}

					final CacheManager cacheManager;
					final String cacheManagerUri = configService.getSetting(
							CONFIG_URI,
							StandardConverters.STRING
					);

					if ( cacheManagerUri != null ) {
						URI uri;
						try {
							uri = new URI( cacheManagerUri );
						}
						catch ( URISyntaxException e ) {
							throw new CacheException( "Couldn't create URI from " + cacheManagerUri, e );
						}
						cacheManager = cachingProvider.getCacheManager( uri, cachingProvider.getDefaultClassLoader() );
					}
					else {
						cacheManager = cachingProvider.getCacheManager();
					}
					this.cacheManager = cacheManager;
				}
				finally {
					if ( this.cacheManager == null ) {
						started.set( false );
					}
				}
			}
		}
		else {
			LOG.attemptToRestartAlreadyStartedJCacheProvider();
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Shut down

	@Override
	public void stop() {
		if ( started.compareAndSet( true, false ) ) {
			synchronized ( this ) {
				cacheManager.close();
				cacheManager = null;
			}
		}
		else {
			LOG.attemptToRestopAlreadyStoppedJCacheProvider();
		}
	}

}
