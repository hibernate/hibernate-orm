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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.collection.CollectionRegionImpl;
import org.hibernate.cache.infinispan.entity.EntityRegionImpl;
import org.hibernate.cache.infinispan.impl.BaseRegion;
import org.hibernate.cache.infinispan.naturalid.NaturalIdRegionImpl;
import org.hibernate.cache.infinispan.query.QueryResultsRegionImpl;
import org.hibernate.cache.infinispan.timestamp.ClusteredTimestampsRegionImpl;
import org.hibernate.cache.infinispan.timestamp.TimestampsRegionImpl;
import org.hibernate.cache.infinispan.tm.HibernateTransactionManagerLookup;
import org.hibernate.cache.infinispan.util.CacheCommandFactory;
import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.cache.infinispan.util.InfinispanMessageLogger;
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
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;

import org.infinispan.AdvancedCache;
import org.infinispan.commands.module.ModuleCommandFactory;
import org.infinispan.commons.util.FileLookup;
import org.infinispan.commons.util.FileLookupFactory;
import org.infinispan.commons.util.Util;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.TransactionConfiguration;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.transaction.lookup.GenericTransactionManagerLookup;

import javax.transaction.TransactionManager;

/**
 * A {@link RegionFactory} for <a href="http://www.jboss.org/infinispan">Infinispan</a>-backed cache
 * regions.
 *
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
public class InfinispanRegionFactory implements RegionFactory {
	private static final InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog( InfinispanRegionFactory.class );

	private static final String PREFIX = "hibernate.cache.infinispan.";

	private static final String CONFIG_SUFFIX = ".cfg";

	private static final String STRATEGY_SUFFIX = ".eviction.strategy";

	// The attribute was incorrectly named; in fact this sets expiration check interval
	// (eviction is triggered by writes, expiration is time-based)
	private static final String DEPRECATED_WAKE_UP_INTERVAL_SUFFIX = ".eviction.wake_up_interval";

	private static final String MAX_ENTRIES_SUFFIX = ".eviction.max_entries";

	private static final String WAKE_UP_INTERVAL_SUFFIX = ".expiration.wake_up_interval";

	private static final String LIFESPAN_SUFFIX = ".expiration.lifespan";

	private static final String MAX_IDLE_SUFFIX = ".expiration.max_idle";

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
	 * an XA resource.
	 * @deprecated Infinispan Second Level Cache is designed to always register as synchronization
	 *             on transactional caches, or use non-transactional caches.
	 *
	 * @see #DEF_USE_SYNCHRONIZATION
	 */
	@Deprecated
	public static final String INFINISPAN_USE_SYNCHRONIZATION_PROP = "hibernate.cache.infinispan.use_synchronization";

	private static final Consumer<Configuration> NO_VALIDATION = c -> {};

	public enum DataType {
		ENTITY("entity", DEF_ENTITY_RESOURCE, NO_VALIDATION),
		NATURAL_ID("naturalid", DEF_ENTITY_RESOURCE, NO_VALIDATION),
		COLLECTION("collection", DEF_ENTITY_RESOURCE, NO_VALIDATION),
		IMMUTABLE_ENTITY("immutable-entity", DEF_ENTITY_RESOURCE, NO_VALIDATION),
		TIMESTAMPS("timestamps", DEF_TIMESTAMPS_RESOURCE, c -> {
			if ( c.clustering().cacheMode().isInvalidation() ) {
				throw log.timestampsMustNotUseInvalidation();
			}
			if (c.eviction().strategy().isEnabled()) {
				throw log.timestampsMustNotUseEviction();
			}
		}),
		QUERY("query", DEF_QUERY_RESOURCE, NO_VALIDATION),
		PENDING_PUTS("pending-puts", DEF_PENDING_PUTS_RESOURCE, c -> {
			if (!c.isTemplate()) {
				log.pendingPutsShouldBeTemplate();
			}
			if (c.clustering().cacheMode().isClustered()) {
				throw log.pendingPutsMustNotBeClustered();
			}
			if (c.transaction().transactionMode().isTransactional()) {
				throw log.pendingPutsMustNotBeTransactional();
			}
			if (c.expiration().maxIdle() <= 0) {
				throw log.pendingPutsMustHaveMaxIdle();
			}
		});

		public final String key;
		private final String defaultCacheName;
		private final Consumer<Configuration> validation;

		DataType(String key, String defaultCacheName, Consumer<Configuration> validation) {
			this.key = key;
			this.defaultCacheName = defaultCacheName;
			this.validation = validation;
		}

		public void validate(Configuration configuration) {
			validation.accept(configuration);
		}
	}

	/**
	 * Name of the configuration that should be used for natural id caches.
	 *
	 * @see #DEF_ENTITY_RESOURCE
	 */
	@SuppressWarnings("UnusedDeclaration")
	public static final String NATURAL_ID_CACHE_RESOURCE_PROP = PREFIX + DataType.NATURAL_ID.key + CONFIG_SUFFIX;

	/**
	 * Name of the configuration that should be used for entity caches.
	 *
	 * @see #DEF_ENTITY_RESOURCE
	 */
	@SuppressWarnings("UnusedDeclaration")
	public static final String ENTITY_CACHE_RESOURCE_PROP = PREFIX + DataType.ENTITY.key + CONFIG_SUFFIX;

	/**
	 * Name of the configuration that should be used for immutable entity caches.
	 * Defaults to the same configuration as {@link #ENTITY_CACHE_RESOURCE_PROP} - {@link #DEF_ENTITY_RESOURCE}
	 */
	@SuppressWarnings("UnusedDeclaration")
	public static final String IMMUTABLE_ENTITY_CACHE_RESOURCE_PROP = PREFIX + DataType.IMMUTABLE_ENTITY.key + CONFIG_SUFFIX;

	/**
	 * Name of the configuration that should be used for collection caches.
	 * No default value, as by default we try to use the same Infinispan cache
	 * instance we use for entity caching.
	 *
	 * @see #ENTITY_CACHE_RESOURCE_PROP
	 * @see #DEF_ENTITY_RESOURCE
	 */
	@SuppressWarnings("UnusedDeclaration")
	public static final String COLLECTION_CACHE_RESOURCE_PROP = PREFIX + DataType.COLLECTION.key + CONFIG_SUFFIX;

	/**
	 * Name of the configuration that should be used for timestamp caches.
	 *
	 * @see #DEF_TIMESTAMPS_RESOURCE
	 */
	@SuppressWarnings("UnusedDeclaration")
	public static final String TIMESTAMPS_CACHE_RESOURCE_PROP = PREFIX + DataType.TIMESTAMPS.key + CONFIG_SUFFIX;

	/**
	 * Name of the configuration that should be used for query caches.
	 *
	 * @see #DEF_QUERY_RESOURCE
	 */
	public static final String QUERY_CACHE_RESOURCE_PROP = PREFIX + DataType.QUERY.key + CONFIG_SUFFIX;

	/**
	 * Name of the configuration that should be used for pending-puts caches.
	 *
	 * @see #DEF_PENDING_PUTS_RESOURCE
	 */
	@SuppressWarnings("UnusedDeclaration")
	public static final String PENDING_PUTS_CACHE_RESOURCE_PROP = PREFIX + DataType.PENDING_PUTS.key + CONFIG_SUFFIX;

	/**
	 * Default value for {@link #INFINISPAN_CONFIG_RESOURCE_PROP}. Specifies the "infinispan-configs.xml" file in this package.
	 */
	public static final String DEF_INFINISPAN_CONFIG_RESOURCE = "org/hibernate/cache/infinispan/builder/infinispan-configs.xml";

	/**
	 * Default configuration for cases where non-clustered cache manager is provided.
	 */
	public static final String INFINISPAN_CONFIG_LOCAL_RESOURCE = "org/hibernate/cache/infinispan/builder/infinispan-configs-local.xml";

	/**
	 * Default value for {@link #ENTITY_CACHE_RESOURCE_PROP}.
	 */
	public static final String DEF_ENTITY_RESOURCE = "entity";

	/**
	 * Default value for {@link #TIMESTAMPS_CACHE_RESOURCE_PROP}.
	 */
	public static final String DEF_TIMESTAMPS_RESOURCE = "timestamps";

	/**
	 * Default value for {@link #QUERY_CACHE_RESOURCE_PROP}.
	 */
	public static final String DEF_QUERY_RESOURCE = "local-query";

	/**
	 * Default value for {@link #PENDING_PUTS_CACHE_RESOURCE_PROP}
	 */
	public static final String DEF_PENDING_PUTS_RESOURCE = "pending-puts";

	/**
	 * @deprecated Use {@link #DEF_PENDING_PUTS_RESOURCE} instead.
	 */
	@Deprecated
	public static final String PENDING_PUTS_CACHE_NAME = DEF_PENDING_PUTS_RESOURCE;

	/**
	 * Default value for {@link #INFINISPAN_USE_SYNCHRONIZATION_PROP}.
	 */
	public static final boolean DEF_USE_SYNCHRONIZATION = true;

	/**
	 * Defines custom mapping for regionName -> cacheName and also DataType.key -> cacheName
	 * (for the case that you want to change the cache configuration for whole type)
	 */
	protected final Map<String, String> baseConfigurations = new HashMap<>();
	/**
	 * Defines configuration properties applied on top of configuration set in any file, by regionName or DataType.key
	 */
	protected final Map<String, ConfigurationBuilder> configOverrides = new HashMap<>();

	private CacheKeysFactory cacheKeysFactory;
	private ConfigurationBuilderHolder defaultConfiguration;
	private final Map<DataType, Configuration> dataTypeConfigurations = new HashMap<>();
	private EmbeddedCacheManager manager;

	private org.infinispan.transaction.lookup.TransactionManagerLookup transactionManagerlookup;
	private TransactionManager transactionManager;

	private List<BaseRegion> regions = new ArrayList<>();
	private SessionFactoryOptions settings;

	private Boolean globalStats;

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
	public CollectionRegion buildCollectionRegion(String regionName, Map<String, Object> configValues, CacheDataDescription metadata) {
		if ( log.isDebugEnabled() ) {
			log.debug( "Building collection cache region [" + regionName + "]" );
		}
		final AdvancedCache cache = getCache( regionName, DataType.COLLECTION, metadata);
		final CollectionRegionImpl region = new CollectionRegionImpl( cache, regionName, transactionManager, metadata, this, getCacheKeysFactory() );
		startRegion( region );
		return region;
	}

	@Override
	@SuppressWarnings("unchecked")
	public CollectionRegion buildCollectionRegion(
			String regionName,
			Properties properties,
			CacheDataDescription metadata) throws CacheException {
		return buildCollectionRegion( regionName, (Map) properties, metadata );
	}

	@Override
	public EntityRegion buildEntityRegion(String regionName, Map<String, Object> configValues, CacheDataDescription metadata) {
		if ( log.isDebugEnabled() ) {
			log.debugf(
					"Building entity cache region [%s] (mutable=%s, versioned=%s)",
					regionName,
					metadata.isMutable(),
					metadata.isVersioned()
			);
		}
		final AdvancedCache cache = getCache( regionName, metadata.isMutable() ? DataType.ENTITY : DataType.IMMUTABLE_ENTITY, metadata );
		final EntityRegionImpl region = new EntityRegionImpl( cache, regionName, transactionManager, metadata, this, getCacheKeysFactory() );
		startRegion( region );
		return region;
	}

	@Override
	@SuppressWarnings("unchecked")
	public EntityRegion buildEntityRegion(String regionName, Properties properties, CacheDataDescription metadata) {
		return buildEntityRegion( regionName, (Map) properties, metadata );
	}

	@Override
	public NaturalIdRegion buildNaturalIdRegion(String regionName, Map<String, Object> configValues, CacheDataDescription metadata) {
		if ( log.isDebugEnabled() ) {
			log.debug("Building natural id cache region [" + regionName + "]");
		}
		final AdvancedCache cache = getCache( regionName, DataType.NATURAL_ID, metadata);
		final NaturalIdRegionImpl region = new NaturalIdRegionImpl( cache, regionName, transactionManager, metadata, this, getCacheKeysFactory());
		startRegion( region );
		return region;
	}

	@Override
	@SuppressWarnings("unchecked")
	public NaturalIdRegion buildNaturalIdRegion(String regionName, Properties properties, CacheDataDescription metadata) {
		return buildNaturalIdRegion( regionName, (Map) properties, metadata );
	}

	@Override
	public QueryResultsRegion buildQueryResultsRegion(String regionName, Map<String, Object> configValues) {
		if ( log.isDebugEnabled() ) {
			log.debug( "Building query results cache region [" + regionName + "]" );
		}

		final AdvancedCache cache = getCache( regionName, DataType.QUERY, null);
		final QueryResultsRegionImpl region = new QueryResultsRegionImpl( cache, regionName, transactionManager, this );
		startRegion( region );
		return region;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryResultsRegion buildQueryResultsRegion(String regionName, Properties properties) {
		return buildQueryResultsRegion( regionName, (Map) properties );
	}

	@Override
	public TimestampsRegion buildTimestampsRegion(String regionName, Map<String, Object> configValues) {
		if ( log.isDebugEnabled() ) {
			log.debug( "Building timestamps cache region [" + regionName + "]" );
		}
		final AdvancedCache cache = getCache( regionName, DataType.TIMESTAMPS, null);
		final TimestampsRegionImpl region = createTimestampsRegion( cache, regionName );
		startRegion( region );
		return region;
	}

	@Override
	@SuppressWarnings("unchecked")
	public TimestampsRegion buildTimestampsRegion(String regionName, Properties properties) {
		return buildTimestampsRegion( regionName, (Map) properties );
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

	public Configuration getPendingPutsCacheConfiguration() {
		return dataTypeConfigurations.get(DataType.PENDING_PUTS);
	}

	private CacheKeysFactory getCacheKeysFactory() {
		return cacheKeysFactory;
	}

	@Override
	public boolean isMinimalPutsEnabledByDefault() {
		// TODO: change to false
		return true;
	}

	@Override
	public AccessType getDefaultAccessType() {
		return AccessType.TRANSACTIONAL;
	}

	@Override
	public long nextTimestamp() {
		return System.currentTimeMillis();
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

		// determine the CacheKeysFactory to use...
		this.cacheKeysFactory = determineCacheKeysFactory( settings, properties );

		try {
			this.settings = settings;
			transactionManagerlookup = createTransactionManagerLookup( settings, properties );
			transactionManager = transactionManagerlookup.getTransactionManager();

			final Enumeration keys = properties.propertyNames();
			while ( keys.hasMoreElements() ) {
				final String key = (String) keys.nextElement();
				int prefixLoc;
				if ( (prefixLoc = key.indexOf( PREFIX )) != -1 ) {
					parseProperty( prefixLoc, key, extractProperty(key, properties));
				}
			}

			defaultConfiguration = loadConfiguration(settings.getServiceRegistry(), DEF_INFINISPAN_CONFIG_RESOURCE);
			manager = createCacheManager(properties, settings.getServiceRegistry());
			if (!manager.getCacheManagerConfiguration().isClustered()) {
				// If we got non-clustered cache manager, use non-clustered (local) configuration as defaults
				// for the data types
				defaultConfiguration = loadConfiguration(settings.getServiceRegistry(), INFINISPAN_CONFIG_LOCAL_RESOURCE);
			}
			defineDataTypeCacheConfigurations();
		}
		catch (CacheException ce) {
			throw ce;
		}
		catch (Throwable t) {
			throw log.unableToStart(t);
		}
	}

	private CacheKeysFactory determineCacheKeysFactory(SessionFactoryOptions settings, Properties properties) {
		return settings.getServiceRegistry().getService( StrategySelector.class ).resolveDefaultableStrategy(
				CacheKeysFactory.class,
				properties.get( AvailableSettings.CACHE_KEYS_FACTORY ),
				DefaultCacheKeysFactory.INSTANCE
		);
	}

	/* This method is overridden in WildFly, so the signature must not change. */
	/* In WF, the global configuration setting is ignored */
	protected EmbeddedCacheManager createCacheManager(Properties properties, ServiceRegistry serviceRegistry) {
		if (properties.containsKey(INFINISPAN_USE_SYNCHRONIZATION_PROP)) {
			log.propertyUseSynchronizationDeprecated();
		}
		ConfigurationBuilderHolder cfgHolder;
		String configFile = ConfigurationHelper.extractPropertyValue(INFINISPAN_CONFIG_RESOURCE_PROP, properties);
		if (configFile != null) {
			cfgHolder = loadConfiguration(serviceRegistry, configFile);
		}
		else {
			cfgHolder = defaultConfiguration;
		}

		// We cannot just add the default configurations not defined in provided configuration
		// since WF overrides this method - we have to deal with missing configuration for each cache separately
		String globalStatsStr = extractProperty( INFINISPAN_GLOBAL_STATISTICS_PROP, properties	);
		if ( globalStatsStr != null ) {
			globalStats = Boolean.parseBoolean(globalStatsStr);
		}
		if (globalStats != null) {
			cfgHolder.getGlobalConfigurationBuilder().globalJmxStatistics().enabled(globalStats);
		}

		return createCacheManager(cfgHolder);
	}

	protected EmbeddedCacheManager createCacheManager(ConfigurationBuilderHolder cfgHolder) {
		return new DefaultCacheManager( cfgHolder, true );
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
		getCacheCommandFactory().clearRegions( regions );
		// Ensure we cleanup any caches we created
		regions.forEach( region -> {
			region.getCache().stop();
			manager.undefineConfiguration( region.getCache().getName() );
		} );
		regions.clear();
	}

	protected void stopCacheManager() {
		log.debug( "Stop cache manager" );
		manager.stop();
	}

	private ConfigurationBuilderHolder loadConfiguration(ServiceRegistry serviceRegistry, String configFile) {
		final FileLookup fileLookup = FileLookupFactory.newInstance();
		final ClassLoader infinispanClassLoader = InfinispanRegionFactory.class.getClassLoader();
		return serviceRegistry.getService( ClassLoaderService.class ).workWithClassLoader(
				new ClassLoaderService.Work<ConfigurationBuilderHolder>() {
					@Override
					public ConfigurationBuilderHolder doWork(ClassLoader classLoader) {
						InputStream is = null;
						try {
							is = fileLookup.lookupFile(configFile, classLoader );
							if ( is == null ) {
								// when it's not a user-provided configuration file, it might be a default configuration file,
								// and if that's included in [this] module might not be visible to the ClassLoaderService:
								classLoader = infinispanClassLoader;
								// This time use lookupFile*Strict* so to provide an exception if we can't find it yet:
								is = FileLookupFactory.newInstance().lookupFileStrict(configFile, classLoader );
							}
							final ParserRegistry parserRegistry = new ParserRegistry( infinispanClassLoader );
							final ConfigurationBuilderHolder holder = parseWithOverridenClassLoader( parserRegistry, is, infinispanClassLoader );

							return holder;
						}
						catch (IOException e) {
							throw log.unableToCreateCacheManager(e);
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

	private void startRegion(BaseRegion region) {
		regions.add( region );
		getCacheCommandFactory().addRegion( region );
	}

	private void parseProperty(int prefixLoc, String key, String value) {
		final ConfigurationBuilder builder;
		int suffixLoc;
		if ( (suffixLoc = key.indexOf( CONFIG_SUFFIX )) != -1 && !key.equals( INFINISPAN_CONFIG_RESOURCE_PROP )) {
			String regionName = key.substring( prefixLoc + PREFIX.length(), suffixLoc );
			baseConfigurations.put(regionName, value);
		}
		else if ( (suffixLoc = key.indexOf( STRATEGY_SUFFIX )) != -1 ) {
			builder = getOrCreateConfig( prefixLoc, key, suffixLoc );
			builder.eviction().strategy( EvictionStrategy.valueOf(value) );
		}
		else if ( (suffixLoc = key.indexOf( WAKE_UP_INTERVAL_SUFFIX )) != -1
				|| (suffixLoc = key.indexOf(DEPRECATED_WAKE_UP_INTERVAL_SUFFIX)) != -1 ) {
			builder = getOrCreateConfig( prefixLoc, key, suffixLoc );
			builder.expiration().wakeUpInterval( Long.parseLong(value) );
		}
		else if ( (suffixLoc = key.indexOf( MAX_ENTRIES_SUFFIX )) != -1 ) {
			builder = getOrCreateConfig( prefixLoc, key, suffixLoc );
			builder.eviction().size( Long.parseLong(value) );
		}
		else if ( (suffixLoc = key.indexOf( LIFESPAN_SUFFIX )) != -1 ) {
			builder = getOrCreateConfig( prefixLoc, key, suffixLoc );
			builder.expiration().lifespan( Long.parseLong(value) );
		}
		else if ( (suffixLoc = key.indexOf( MAX_IDLE_SUFFIX )) != -1 ) {
			builder = getOrCreateConfig( prefixLoc, key, suffixLoc );
			builder.expiration().maxIdle( Long.parseLong(value) );
		}
	}

	private String extractProperty(String key, Properties properties) {
		final String value = ConfigurationHelper.extractPropertyValue( key, properties );
		log.debugf( "Configuration override via property %s: %s", key, value );
		return value;
	}

	private ConfigurationBuilder getOrCreateConfig(int prefixLoc, String key, int suffixLoc) {
		final String name = key.substring( prefixLoc + PREFIX.length(), suffixLoc );
		ConfigurationBuilder builder = configOverrides.get( name );
		if ( builder == null ) {
			builder = new ConfigurationBuilder();
			configOverrides.put( name, builder );
		}
		return builder;
	}

	private void defineDataTypeCacheConfigurations() {
		for ( DataType type : DataType.values() ) {
			String cacheName = baseConfigurations.get(type.key);
			if (cacheName == null) {
				cacheName = type.defaultCacheName;
			}
			Configuration configuration = manager.getCacheConfiguration(cacheName);
			ConfigurationBuilder builder;
			if (configuration == null) {
				log.debugf("Cache configuration not found for %s", type);
				if (!cacheName.equals(type.defaultCacheName)) {
					log.customConfigForTypeNotFound(cacheName, type.key);
				}
				builder = defaultConfiguration.getNamedConfigurationBuilders().get(type.defaultCacheName);
				if (builder == null) {
					throw new IllegalStateException("Generic data types must have default configuration, none found for " + type);
				}
			}
			else {
				builder = new ConfigurationBuilder().read(configuration);
			}
			ConfigurationBuilder override = configOverrides.get( type.key );
			if (override != null) {
				builder.read(override.build(false));
			}
			builder.template(true);
			configureTransactionManager( builder );
			dataTypeConfigurations.put(type, builder.build());
		}
	}

	protected AdvancedCache getCache(String regionName, DataType type, CacheDataDescription metadata) {
		if (!manager.cacheExists(regionName)) {
			String templateCacheName = baseConfigurations.get(regionName);
			Configuration configuration = null;
			ConfigurationBuilder builder = new ConfigurationBuilder();
			if (templateCacheName != null) {
				configuration = manager.getCacheConfiguration(templateCacheName);
				if (configuration == null) {
					log.customConfigForRegionNotFound(templateCacheName, regionName, type.key);
				}
				else {
					log.debugf("Region '%s' will use cache template '%s'", regionName, templateCacheName);
					builder.read(configuration);
					configureTransactionManager(builder);
					// do not apply data type overrides to regions that set special cache configuration
				}
			}
			if (configuration == null) {
				configuration = dataTypeConfigurations.get(type);
				if (configuration == null) {
					throw new IllegalStateException("Configuration not defined for type " + type.key);
				}
				builder.read(configuration);
				// overrides for data types are already applied, but we should check custom ones
			}
			ConfigurationBuilder override = configOverrides.get(regionName);
			if (override != null) {
				log.debugf("Region '%s' has additional configuration set through properties.", regionName);
				builder.read(override.build(false));
			}
			if (getCacheKeysFactory() instanceof SimpleCacheKeysFactory) {
				// the keys may not define hashCode/equals correctly (e.g. arrays)
				if (metadata != null && metadata.getKeyType() != null) {
					builder.dataContainer().keyEquivalence(new TypeEquivalance(metadata.getKeyType()));
				}
			}
			if (globalStats != null) {
				builder.jmxStatistics().enabled(globalStats).available(globalStats);
			}
			configuration = builder.build();
			type.validate(configuration);
			manager.defineConfiguration(regionName, configuration);
		}
		final AdvancedCache cache = manager.getCache( regionName ).getAdvancedCache();
		// TODO: not sure if this is needed in recent Infinispan
		if ( !cache.getStatus().allowInvocations() ) {
			cache.start();
		}
		return createCacheWrapper( cache );
	}

	private CacheCommandFactory getCacheCommandFactory() {
		final GlobalComponentRegistry globalCr = manager.getGlobalComponentRegistry();

		final Map<Byte, ModuleCommandFactory> factories =
				(Map<Byte, ModuleCommandFactory>) globalCr.getComponent( "org.infinispan.modules.command.factories" );

		for ( ModuleCommandFactory factory : factories.values() ) {
			if ( factory instanceof CacheCommandFactory ) {
				return (CacheCommandFactory) factory;
			}
		}

		throw log.cannotInstallCommandFactory();
	}

	protected AdvancedCache createCacheWrapper(AdvancedCache cache) {
		return cache;
	}

	private void configureTransactionManager(ConfigurationBuilder builder) {
		TransactionConfiguration transaction = builder.transaction().create();
		if (transaction.transactionMode().isTransactional() ) {
			final String ispnTmLookupClassName = transaction.transactionManagerLookup().getClass().getName();
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
			builder.transaction().useSynchronization( DEF_USE_SYNCHRONIZATION );
		}
	}
}
