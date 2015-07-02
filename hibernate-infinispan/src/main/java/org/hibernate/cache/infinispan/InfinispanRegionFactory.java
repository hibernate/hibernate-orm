/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.collection.CollectionRegionImpl;
import org.hibernate.cache.infinispan.entity.EntityRegionImpl;
import org.hibernate.cache.infinispan.impl.BaseRegion;
import org.hibernate.cache.infinispan.naturalid.NaturalIdRegionImpl;
import org.hibernate.cache.infinispan.query.QueryResultsRegionImpl;
import org.hibernate.cache.infinispan.timestamp.ClusteredTimestampsRegionImpl;
import org.hibernate.cache.infinispan.timestamp.TimestampTypeOverrides;
import org.hibernate.cache.infinispan.timestamp.TimestampsRegionImpl;
import org.hibernate.cache.infinispan.tm.HibernateTransactionManagerLookup;
import org.hibernate.cache.infinispan.util.CacheCommandFactory;
import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.internal.SimpleCacheKeysFactory;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;

import org.infinispan.AdvancedCache;
import org.infinispan.commands.module.ModuleCommandFactory;
import org.infinispan.commons.util.FileLookup;
import org.infinispan.commons.util.FileLookupFactory;
import org.infinispan.commons.util.Util;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.GenericTransactionManagerLookup;
import org.infinispan.util.concurrent.IsolationLevel;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * A {@link RegionFactory} for <a href="http://www.jboss.org/infinispan">Infinispan</a>-backed cache
 * regions.
 *
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
public class InfinispanRegionFactory implements RegionFactory {
	private static final Log log = LogFactory.getLog( InfinispanRegionFactory.class );

	private static final String PREFIX = "hibernate.cache.infinispan.";

	private static final String CONFIG_SUFFIX = ".cfg";

	private static final String STRATEGY_SUFFIX = ".eviction.strategy";

	private static final String WAKE_UP_INTERVAL_SUFFIX = ".eviction.wake_up_interval";

	private static final String MAX_ENTRIES_SUFFIX = ".eviction.max_entries";

	private static final String LIFESPAN_SUFFIX = ".expiration.lifespan";

	private static final String MAX_IDLE_SUFFIX = ".expiration.max_idle";

//   private static final String STATISTICS_SUFFIX = ".statistics";

	/**
	 * Classpath or filesystem resource containing Infinispan configurations the factory should use.
	 *
	 * @see #DEF_INFINISPAN_CONFIG_RESOURCE
	 */
	public static final String INFINISPAN_CONFIG_RESOURCE_PROP = "hibernate.cache.infinispan.cfg";

   /**
    * Property name that controls whether Infinispan statistics are enabled.
    * The property value is expected to be a boolean true or false, and it
    * overrides statistic configuration in base Infinispan configuration,
    * if provided.
    */
	public static final String INFINISPAN_GLOBAL_STATISTICS_PROP = "hibernate.cache.infinispan.statistics";

	/**
	 * Property that controls whether Infinispan should interact with the
	 * transaction manager as a {@link javax.transaction.Synchronization} or as
	 * an XA resource. If the property is set to true, it will be a
	 * synchronization, otherwise an XA resource.
	 *
	 * @see #DEF_USE_SYNCHRONIZATION
	 */
	public static final String INFINISPAN_USE_SYNCHRONIZATION_PROP = "hibernate.cache.infinispan.use_synchronization";

	private static final String NATURAL_ID_KEY = "naturalid";

	/**
	 * Name of the configuration that should be used for natural id caches.
	 *
	 * @see #DEF_ENTITY_RESOURCE
	 */
	@SuppressWarnings("UnusedDeclaration")
	public static final String NATURAL_ID_CACHE_RESOURCE_PROP = PREFIX + NATURAL_ID_KEY + CONFIG_SUFFIX;

	private static final String ENTITY_KEY = "entity";

	/**
	 * Name of the configuration that should be used for entity caches.
	 *
	 * @see #DEF_ENTITY_RESOURCE
	 */
	public static final String ENTITY_CACHE_RESOURCE_PROP = PREFIX + ENTITY_KEY + CONFIG_SUFFIX;

	private static final String IMMUTABLE_ENTITY_KEY = "immutable-entity";

	/**
	 * Name of the configuration that should be used for immutable entity caches.
	 */
	public static final String IMMUTABLE_ENTITY_CACHE_RESOURCE_PROP = PREFIX + IMMUTABLE_ENTITY_KEY + CONFIG_SUFFIX;

	private static final String COLLECTION_KEY = "collection";

	/**
	 * Name of the configuration that should be used for collection caches.
	 * No default value, as by default we try to use the same Infinispan cache
	 * instance we use for entity caching.
	 *
	 * @see #ENTITY_CACHE_RESOURCE_PROP
	 * @see #DEF_ENTITY_RESOURCE
	 */
	@SuppressWarnings("UnusedDeclaration")
	public static final String COLLECTION_CACHE_RESOURCE_PROP = PREFIX + COLLECTION_KEY + CONFIG_SUFFIX;

	private static final String TIMESTAMPS_KEY = "timestamps";

	/**
	 * Name of the configuration that should be used for timestamp caches.
	 *
	 * @see #DEF_TIMESTAMPS_RESOURCE
	 */
	@SuppressWarnings("UnusedDeclaration")
	public static final String TIMESTAMPS_CACHE_RESOURCE_PROP = PREFIX + TIMESTAMPS_KEY + CONFIG_SUFFIX;

	private static final String QUERY_KEY = "query";

	/**
	 * Name of the configuration that should be used for query caches.
	 *
	 * @see #DEF_QUERY_RESOURCE
	 */
	public static final String QUERY_CACHE_RESOURCE_PROP = PREFIX + QUERY_KEY + CONFIG_SUFFIX;

	/**
	 * Default value for {@link #INFINISPAN_CONFIG_RESOURCE_PROP}. Specifies the "infinispan-configs.xml" file in this package.
	 */
	public static final String DEF_INFINISPAN_CONFIG_RESOURCE = "org/hibernate/cache/infinispan/builder/infinispan-configs.xml";

	/**
	 * Default value for {@link #ENTITY_CACHE_RESOURCE_PROP}.
	 */
	public static final String DEF_ENTITY_RESOURCE = "entity";

	/**
	 * Default value for {@link #IMMUTABLE_ENTITY_CACHE_RESOURCE_PROP}.
	 */
	public static final String DEF_IMMUTABLE_ENTITY_RESOURCE = "immutable-entity";

	/**
	 * Default value for {@link #TIMESTAMPS_CACHE_RESOURCE_PROP}.
	 */
	public static final String DEF_TIMESTAMPS_RESOURCE = "timestamps";

	/**
	 * Default value for {@link #QUERY_CACHE_RESOURCE_PROP}.
	 */
	public static final String DEF_QUERY_RESOURCE = "local-query";

	/**
	 * Default value for {@link #INFINISPAN_USE_SYNCHRONIZATION_PROP}.
	 */
	public static final boolean DEF_USE_SYNCHRONIZATION = true;

	/**
	 * Name of the pending puts cache.
	 */
	public static final String PENDING_PUTS_CACHE_NAME = "pending-puts";

	private EmbeddedCacheManager manager;

	private final Map<String, TypeOverrides> typeOverrides = new HashMap<String, TypeOverrides>();

	private final Set<String> definedConfigurations = new HashSet<String>();

	private org.infinispan.transaction.lookup.TransactionManagerLookup transactionManagerlookup;

	private List<String> regionNames = new ArrayList<String>();
	private SessionFactoryOptions settings;

	/**
	 * Create a new instance using the default configuration.
	 */
	public InfinispanRegionFactory() {
	}

	/**
	 * Create a new instance using conifguration properties in <code>props</code>.
	 *
	 * @param props Environmental properties; currently unused.
	 */
	@SuppressWarnings("UnusedParameters")
	public InfinispanRegionFactory(Properties props) {
	}

	@Override
	public CollectionRegion buildCollectionRegion(
			String regionName,
			Properties properties,
			CacheDataDescription metadata) throws CacheException {
		if ( log.isDebugEnabled() ) {
			log.debug( "Building collection cache region [" + regionName + "]" );
		}
		final AdvancedCache cache = getCache( regionName, COLLECTION_KEY, properties, metadata);
		final CollectionRegionImpl region = new CollectionRegionImpl( cache, regionName, metadata, this, buildCacheKeysFactory() );
		startRegion( region, regionName );
		return region;
	}

	@Override
	public EntityRegion buildEntityRegion(String regionName, Properties properties, CacheDataDescription metadata)
			throws CacheException {
		if ( log.isDebugEnabled() ) {
			log.debugf(
					"Building entity cache region [%s] (mutable=%s, versioned=%s)",
					regionName,
					metadata.isMutable(),
					metadata.isVersioned()
			);
		}
		final AdvancedCache cache = getCache( regionName, metadata.isMutable() ? ENTITY_KEY : IMMUTABLE_ENTITY_KEY, properties, metadata );
		final EntityRegionImpl region = new EntityRegionImpl( cache, regionName, metadata, this, buildCacheKeysFactory() );
		startRegion( region, regionName );
		return region;
	}

	@Override
	public NaturalIdRegion buildNaturalIdRegion(String regionName, Properties properties, CacheDataDescription metadata)
			throws CacheException {
		if ( log.isDebugEnabled() ) {
			log.debug("Building natural id cache region [" + regionName + "]");
		}
		final AdvancedCache cache = getCache( regionName, NATURAL_ID_KEY, properties, metadata);
		final NaturalIdRegionImpl region = new NaturalIdRegionImpl( cache, regionName, metadata, this, buildCacheKeysFactory());
		startRegion( region, regionName );
		return region;
	}

	@Override
	public QueryResultsRegion buildQueryResultsRegion(String regionName, Properties properties)
			throws CacheException {
		if ( log.isDebugEnabled() ) {
			log.debug( "Building query results cache region [" + regionName + "]" );
		}
		String cacheName = typeOverrides.get( QUERY_KEY ).getCacheName();
		// If region name is not default one, lookup a cache for that region name
		if ( !regionName.equals( "org.hibernate.cache.internal.StandardQueryCache" ) ) {
			cacheName = regionName;
		}

		final AdvancedCache cache = getCache( cacheName, QUERY_KEY, properties, null);
		final QueryResultsRegionImpl region = new QueryResultsRegionImpl( cache, regionName, this );
		startRegion( region, regionName );
		return region;
	}

	@Override
	public TimestampsRegion buildTimestampsRegion(String regionName, Properties properties)
			throws CacheException {
		if ( log.isDebugEnabled() ) {
			log.debug( "Building timestamps cache region [" + regionName + "]" );
		}
		final AdvancedCache cache = getCache( regionName, TIMESTAMPS_KEY, properties, null);
		final TimestampsRegionImpl region = createTimestampsRegion( cache, regionName );
		startRegion( region, regionName );
		return region;
	}

	protected TimestampsRegionImpl createTimestampsRegion(
			AdvancedCache cache, String regionName) {
		if ( Caches.isClustered(cache) ) {
			return new ClusteredTimestampsRegionImpl( cache, regionName, this );
		}
		else {
			return new TimestampsRegionImpl( cache, regionName, this );
		}
	}

	private CacheKeysFactory buildCacheKeysFactory() {
		if (settings.getMultiTenancyStrategy() != MultiTenancyStrategy.NONE) {
			return DefaultCacheKeysFactory.INSTANCE;
		}
		else {
			return SimpleCacheKeysFactory.INSTANCE;
		}
	}

	@Override
	public boolean isMinimalPutsEnabledByDefault() {
		return true;
	}

	@Override
	public AccessType getDefaultAccessType() {
		return AccessType.TRANSACTIONAL;
	}

	@Override
	public long nextTimestamp() {
		return System.currentTimeMillis() / 100;
	}

	public void setCacheManager(EmbeddedCacheManager manager) {
		this.manager = manager;
	}

	public EmbeddedCacheManager getCacheManager() {
		return manager;
	}

	@Override
	public void start(SessionFactoryOptions settings, Properties properties) throws CacheException {
		log.debug( "Starting Infinispan region factory" );
		try {
			transactionManagerlookup = createTransactionManagerLookup( settings, properties );
			manager = createCacheManager( properties, settings.getServiceRegistry() );
			this.settings = settings;
			initGenericDataTypeOverrides();
			final Enumeration keys = properties.propertyNames();
			while ( keys.hasMoreElements() ) {
				final String key = (String) keys.nextElement();
				int prefixLoc;
				if ( (prefixLoc = key.indexOf( PREFIX )) != -1 ) {
					dissectProperty( prefixLoc, key, properties );
				}
			}
			defineGenericDataTypeCacheConfigurations( properties );
			definePendingPutsCache();
		}
		catch (CacheException ce) {
			throw ce;
		}
		catch (Throwable t) {
			throw new CacheException( "Unable to start region factory", t );
		}
	}

	private void definePendingPutsCache() {
		final ConfigurationBuilder builder = new ConfigurationBuilder();
		// A local, lightweight cache for pending puts, which is
		// non-transactional and has aggressive expiration settings.
		// Locking is still required since the putFromLoad validator
		// code uses conditional operations (i.e. putIfAbsent).
		builder.clustering().cacheMode( CacheMode.LOCAL )
				.transaction().transactionMode( TransactionMode.NON_TRANSACTIONAL )
				.expiration().maxIdle( TimeUnit.SECONDS.toMillis( 60 ) )
				.storeAsBinary().enabled( false )
				.locking().isolationLevel( IsolationLevel.READ_COMMITTED )
				.jmxStatistics().disable();

		manager.defineConfiguration( PENDING_PUTS_CACHE_NAME, builder.build() );
	}

	protected org.infinispan.transaction.lookup.TransactionManagerLookup createTransactionManagerLookup(
			SessionFactoryOptions settings, Properties properties) {
		return new HibernateTransactionManagerLookup( settings, properties );
	}

	@Override
	public void stop() {
		log.debug( "Stop region factory" );
		stopCacheRegions();
		stopCacheManager();
	}

	protected void stopCacheRegions() {
		log.debug( "Clear region references" );
		getCacheCommandFactory( manager.getCache().getAdvancedCache() )
				.clearRegions( regionNames );
		regionNames.clear();
	}

	protected void stopCacheManager() {
		log.debug( "Stop cache manager" );
		manager.stop();
	}

	/**
	 * Returns an unmodifiable map containing configured entity/collection type configuration overrides.
	 * This method should be used primarily for testing/checking purpouses.
	 *
	 * @return an unmodifiable map.
	 */
	public Map<String, TypeOverrides> getTypeOverrides() {
		return Collections.unmodifiableMap( typeOverrides );
	}

	public Set<String> getDefinedConfigurations() {
		return Collections.unmodifiableSet( definedConfigurations );
	}

	protected EmbeddedCacheManager createCacheManager(
			final Properties properties,
			final ServiceRegistry serviceRegistry) throws CacheException {
		final String configLoc = ConfigurationHelper.getString(
				INFINISPAN_CONFIG_RESOURCE_PROP,
				properties,
				DEF_INFINISPAN_CONFIG_RESOURCE
		);
		final FileLookup fileLookup = FileLookupFactory.newInstance();
		//The classloader of the current module:
		final ClassLoader infinispanClassLoader = InfinispanRegionFactory.class.getClassLoader();

		return serviceRegistry.getService( ClassLoaderService.class ).workWithClassLoader(
				new ClassLoaderService.Work<EmbeddedCacheManager>() {
					@Override
					public EmbeddedCacheManager doWork(ClassLoader classLoader) {
						InputStream is = null;
						try {
							is = fileLookup.lookupFile( configLoc, classLoader );
							if ( is == null ) {
								// when it's not a user-provided configuration file, it might be a default configuration file,
								// and if that's included in [this] module might not be visible to the ClassLoaderService:
								classLoader = infinispanClassLoader;
								// This time use lookupFile*Strict* so to provide an exception if we can't find it yet:
								is = FileLookupFactory.newInstance().lookupFileStrict( configLoc, classLoader );
							}
							final ParserRegistry parserRegistry = new ParserRegistry( infinispanClassLoader );
							final ConfigurationBuilderHolder holder = parseWithOverridenClassLoader( parserRegistry, is, infinispanClassLoader );

							// Override global jmx statistics exposure
							final String globalStats = extractProperty(
									INFINISPAN_GLOBAL_STATISTICS_PROP,
									properties
							);
							if ( globalStats != null ) {
								holder.getGlobalConfigurationBuilder()
										.globalJmxStatistics()
										.enabled( Boolean.parseBoolean( globalStats ) );
							}

							return createCacheManager( holder );
						}
						catch (IOException e) {
							throw new CacheException( "Unable to create default cache manager", e );
						}
						finally {
							Util.close( is );
						}
					}

				}
		);
	}

	private static ConfigurationBuilderHolder parseWithOverridenClassLoader(ParserRegistry configurationParser, InputStream is, ClassLoader infinispanClassLoader) {
		// Infinispan requires the context ClassLoader to have full visibility on all
		// its components and eventual extension points even *during* configuration parsing.
		final Thread currentThread = Thread.currentThread();
		final ClassLoader originalContextClassLoader = currentThread.getContextClassLoader();
		try {
			currentThread.setContextClassLoader( infinispanClassLoader );
			ConfigurationBuilderHolder builderHolder = configurationParser.parse( is );
			// Workaround Infinispan's ClassLoader strategies to bend to our will:
			builderHolder.getGlobalConfigurationBuilder().classLoader( infinispanClassLoader );
			return builderHolder;
		}
		finally {
			currentThread.setContextClassLoader( originalContextClassLoader );
		}
	}

	protected EmbeddedCacheManager createCacheManager(ConfigurationBuilderHolder holder) {
		return new DefaultCacheManager( holder, true );
	}

	private void startRegion(BaseRegion region, String regionName) {
		regionNames.add( regionName );
		getCacheCommandFactory( region.getCache() ).addRegion( regionName, region );
	}

	private Map<String, TypeOverrides> initGenericDataTypeOverrides() {
		final TypeOverrides entityOverrides = new TypeOverrides();
		entityOverrides.setCacheName( DEF_ENTITY_RESOURCE );
		typeOverrides.put( ENTITY_KEY, entityOverrides );
		final TypeOverrides immutableEntityOverrides = new TypeOverrides();
		immutableEntityOverrides.setCacheName( DEF_IMMUTABLE_ENTITY_RESOURCE );
		typeOverrides.put( IMMUTABLE_ENTITY_KEY, immutableEntityOverrides );
		final TypeOverrides collectionOverrides = new TypeOverrides();
		collectionOverrides.setCacheName( DEF_ENTITY_RESOURCE );
		typeOverrides.put( COLLECTION_KEY, collectionOverrides );
		final TypeOverrides naturalIdOverrides = new TypeOverrides();
		naturalIdOverrides.setCacheName( DEF_ENTITY_RESOURCE );
		typeOverrides.put( NATURAL_ID_KEY, naturalIdOverrides );
		final TypeOverrides timestampOverrides = new TimestampTypeOverrides();
		timestampOverrides.setCacheName( DEF_TIMESTAMPS_RESOURCE );
		typeOverrides.put( TIMESTAMPS_KEY, timestampOverrides );
		final TypeOverrides queryOverrides = new TypeOverrides();
		queryOverrides.setCacheName( DEF_QUERY_RESOURCE );
		typeOverrides.put( QUERY_KEY, queryOverrides );
		return typeOverrides;
	}

	private void dissectProperty(int prefixLoc, String key, Properties properties) {
		final TypeOverrides cfgOverride;
		int suffixLoc;
		if ( !key.equals( INFINISPAN_CONFIG_RESOURCE_PROP ) && (suffixLoc = key.indexOf( CONFIG_SUFFIX )) != -1 ) {
			cfgOverride = getOrCreateConfig( prefixLoc, key, suffixLoc );
			cfgOverride.setCacheName( extractProperty( key, properties ) );
		}
		else if ( (suffixLoc = key.indexOf( STRATEGY_SUFFIX )) != -1 ) {
			cfgOverride = getOrCreateConfig( prefixLoc, key, suffixLoc );
			cfgOverride.setEvictionStrategy( extractProperty( key, properties ) );
		}
		else if ( (suffixLoc = key.indexOf( WAKE_UP_INTERVAL_SUFFIX )) != -1 ) {
			cfgOverride = getOrCreateConfig( prefixLoc, key, suffixLoc );
			cfgOverride.setEvictionWakeUpInterval( Long.parseLong( extractProperty( key, properties ) ) );
		}
		else if ( (suffixLoc = key.indexOf( MAX_ENTRIES_SUFFIX )) != -1 ) {
			cfgOverride = getOrCreateConfig( prefixLoc, key, suffixLoc );
			cfgOverride.setEvictionMaxEntries( Integer.parseInt( extractProperty( key, properties ) ) );
		}
		else if ( (suffixLoc = key.indexOf( LIFESPAN_SUFFIX )) != -1 ) {
			cfgOverride = getOrCreateConfig( prefixLoc, key, suffixLoc );
			cfgOverride.setExpirationLifespan( Long.parseLong( extractProperty( key, properties ) ) );
		}
		else if ( (suffixLoc = key.indexOf( MAX_IDLE_SUFFIX )) != -1 ) {
			cfgOverride = getOrCreateConfig( prefixLoc, key, suffixLoc );
			cfgOverride.setExpirationMaxIdle( Long.parseLong( extractProperty( key, properties ) ) );
		}
	}

	private String extractProperty(String key, Properties properties) {
		final String value = ConfigurationHelper.extractPropertyValue( key, properties );
		log.debugf( "Configuration override via property %s: %s", key, value );
		return value;
	}

	private TypeOverrides getOrCreateConfig(int prefixLoc, String key, int suffixLoc) {
		final String name = key.substring( prefixLoc + PREFIX.length(), suffixLoc );
		TypeOverrides cfgOverride = typeOverrides.get( name );
		if ( cfgOverride == null ) {
			cfgOverride = new TypeOverrides();
			typeOverrides.put( name, cfgOverride );
		}
		return cfgOverride;
	}

	private void defineGenericDataTypeCacheConfigurations(Properties properties) {
		final String[] defaultGenericDataTypes = new String[] {ENTITY_KEY, IMMUTABLE_ENTITY_KEY, COLLECTION_KEY, TIMESTAMPS_KEY, QUERY_KEY};
		for ( String type : defaultGenericDataTypes ) {
			final TypeOverrides override = overrideStatisticsIfPresent( typeOverrides.get( type ), properties );
			final String cacheName = override.getCacheName();
			final ConfigurationBuilder builder = new ConfigurationBuilder();
			// Read base configuration
			applyConfiguration( cacheName, builder );

			// Apply overrides
			override.applyTo( builder );
			// Configure transaction manager
			configureTransactionManager( builder, cacheName, properties );
			// Define configuration, validate and then apply
			final Configuration cfg = builder.build();
			override.validateInfinispanConfiguration( cfg );
			manager.defineConfiguration( cacheName, cfg );
			definedConfigurations.add( cacheName );
		}
	}

	private AdvancedCache getCache(String regionName, String typeKey, Properties properties, CacheDataDescription metadata) {
		TypeOverrides regionOverride = typeOverrides.get( regionName );
		if ( !definedConfigurations.contains( regionName ) ) {
			final String templateCacheName;
			final ConfigurationBuilder builder = new ConfigurationBuilder();
			if ( regionOverride != null ) {
				if ( log.isDebugEnabled() ) {
					log.debug( "Cache region specific configuration exists: " + regionOverride );
				}
				final String cacheName = regionOverride.getCacheName();
				if ( cacheName != null ) {
					// Region specific override with a given cache name
					templateCacheName = cacheName;
				}
				else {
					// Region specific override without cache name, so template cache name is generic for data type.
					templateCacheName = typeOverrides.get( typeKey ).getCacheName();
				}

				// Read template configuration
				applyConfiguration( templateCacheName, builder );

				regionOverride = overrideStatisticsIfPresent( regionOverride, properties );
				regionOverride.applyTo( builder );

			}
			else {
				// No region specific overrides, template cache name is generic for data type.
				templateCacheName = typeOverrides.get( typeKey ).getCacheName();
				// Read template configuration
				builder.read( manager.getCacheConfiguration( templateCacheName ) );
				// Apply overrides
				typeOverrides.get( typeKey ).applyTo( builder );
			}
			// with multi-tenancy the keys will be wrapped
			if (settings.getMultiTenancyStrategy() == MultiTenancyStrategy.NONE) {
				// the keys may not define hashCode/equals correctly (e.g. arrays)
				if (metadata != null && metadata.getKeyType() != null) {
					builder.dataContainer().keyEquivalence(new TypeEquivalance(metadata.getKeyType()));
				}
			}
			// Configure transaction manager
			configureTransactionManager( builder, templateCacheName, properties );
			// Define configuration
			manager.defineConfiguration( regionName, builder.build() );
			definedConfigurations.add( regionName );
		}
		final AdvancedCache cache = manager.getCache( regionName ).getAdvancedCache();
		if ( !cache.getStatus().allowInvocations() ) {
			cache.start();
		}
		return createCacheWrapper( cache );
	}

	private void applyConfiguration(String cacheName, ConfigurationBuilder builder) {
		final Configuration cfg = manager.getCacheConfiguration( cacheName );
		if ( cfg != null ) {
			builder.read( cfg );
		}
	}

	private CacheCommandFactory getCacheCommandFactory(AdvancedCache cache) {
		final GlobalComponentRegistry globalCr = cache.getComponentRegistry().getGlobalComponentRegistry();

		final Map<Byte, ModuleCommandFactory> factories =
				(Map<Byte, ModuleCommandFactory>) globalCr.getComponent( "org.infinispan.modules.command.factories" );

		for ( ModuleCommandFactory factory : factories.values() ) {
			if ( factory instanceof CacheCommandFactory ) {
				return (CacheCommandFactory) factory;
			}
		}

		throw new CacheException(
				"Infinispan custom cache command factory not " +
						"installed (possibly because the classloader where Infinispan " +
						"lives couldn't find the Hibernate Infinispan cache provider)"
		);
	}

	protected AdvancedCache createCacheWrapper(AdvancedCache cache) {
		return cache;
	}

	private void configureTransactionManager(
			ConfigurationBuilder builder,
			String cacheName,
			Properties properties) {
		// Get existing configuration to verify whether a tm was configured or not.
		final Configuration baseCfg = manager.getCacheConfiguration( cacheName );
		if ( baseCfg != null && baseCfg.transaction().transactionMode().isTransactional() ) {
			final String ispnTmLookupClassName = baseCfg.transaction().transactionManagerLookup().getClass().getName();
			final String hbTmLookupClassName = org.hibernate.cache.infinispan.tm.HibernateTransactionManagerLookup.class.getName();
			if ( GenericTransactionManagerLookup.class.getName().equals( ispnTmLookupClassName ) ) {
				log.debug(
						"Using default Infinispan transaction manager lookup " +
								"instance (GenericTransactionManagerLookup), overriding it " +
								"with Hibernate transaction manager lookup"
				);
				builder.transaction().transactionManagerLookup( transactionManagerlookup );
			}
			else if ( ispnTmLookupClassName != null && !ispnTmLookupClassName.equals( hbTmLookupClassName ) ) {
				log.debug(
						"Infinispan is configured [" + ispnTmLookupClassName + "] with a different transaction manager lookup " +
								"class than Hibernate [" + hbTmLookupClassName + "]"
				);
			}
			else {
				// Infinispan TM lookup class null, so apply Hibernate one directly
				builder.transaction().transactionManagerLookup( transactionManagerlookup );
			}

			final String useSyncProp = extractProperty( INFINISPAN_USE_SYNCHRONIZATION_PROP, properties );
			final boolean useSync = useSyncProp == null ? DEF_USE_SYNCHRONIZATION : Boolean.parseBoolean( useSyncProp );
			builder.transaction().useSynchronization( useSync );
		}
	}

	private TypeOverrides overrideStatisticsIfPresent(TypeOverrides override, Properties properties) {
		final String globalStats = extractProperty( INFINISPAN_GLOBAL_STATISTICS_PROP, properties );
		if ( globalStats != null ) {
			override.setExposeStatistics( Boolean.parseBoolean( globalStats ) );
		}
		return override;
	}
}
