package org.hibernate.cache.infinispan;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.transaction.TransactionManager;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CollectionRegion;
import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.QueryResultsRegion;
import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.TimestampsRegion;
import org.hibernate.cache.access.AccessType;
import org.hibernate.cache.infinispan.collection.CollectionRegionImpl;
import org.hibernate.cache.infinispan.entity.EntityRegionImpl;
import org.hibernate.cache.infinispan.query.QueryResultsRegionImpl;
import org.hibernate.cache.infinispan.timestamp.TimestampsRegionImpl;
import org.hibernate.cache.infinispan.timestamp.TimestampTypeOverrides;
import org.hibernate.cache.infinispan.tm.HibernateTransactionManagerLookup;
import org.hibernate.cache.infinispan.util.CacheAdapter;
import org.hibernate.cache.infinispan.util.CacheAdapterImpl;
import org.hibernate.cfg.Settings;
import org.hibernate.util.PropertiesHelper;
import org.infinispan.Cache;
import org.infinispan.config.Configuration;
import org.infinispan.manager.CacheManager;
import org.infinispan.manager.DefaultCacheManager;
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

   private static final Log log = LogFactory.getLog(InfinispanRegionFactory.class);

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

   public static final String INFINISPAN_GLOBAL_STATISTICS_PROP = "hibernate.cache.infinispan.statistics";

   private static final String ENTITY_KEY = "entity";
   
   /**
    * Name of the configuration that should be used for entity caches.
    * 
    * @see #DEF_ENTITY_RESOURCE
    */
   public static final String ENTITY_CACHE_RESOURCE_PROP = PREFIX + ENTITY_KEY + CONFIG_SUFFIX;
   
   private static final String COLLECTION_KEY = "collection";
   
   /**
    * Name of the configuration that should be used for collection caches.
    * No default value, as by default we try to use the same Infinispan cache
    * instance we use for entity caching.
    * 
    * @see #ENTITY_CACHE_RESOURCE_PROP
    * @see #DEF_ENTITY_RESOURCE
    */
   public static final String COLLECTION_CACHE_RESOURCE_PROP = PREFIX + COLLECTION_KEY + CONFIG_SUFFIX;

   private static final String TIMESTAMPS_KEY = "timestamps";

   /**
    * Name of the configuration that should be used for timestamp caches.
    * 
    * @see #DEF_TS_RESOURCE
    */
   public static final String TIMESTAMPS_CACHE_RESOURCE_PROP = PREFIX + TIMESTAMPS_KEY + CONFIG_SUFFIX;

   private static final String QUERY_KEY = "query";

   /**
    * Name of the configuration that should be used for query caches.
    * 
    * @see #DEF_QUERY_RESOURCE
    */
   public static final String QUERY_CACHE_RESOURCE_PROP = PREFIX + QUERY_KEY + CONFIG_SUFFIX;

   /**
    * Default value for {@link #INFINISPAN_RESOURCE_PROP}. Specifies the "infinispan-configs.xml" file in this package.
    */
   public static final String DEF_INFINISPAN_CONFIG_RESOURCE = "org/hibernate/cache/infinispan/builder/infinispan-configs.xml";

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

   private CacheManager manager;

   private final Map<String, TypeOverrides> typeOverrides = new HashMap<String, TypeOverrides>();

   private final Set<String> definedConfigurations = new HashSet<String>();

   private org.infinispan.transaction.lookup.TransactionManagerLookup transactionManagerlookup;

   private TransactionManager transactionManager;

   /**
    * Create a new instance using the default configuration.
    */
   public InfinispanRegionFactory() {
   }

   /**
    * Create a new instance using conifguration properties in <code>props</code>.
    * 
    * @param props
    *           Environmental properties; currently unused.
    */
   public InfinispanRegionFactory(Properties props) {
   }

   /** {@inheritDoc} */
   public CollectionRegion buildCollectionRegion(String regionName, Properties properties, CacheDataDescription metadata) throws CacheException {
      log.debug("Building collection cache region [" + regionName + "]");
      Cache cache = getCache(regionName, COLLECTION_KEY, properties);
      CacheAdapter cacheAdapter = CacheAdapterImpl.newInstance(cache);
      CollectionRegionImpl region = new CollectionRegionImpl(cacheAdapter, regionName, metadata, transactionManager, this);
      region.start();
      return region;
   }

   /** {@inheritDoc} */
   public EntityRegion buildEntityRegion(String regionName, Properties properties, CacheDataDescription metadata) throws CacheException {
      if (log.isDebugEnabled()) log.debug("Building entity cache region [" + regionName + "]");
      Cache cache = getCache(regionName, ENTITY_KEY, properties);
      CacheAdapter cacheAdapter = CacheAdapterImpl.newInstance(cache);
      EntityRegionImpl region = new EntityRegionImpl(cacheAdapter, regionName, metadata, transactionManager, this);
      region.start();
      return region;
   }

   /**
    * {@inheritDoc}
    */
   public QueryResultsRegion buildQueryResultsRegion(String regionName, Properties properties)
            throws CacheException {
      log.debug("Building query results cache region [" + regionName + "]");
      String cacheName = typeOverrides.get(QUERY_KEY).getCacheName();
      CacheAdapter cacheAdapter = CacheAdapterImpl.newInstance(manager.getCache(cacheName));
      QueryResultsRegionImpl region = new QueryResultsRegionImpl(cacheAdapter, regionName, properties, transactionManager, this);
      region.start();
      return region;
   }

   /**
    * {@inheritDoc}
    */
   public TimestampsRegion buildTimestampsRegion(String regionName, Properties properties)
            throws CacheException {
      log.debug("Building timestamps cache region [" + regionName + "]");
      String cacheName = typeOverrides.get(TIMESTAMPS_KEY).getCacheName();
      CacheAdapter cacheAdapter = CacheAdapterImpl.newInstance(manager.getCache(cacheName));
      TimestampsRegionImpl region = new TimestampsRegionImpl(cacheAdapter, regionName, transactionManager, this);
      region.start();
      return region;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isMinimalPutsEnabledByDefault() {
      return true;
   }

   @Override
   public AccessType getDefaultAccessType() {
      return AccessType.TRANSACTIONAL;
   }

   /**
    * {@inheritDoc}
    */
   public long nextTimestamp() {
      return System.currentTimeMillis() / 100;
   }
   
   public void setCacheManager(CacheManager manager) {
      this.manager = manager;
   }

   public CacheManager getCacheManager() {
      return manager;
   }

   /**
    * {@inheritDoc}
    */
   public void start(Settings settings, Properties properties) throws CacheException {
      log.debug("Starting Infinispan region factory");
      try {
         transactionManagerlookup = new HibernateTransactionManagerLookup(settings, properties);
         transactionManager = transactionManagerlookup.getTransactionManager();
         manager = createCacheManager(properties);
         initGenericDataTypeOverrides();
         Enumeration keys = properties.propertyNames();
         while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            int prefixLoc = -1;
            if ((prefixLoc = key.indexOf(PREFIX)) != -1) {
               dissectProperty(prefixLoc, key, properties);
            }
         }
         defineGenericDataTypeCacheConfigurations(settings, properties);
      } catch (CacheException ce) {
         throw ce;
      } catch (Throwable t) {
          throw new CacheException("Unable to start region factory", t);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void stop() {
      log.debug("Stopping Infinispan CacheManager");
      manager.stop();
   }
   
   /**
    * Returns an unmodifiable map containing configured entity/collection type configuration overrides.
    * This method should be used primarily for testing/checking purpouses.
    * 
    * @return an unmodifiable map.
    */
   public Map<String, TypeOverrides> getTypeOverrides() {
      return Collections.unmodifiableMap(typeOverrides);
   }
   
   public Set<String> getDefinedConfigurations() {
      return Collections.unmodifiableSet(definedConfigurations);
   }

   protected CacheManager createCacheManager(Properties properties) throws CacheException {
      try {
         String configLoc = PropertiesHelper.getString(INFINISPAN_CONFIG_RESOURCE_PROP, properties, DEF_INFINISPAN_CONFIG_RESOURCE);
         CacheManager manager = new DefaultCacheManager(configLoc, false);
         String globalStats = PropertiesHelper.extractPropertyValue(INFINISPAN_GLOBAL_STATISTICS_PROP, properties);
         if (globalStats != null) {
            manager.getGlobalConfiguration().setExposeGlobalJmxStatistics(Boolean.parseBoolean(globalStats));
         }
         manager.start();
         return manager;
      } catch (IOException e) {
         throw new CacheException("Unable to create default cache manager", e);
      }
   }

   private Map<String, TypeOverrides> initGenericDataTypeOverrides() {
      TypeOverrides entityOverrides = new TypeOverrides();
      entityOverrides.setCacheName(DEF_ENTITY_RESOURCE);
      typeOverrides.put(ENTITY_KEY, entityOverrides);
      TypeOverrides collectionOverrides = new TypeOverrides();
      collectionOverrides.setCacheName(DEF_ENTITY_RESOURCE);
      typeOverrides.put(COLLECTION_KEY, collectionOverrides);
      TypeOverrides timestampOverrides = new TimestampTypeOverrides();
      timestampOverrides.setCacheName(DEF_TIMESTAMPS_RESOURCE);
      typeOverrides.put(TIMESTAMPS_KEY, timestampOverrides);
      TypeOverrides queryOverrides = new TypeOverrides();
      queryOverrides.setCacheName(DEF_QUERY_RESOURCE);
      typeOverrides.put(QUERY_KEY, queryOverrides);
      return typeOverrides;
   }

   private void dissectProperty(int prefixLoc, String key, Properties properties) {
      TypeOverrides cfgOverride = null;
      int suffixLoc = -1;
      if (!key.equals(INFINISPAN_CONFIG_RESOURCE_PROP) && (suffixLoc = key.indexOf(CONFIG_SUFFIX)) != -1) {
         cfgOverride = getOrCreateConfig(prefixLoc, key, suffixLoc);
         cfgOverride.setCacheName(PropertiesHelper.extractPropertyValue(key, properties));
      } else if ((suffixLoc = key.indexOf(STRATEGY_SUFFIX)) != -1) {
         cfgOverride = getOrCreateConfig(prefixLoc, key, suffixLoc);
         cfgOverride.setEvictionStrategy(PropertiesHelper.extractPropertyValue(key, properties));
      } else if ((suffixLoc = key.indexOf(WAKE_UP_INTERVAL_SUFFIX)) != -1) {
         cfgOverride = getOrCreateConfig(prefixLoc, key, suffixLoc);
         cfgOverride.setEvictionWakeUpInterval(Long.parseLong(PropertiesHelper.extractPropertyValue(key, properties)));
      } else if ((suffixLoc = key.indexOf(MAX_ENTRIES_SUFFIX)) != -1) {
         cfgOverride = getOrCreateConfig(prefixLoc, key, suffixLoc);
         cfgOverride.setEvictionMaxEntries(PropertiesHelper.getInt(key, properties, -1));
      } else if ((suffixLoc = key.indexOf(LIFESPAN_SUFFIX)) != -1) {
         cfgOverride = getOrCreateConfig(prefixLoc, key, suffixLoc);
         cfgOverride.setExpirationLifespan(Long.parseLong(PropertiesHelper.extractPropertyValue(key, properties)));
      } else if ((suffixLoc = key.indexOf(MAX_IDLE_SUFFIX)) != -1) {
         cfgOverride = getOrCreateConfig(prefixLoc, key, suffixLoc);
         cfgOverride.setExpirationMaxIdle(Long.parseLong(PropertiesHelper.extractPropertyValue(key, properties)));
      }
//      else if ((suffixLoc = key.indexOf(STATISTICS_SUFFIX)) != -1) {
//         cfgOverride = getOrCreateConfig(prefixLoc, key, suffixLoc);
//         cfgOverride.setExposeStatistics(Boolean.parseBoolean(PropertiesHelper.extractPropertyValue(key, properties)));
//      }
   }

   private TypeOverrides getOrCreateConfig(int prefixLoc, String key, int suffixLoc) {
      String name = key.substring(prefixLoc + PREFIX.length(), suffixLoc);
      TypeOverrides cfgOverride = typeOverrides.get(name);
      if (cfgOverride == null) {
         cfgOverride = new TypeOverrides();
         typeOverrides.put(name, cfgOverride);
      }
      return cfgOverride;
   }

   private void defineGenericDataTypeCacheConfigurations(Settings settings, Properties properties) throws CacheException {
      String[] defaultGenericDataTypes = new String[]{ENTITY_KEY, COLLECTION_KEY, TIMESTAMPS_KEY, QUERY_KEY};
      for (String type : defaultGenericDataTypes) {
         TypeOverrides override = overrideStatisticsIfPresent(typeOverrides.get(type), properties);
         String cacheName = override.getCacheName();
         Configuration newCacheCfg = override.createInfinispanConfiguration();
         // Apply overrides
         Configuration cacheConfig = manager.defineConfiguration(cacheName, cacheName, newCacheCfg);
         // Configure transaction manager
         cacheConfig = configureTransactionManager(cacheConfig, cacheName, properties);
         manager.defineConfiguration(cacheName, cacheName, cacheConfig);
         definedConfigurations.add(cacheName);
         override.validateInfinispanConfiguration(cacheConfig);
      }
   }

   private Cache getCache(String regionName, String typeKey, Properties properties) {
      TypeOverrides regionOverride = typeOverrides.get(regionName);
      if (!definedConfigurations.contains(regionName)) {
         String templateCacheName = null;
         Configuration regionCacheCfg = null;
         if (regionOverride != null) {
            if (log.isDebugEnabled()) log.debug("Entity cache region specific configuration exists: " + regionOverride);
            regionOverride = overrideStatisticsIfPresent(regionOverride, properties);
            regionCacheCfg = regionOverride.createInfinispanConfiguration();
            String cacheName = regionOverride.getCacheName();
            if (cacheName != null) // Region specific override with a given cache name
               templateCacheName = cacheName; 
            else // Region specific override without cache name, so template cache name is generic for data type.
               templateCacheName = typeOverrides.get(typeKey).getCacheName(); 
         } else {
            // No region specific overrides, template cache name is generic for data type.
            templateCacheName = typeOverrides.get(typeKey).getCacheName();
            regionCacheCfg = typeOverrides.get(typeKey).createInfinispanConfiguration();
         }
         // Configure transaction manager
         regionCacheCfg = configureTransactionManager(regionCacheCfg, templateCacheName, properties);
         // Apply overrides
         manager.defineConfiguration(regionName, templateCacheName, regionCacheCfg);
         definedConfigurations.add(regionName);
      }
      return manager.getCache(regionName);
   }

   private Configuration configureTransactionManager(Configuration regionOverrides, String templateCacheName, Properties properties) {
      // Get existing configuration to verify whether a tm was configured or not.
      Configuration templateConfig = manager.defineConfiguration(templateCacheName, new Configuration());
      String ispnTmLookupClassName = templateConfig.getTransactionManagerLookupClass();
      String hbTmLookupClassName = org.hibernate.cache.infinispan.tm.HibernateTransactionManagerLookup.class.getName();
      if (ispnTmLookupClassName != null && !ispnTmLookupClassName.equals(hbTmLookupClassName)) {
         log.debug("Infinispan is configured [" + ispnTmLookupClassName + "] with a different transaction manager lookup " +
               "class than Hibernate [" + hbTmLookupClassName + "]");
      } else {
         regionOverrides.setTransactionManagerLookup(transactionManagerlookup);
      }
      return regionOverrides;
   }

   private TypeOverrides overrideStatisticsIfPresent(TypeOverrides override, Properties properties) {
      String globalStats = PropertiesHelper.extractPropertyValue(INFINISPAN_GLOBAL_STATISTICS_PROP, properties);
      if (globalStats != null) {
         override.setExposeStatistics(Boolean.parseBoolean(globalStats));
      }
      return override;
   }
}