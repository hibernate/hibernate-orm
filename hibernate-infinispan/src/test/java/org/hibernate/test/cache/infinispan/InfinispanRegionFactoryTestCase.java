/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan;

import java.io.InputStream;
import java.util.Properties;
import java.util.function.BiConsumer;
import javax.transaction.TransactionManager;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.collection.CollectionRegionImpl;
import org.hibernate.cache.infinispan.entity.EntityRegionImpl;
import org.hibernate.cache.infinispan.query.QueryResultsRegionImpl;
import org.hibernate.cache.infinispan.timestamp.TimestampsRegionImpl;
import org.hibernate.cache.infinispan.tm.HibernateTransactionManagerLookup;
import org.hibernate.cache.internal.CacheDataDescriptionImpl;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.transaction.jta.platform.internal.AbstractJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JBossStandAloneJtaPlatform;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.test.cache.infinispan.util.CacheTestUtil;
import org.hibernate.test.cache.infinispan.util.InfinispanTestingSetup;
import org.infinispan.commons.util.FileLookupFactory;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.junit.Rule;
import org.junit.Test;

import org.infinispan.AdvancedCache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ClusteringConfigurationBuilder;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.TestingUtil;
import org.infinispan.transaction.TransactionMode;

import static org.hibernate.cache.infinispan.InfinispanRegionFactory.DEF_PENDING_PUTS_RESOURCE;
import static org.hibernate.cache.infinispan.InfinispanRegionFactory.DEF_TIMESTAMPS_RESOURCE;
import static org.hibernate.cache.infinispan.InfinispanRegionFactory.DataType;
import static org.hibernate.cache.infinispan.InfinispanRegionFactory.INFINISPAN_CONFIG_RESOURCE_PROP;
import static org.hibernate.cache.infinispan.InfinispanRegionFactory.TIMESTAMPS_CACHE_RESOURCE_PROP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * InfinispanRegionFactoryTestCase.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public class InfinispanRegionFactoryTestCase  {
	private static final CacheDataDescription MUTABLE_NON_VERSIONED = new CacheDataDescriptionImpl(true, false, null, null);
	private static final CacheDataDescription IMMUTABLE_NON_VERSIONED = new CacheDataDescriptionImpl(false, false, null, null);

	@Rule
	public InfinispanTestingSetup infinispanTestIdentifier = new InfinispanTestingSetup();

	@Test
	public void testConfigurationProcessing() {
		final String person = "com.acme.Person";
		final String addresses = "com.acme.Person.addresses";
		Properties p = createProperties();
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.cfg", "person-cache");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.eviction.strategy", "LRU");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.eviction.max_entries", "5000");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.expiration.wake_up_interval", "2000");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.expiration.lifespan", "60000");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.expiration.max_idle", "30000");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.addresses.cfg", "person-addresses-cache");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.addresses.expiration.lifespan", "120000");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.addresses.expiration.max_idle", "60000");
		p.setProperty("hibernate.cache.infinispan.query.cfg", "my-query-cache");
		p.setProperty("hibernate.cache.infinispan.query.eviction.strategy", "LIRS");
		p.setProperty("hibernate.cache.infinispan.query.expiration.wake_up_interval", "3000");
		p.setProperty("hibernate.cache.infinispan.query.eviction.max_entries", "10000");

		TestInfinispanRegionFactory factory = createRegionFactory(p);

		try {
			assertEquals("person-cache", factory.getBaseConfiguration(person));
			Configuration personOverride = factory.getConfigurationOverride(person);
			assertEquals(EvictionStrategy.LRU, personOverride.eviction().strategy());
			assertEquals(5000, personOverride.eviction().maxEntries());
			assertEquals(2000, personOverride.expiration().wakeUpInterval());
			assertEquals(60000, personOverride.expiration().lifespan());
			assertEquals(30000, personOverride.expiration().maxIdle());

			assertEquals("person-addresses-cache", factory.getBaseConfiguration(addresses));
			Configuration addressesOverride = factory.getConfigurationOverride(addresses);
			assertEquals(120000, addressesOverride.expiration().lifespan());
			assertEquals(60000, addressesOverride.expiration().maxIdle());

			assertEquals("my-query-cache", factory.getBaseConfiguration(DataType.QUERY));
			Configuration queryOverride = factory.getConfigurationOverride(DataType.QUERY);
			assertEquals(EvictionStrategy.LIRS, queryOverride.eviction().strategy());
			assertEquals(10000, queryOverride.eviction().maxEntries());
			assertEquals(3000, queryOverride.expiration().wakeUpInterval());
		} finally {
			factory.stop();
		}
	}

	@Test
	public void testBuildEntityCollectionRegionsPersonPlusEntityCollectionOverrides() {
		final String person = "com.acme.Person";
		final String address = "com.acme.Address";
		final String car = "com.acme.Car";
		final String addresses = "com.acme.Person.addresses";
		final String parts = "com.acme.Car.parts";
		Properties p = createProperties();
		// First option, cache defined for entity and overrides for generic entity data type and entity itself.
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.cfg", "person-cache");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.eviction.strategy", "LRU");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.eviction.max_entries", "5000");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.expiration.wake_up_interval", "2000");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.expiration.lifespan", "60000");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.expiration.max_idle", "30000");
		p.setProperty("hibernate.cache.infinispan.entity.cfg", "myentity-cache");
		p.setProperty("hibernate.cache.infinispan.entity.eviction.strategy", "LIRS");
		p.setProperty("hibernate.cache.infinispan.entity.expiration.wake_up_interval", "3000");
		p.setProperty("hibernate.cache.infinispan.entity.eviction.max_entries", "20000");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.addresses.cfg", "addresses-cache");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.addresses.eviction.strategy", "LIRS");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.addresses.eviction.max_entries", "5500");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.addresses.expiration.wake_up_interval", "2500");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.addresses.expiration.lifespan", "65000");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.addresses.expiration.max_idle", "35000");
		p.setProperty("hibernate.cache.infinispan.collection.cfg", "mycollection-cache");
		p.setProperty("hibernate.cache.infinispan.collection.eviction.strategy", "LRU");
		p.setProperty("hibernate.cache.infinispan.collection.expiration.wake_up_interval", "3500");
		p.setProperty("hibernate.cache.infinispan.collection.eviction.max_entries", "25000");
		TestInfinispanRegionFactory factory = createRegionFactory(p);
		try {
			EmbeddedCacheManager manager = factory.getCacheManager();
			assertFalse(manager.getCacheManagerConfiguration().globalJmxStatistics().enabled());
			assertNotNull(factory.getBaseConfiguration(person));
			assertFalse(isDefinedCache(factory, person));
			assertNotNull(factory.getBaseConfiguration(addresses));
			assertFalse(isDefinedCache(factory, addresses));
			assertNull(factory.getBaseConfiguration(address));
			assertNull(factory.getBaseConfiguration(parts));
			AdvancedCache cache;

			EntityRegionImpl region = (EntityRegionImpl) factory.buildEntityRegion(person, p, MUTABLE_NON_VERSIONED);
			assertTrue(isDefinedCache(factory, person));
			cache = region.getCache();
			Configuration cacheCfg = cache.getCacheConfiguration();
			assertEquals(EvictionStrategy.LRU, cacheCfg.eviction().strategy());
			assertEquals(2000, cacheCfg.expiration().wakeUpInterval());
			assertEquals(5000, cacheCfg.eviction().maxEntries());
			assertEquals(60000, cacheCfg.expiration().lifespan());
			assertEquals(30000, cacheCfg.expiration().maxIdle());
			assertFalse(cacheCfg.jmxStatistics().enabled());

			region = (EntityRegionImpl) factory.buildEntityRegion(address, p, MUTABLE_NON_VERSIONED);
			assertTrue(isDefinedCache(factory, person));
			cache = region.getCache();
			cacheCfg = cache.getCacheConfiguration();
			assertEquals(EvictionStrategy.LIRS, cacheCfg.eviction().strategy());
			assertEquals(3000, cacheCfg.expiration().wakeUpInterval());
			assertEquals(20000, cacheCfg.eviction().maxEntries());
			assertFalse(cacheCfg.jmxStatistics().enabled());

			region = (EntityRegionImpl) factory.buildEntityRegion(car, p, MUTABLE_NON_VERSIONED);
			assertTrue(isDefinedCache(factory, person));
			cache = region.getCache();
			cacheCfg = cache.getCacheConfiguration();
			assertEquals(EvictionStrategy.LIRS, cacheCfg.eviction().strategy());
			assertEquals(3000, cacheCfg.expiration().wakeUpInterval());
			assertEquals(20000, cacheCfg.eviction().maxEntries());
			assertFalse(cacheCfg.jmxStatistics().enabled());

			CollectionRegionImpl collectionRegion = (CollectionRegionImpl)
					factory.buildCollectionRegion(addresses, p, MUTABLE_NON_VERSIONED);
			assertTrue(isDefinedCache(factory, person));

			cache = collectionRegion .getCache();
			cacheCfg = cache.getCacheConfiguration();
			assertEquals(EvictionStrategy.LIRS, cacheCfg.eviction().strategy());
			assertEquals(2500, cacheCfg.expiration().wakeUpInterval());
			assertEquals(5500, cacheCfg.eviction().maxEntries());
			assertEquals(65000, cacheCfg.expiration().lifespan());
			assertEquals(35000, cacheCfg.expiration().maxIdle());
			assertFalse(cacheCfg.jmxStatistics().enabled());

			collectionRegion = (CollectionRegionImpl) factory.buildCollectionRegion(parts, p, MUTABLE_NON_VERSIONED);
			assertTrue(isDefinedCache(factory, addresses));
			cache = collectionRegion.getCache();
			cacheCfg = cache.getCacheConfiguration();
			assertEquals(EvictionStrategy.LRU, cacheCfg.eviction().strategy());
			assertEquals(3500, cacheCfg.expiration().wakeUpInterval());
			assertEquals(25000, cacheCfg.eviction().maxEntries());
			assertFalse(cacheCfg.jmxStatistics().enabled());

			collectionRegion = (CollectionRegionImpl) factory.buildCollectionRegion(parts, p, MUTABLE_NON_VERSIONED);
			assertTrue(isDefinedCache(factory, addresses));
			cache = collectionRegion.getCache();
			cacheCfg = cache.getCacheConfiguration();
			assertEquals(EvictionStrategy.LRU, cacheCfg.eviction().strategy());
			assertEquals(3500, cacheCfg.expiration().wakeUpInterval());
			assertEquals(25000, cacheCfg.eviction().maxEntries());
			assertFalse(cacheCfg.jmxStatistics().enabled());
		} finally {
			factory.stop();
		}
	}

	@Test
	public void testBuildEntityCollectionRegionOverridesOnly() {
		final String address = "com.acme.Address";
		final String personAddressses = "com.acme.Person.addresses";
		AdvancedCache cache;
		Properties p = createProperties();
		p.setProperty("hibernate.cache.infinispan.entity.eviction.strategy", "LIRS");
		p.setProperty("hibernate.cache.infinispan.entity.eviction.max_entries", "30000");
		p.setProperty("hibernate.cache.infinispan.entity.expiration.wake_up_interval", "3000");
		p.setProperty("hibernate.cache.infinispan.collection.eviction.strategy", "LRU");
		p.setProperty("hibernate.cache.infinispan.collection.eviction.max_entries", "35000");
		p.setProperty("hibernate.cache.infinispan.collection.expiration.wake_up_interval", "3500");
		TestInfinispanRegionFactory factory = createRegionFactory(p);
		try {
			factory.getCacheManager();
			EntityRegionImpl region = (EntityRegionImpl) factory.buildEntityRegion(address, p, MUTABLE_NON_VERSIONED);
			assertNull(factory.getBaseConfiguration(address));
			cache = region.getCache();
			Configuration cacheCfg = cache.getCacheConfiguration();
			assertEquals(EvictionStrategy.LIRS, cacheCfg.eviction().strategy());
			assertEquals(3000, cacheCfg.expiration().wakeUpInterval());
			assertEquals(30000, cacheCfg.eviction().maxEntries());
			// Max idle value comes from base XML configuration
			assertEquals(100000, cacheCfg.expiration().maxIdle());
			CollectionRegionImpl collectionRegion = (CollectionRegionImpl)
					factory.buildCollectionRegion(personAddressses, p, MUTABLE_NON_VERSIONED);
			assertNull(factory.getBaseConfiguration(personAddressses));
			cache = collectionRegion.getCache();
			cacheCfg = cache.getCacheConfiguration();
			assertEquals(EvictionStrategy.LRU, cacheCfg.eviction().strategy());
			assertEquals(3500, cacheCfg.expiration().wakeUpInterval());
			assertEquals(35000, cacheCfg.eviction().maxEntries());
			assertEquals(100000, cacheCfg.expiration().maxIdle());
		} finally {
			factory.stop();
		}
	}
	@Test
	public void testBuildEntityRegionPersonPlusEntityOverridesWithoutCfg() {
		final String person = "com.acme.Person";
		Properties p = createProperties();
		// Third option, no cache defined for entity and overrides for generic entity data type and entity itself.
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.eviction.strategy", "LRU");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.expiration.lifespan", "60000");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.expiration.max_idle", "30000");
		p.setProperty("hibernate.cache.infinispan.entity.cfg", "myentity-cache");
		p.setProperty("hibernate.cache.infinispan.entity.eviction.strategy", "FIFO");
		p.setProperty("hibernate.cache.infinispan.entity.eviction.max_entries", "10000");
		p.setProperty("hibernate.cache.infinispan.entity.expiration.wake_up_interval", "3000");
		TestInfinispanRegionFactory factory = createRegionFactory(p);
		try {
			factory.getCacheManager();
			assertFalse( isDefinedCache(factory, person ) );
			EntityRegionImpl region = (EntityRegionImpl) factory.buildEntityRegion( person, p, MUTABLE_NON_VERSIONED );
			assertTrue( isDefinedCache(factory, person ) );
			AdvancedCache cache = region.getCache();
			Configuration cacheCfg = cache.getCacheConfiguration();
			assertEquals(EvictionStrategy.LRU, cacheCfg.eviction().strategy());
			assertEquals(3000, cacheCfg.expiration().wakeUpInterval());
			assertEquals(10000, cacheCfg.eviction().maxEntries());
			assertEquals(60000, cacheCfg.expiration().lifespan());
			assertEquals(30000, cacheCfg.expiration().maxIdle());
		} finally {
			factory.stop();
		}
	}

	@Test
	public void testBuildImmutableEntityRegion() {
		AdvancedCache cache;
		Properties p = new Properties();
		TestInfinispanRegionFactory factory = createRegionFactory(p);
		try {
			factory.getCacheManager();
			EntityRegionImpl region = (EntityRegionImpl) factory.buildEntityRegion("com.acme.Address", p, IMMUTABLE_NON_VERSIONED);
			assertNull( factory.getBaseConfiguration( "com.acme.Address" ) );
			cache = region.getCache();
			Configuration cacheCfg = cache.getCacheConfiguration();
			assertEquals("Immutable entity should get non-transactional cache", TransactionMode.NON_TRANSACTIONAL, cacheCfg.transaction().transactionMode());
		} finally {
			factory.stop();
		}
	}

	@Test(expected = CacheException.class)
	public void testTimestampValidation() {
		final String timestamps = "org.hibernate.cache.spi.UpdateTimestampsCache";
		Properties p = createProperties();
      InputStream configStream = FileLookupFactory.newInstance().lookupFile(InfinispanRegionFactory.DEF_INFINISPAN_CONFIG_RESOURCE, getClass().getClassLoader());
      ConfigurationBuilderHolder cbh = new ParserRegistry().parse(configStream);
      DefaultCacheManager manager = new DefaultCacheManager(cbh, true);
		ConfigurationBuilder builder = new ConfigurationBuilder();
		builder.clustering().cacheMode(CacheMode.INVALIDATION_SYNC);
		manager.defineConfiguration( DEF_TIMESTAMPS_RESOURCE, builder.build() );
		try {
			InfinispanRegionFactory factory = createRegionFactory( manager, p, null );
			factory.start( CacheTestUtil.sfOptionsForStart(), p );
			TimestampsRegionImpl region = (TimestampsRegionImpl) factory.buildTimestampsRegion( timestamps, p );
			fail( "Should have failed saying that invalidation is not allowed for timestamp caches." );
		} finally {
			TestingUtil.killCacheManagers( manager );
		}
	}

	@Test
	public void testBuildDefaultTimestampsRegion() {
		final String timestamps = "org.hibernate.cache.spi.UpdateTimestampsCache";
		Properties p = createProperties();
		InfinispanRegionFactory factory = createRegionFactory(p);
		try {
			assertTrue(isDefinedCache(factory, DEF_TIMESTAMPS_RESOURCE));
			TimestampsRegionImpl region = (TimestampsRegionImpl) factory.buildTimestampsRegion(timestamps, p);
			AdvancedCache cache = region.getCache();
			assertEquals(timestamps, cache.getName());
			Configuration cacheCfg = cache.getCacheConfiguration();
			assertEquals( EvictionStrategy.NONE, cacheCfg.eviction().strategy() );
			assertEquals( CacheMode.REPL_ASYNC, cacheCfg.clustering().cacheMode() );
			assertFalse( cacheCfg.jmxStatistics().enabled() );
		} finally {
			factory.stop();
		}
	}

	protected boolean isDefinedCache(InfinispanRegionFactory factory, String cacheName) {
		return factory.getCacheManager().getCacheConfiguration(cacheName) != null;
	}

	@Test
	public void testBuildDiffCacheNameTimestampsRegion() {
		final String timestamps = "org.hibernate.cache.spi.UpdateTimestampsCache";
		final String unrecommendedTimestamps = "unrecommended-timestamps";
		Properties p = createProperties();
		p.setProperty( TIMESTAMPS_CACHE_RESOURCE_PROP, unrecommendedTimestamps);
		TestInfinispanRegionFactory factory = createRegionFactory(p, (f, m) -> {
			ConfigurationBuilder builder = new ConfigurationBuilder();
			builder.clustering().stateTransfer().fetchInMemoryState(true);
			builder.clustering().cacheMode( CacheMode.REPL_SYNC );
			m.defineConfiguration(unrecommendedTimestamps, builder.build() );
		});
		try {
			assertEquals(unrecommendedTimestamps, factory.getBaseConfiguration(DataType.TIMESTAMPS));
			TimestampsRegionImpl region = (TimestampsRegionImpl) factory.buildTimestampsRegion(timestamps, p);
			AdvancedCache cache = region.getCache();
			Configuration cacheCfg = cache.getCacheConfiguration();
			assertEquals(EvictionStrategy.NONE, cacheCfg.eviction().strategy());
			assertEquals(CacheMode.REPL_SYNC, cacheCfg.clustering().cacheMode());
			assertFalse( cacheCfg.storeAsBinary().enabled() );
			assertFalse(cacheCfg.jmxStatistics().enabled());
		} finally {
			factory.stop();
		}
	}

	@Test
	public void testBuildTimestampsRegionWithCacheNameOverride() {
		final String timestamps = "org.hibernate.cache.spi.UpdateTimestampsCache";
		final String myTimestampsCache = "mytimestamps-cache";
		Properties p = createProperties();
		p.setProperty(TIMESTAMPS_CACHE_RESOURCE_PROP, myTimestampsCache);
		InfinispanRegionFactory factory = createRegionFactory(p, (f, m) -> {
			ClusteringConfigurationBuilder builder = new ConfigurationBuilder().clustering().cacheMode(CacheMode.LOCAL);
			m.defineConfiguration(myTimestampsCache, builder.build());
		});
		try {
			TimestampsRegionImpl region = (TimestampsRegionImpl) factory.buildTimestampsRegion(timestamps, p);
			assertTrue(isDefinedCache(factory, timestamps));
			// default timestamps cache is async replicated
			assertEquals(CacheMode.LOCAL, region.getCache().getCacheConfiguration().clustering().cacheMode());
		} finally {
			factory.stop();
		}
	}

	@Test(expected = CacheException.class)
	public void testBuildTimestampsRegionWithFifoEvictionOverride() {
		final String timestamps = "org.hibernate.cache.spi.UpdateTimestampsCache";
		final String myTimestampsCache = "mytimestamps-cache";
		Properties p = createProperties();
		p.setProperty(TIMESTAMPS_CACHE_RESOURCE_PROP, myTimestampsCache);
		p.setProperty("hibernate.cache.infinispan.timestamps.eviction.strategy", "FIFO");
		p.setProperty("hibernate.cache.infinispan.timestamps.eviction.max_entries", "10000");
		p.setProperty("hibernate.cache.infinispan.timestamps.expiration.wake_up_interval", "3000");
		InfinispanRegionFactory factory = null;
		try {
			factory = createRegionFactory(p);
			factory.buildTimestampsRegion(timestamps, p);
		} finally {
			if (factory != null) factory.stop();
		}
	}

	@Test
	public void testBuildTimestampsRegionWithNoneEvictionOverride() {
		final String timestamps = "org.hibernate.cache.spi.UpdateTimestampsCache";
		final String timestampsNoEviction = "timestamps-no-eviction";
		Properties p = createProperties();
		p.setProperty("hibernate.cache.infinispan.timestamps.cfg", timestampsNoEviction);
		p.setProperty("hibernate.cache.infinispan.timestamps.eviction.strategy", "NONE");
		p.setProperty("hibernate.cache.infinispan.timestamps.eviction.max_entries", "0");
		p.setProperty("hibernate.cache.infinispan.timestamps.expiration.wake_up_interval", "3000");
		InfinispanRegionFactory factory = createRegionFactory(p);
		try {
			TimestampsRegionImpl region = (TimestampsRegionImpl) factory.buildTimestampsRegion( timestamps, p );
			assertTrue( isDefinedCache(factory, timestamps) );
			assertEquals(3000, region.getCache().getCacheConfiguration().expiration().wakeUpInterval());
		} finally {
			factory.stop();
		}
	}

	@Test
	public void testBuildQueryRegion() {
		final String query = "org.hibernate.cache.internal.StandardQueryCache";
		Properties p = createProperties();
		InfinispanRegionFactory factory = createRegionFactory(p);
		try {
			assertTrue(isDefinedCache(factory, "local-query"));
			QueryResultsRegionImpl region = (QueryResultsRegionImpl) factory.buildQueryResultsRegion(query, p);
			AdvancedCache cache = region.getCache();
			Configuration cacheCfg = cache.getCacheConfiguration();
			assertEquals( CacheMode.LOCAL, cacheCfg.clustering().cacheMode() );
			assertFalse( cacheCfg.jmxStatistics().enabled() );
		} finally {
			factory.stop();
		}
	}

	@Test
	public void testBuildQueryRegionWithCustomRegionName() {
		final String queryRegionName = "myquery";
		Properties p = createProperties();
		p.setProperty("hibernate.cache.infinispan.myquery.cfg", "timestamps-none-eviction");
		p.setProperty("hibernate.cache.infinispan.myquery.eviction.strategy", "LIRS");
		p.setProperty("hibernate.cache.infinispan.myquery.expiration.wake_up_interval", "2222");
		p.setProperty("hibernate.cache.infinispan.myquery.eviction.max_entries", "11111");
		TestInfinispanRegionFactory factory = createRegionFactory(p);
		try {
			assertTrue(isDefinedCache(factory, "local-query"));
			QueryResultsRegionImpl region = (QueryResultsRegionImpl) factory.buildQueryResultsRegion(queryRegionName, p);
			assertNotNull(factory.getBaseConfiguration(queryRegionName));
			assertTrue(isDefinedCache(factory, queryRegionName));
			AdvancedCache cache = region.getCache();
			Configuration cacheCfg = cache.getCacheConfiguration();
			assertEquals(EvictionStrategy.LIRS, cacheCfg.eviction().strategy());
			assertEquals(2222, cacheCfg.expiration().wakeUpInterval());
			assertEquals( 11111, cacheCfg.eviction().maxEntries() );
		} finally {
			factory.stop();
		}
	}

	@Test
	public void testEnableStatistics() {
		Properties p = createProperties();
		p.setProperty("hibernate.cache.infinispan.statistics", "true");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.expiration.lifespan", "60000");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.expiration.max_idle", "30000");
		p.setProperty("hibernate.cache.infinispan.entity.cfg", "myentity-cache");
		p.setProperty("hibernate.cache.infinispan.entity.eviction.strategy", "FIFO");
		p.setProperty("hibernate.cache.infinispan.entity.expiration.wake_up_interval", "3000");
		p.setProperty("hibernate.cache.infinispan.entity.eviction.max_entries", "10000");
		InfinispanRegionFactory factory = createRegionFactory(p);
		try {
			EmbeddedCacheManager manager = factory.getCacheManager();
			assertTrue(manager.getCacheManagerConfiguration().globalJmxStatistics().enabled());
			EntityRegionImpl region = (EntityRegionImpl) factory.buildEntityRegion("com.acme.Address", p, MUTABLE_NON_VERSIONED);
			AdvancedCache cache = region.getCache();
			assertTrue(cache.getCacheConfiguration().jmxStatistics().enabled());

			region = (EntityRegionImpl) factory.buildEntityRegion("com.acme.Person", p, MUTABLE_NON_VERSIONED);
			cache = region.getCache();
			assertTrue(cache.getCacheConfiguration().jmxStatistics().enabled());

			final String query = "org.hibernate.cache.internal.StandardQueryCache";
			QueryResultsRegionImpl queryRegion = (QueryResultsRegionImpl)
					factory.buildQueryResultsRegion(query, p);
			cache = queryRegion.getCache();
			assertTrue(cache.getCacheConfiguration().jmxStatistics().enabled());

			final String timestamps = "org.hibernate.cache.spi.UpdateTimestampsCache";
			ConfigurationBuilder builder = new ConfigurationBuilder();
			builder.clustering().stateTransfer().fetchInMemoryState(true);
			manager.defineConfiguration("timestamps", builder.build());
			TimestampsRegionImpl timestampsRegion = (TimestampsRegionImpl)
					factory.buildTimestampsRegion(timestamps, p);
			cache = timestampsRegion.getCache();
			assertTrue(cache.getCacheConfiguration().jmxStatistics().enabled());

			CollectionRegionImpl collectionRegion = (CollectionRegionImpl)
					factory.buildCollectionRegion("com.acme.Person.addresses", p, MUTABLE_NON_VERSIONED);
			cache = collectionRegion.getCache();
			assertTrue(cache.getCacheConfiguration().jmxStatistics().enabled());
		} finally {
			factory.stop();
		}
	}

	@Test
	public void testDisableStatistics() {
		Properties p = createProperties();
		p.setProperty("hibernate.cache.infinispan.statistics", "false");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.expiration.lifespan", "60000");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.expiration.max_idle", "30000");
		p.setProperty("hibernate.cache.infinispan.entity.cfg", "myentity-cache");
		p.setProperty("hibernate.cache.infinispan.entity.eviction.strategy", "FIFO");
		p.setProperty("hibernate.cache.infinispan.entity.expiration.wake_up_interval", "3000");
		p.setProperty("hibernate.cache.infinispan.entity.eviction.max_entries", "10000");
		InfinispanRegionFactory factory = createRegionFactory(p);
		try {
			EntityRegionImpl region = (EntityRegionImpl) factory.buildEntityRegion("com.acme.Address", p, MUTABLE_NON_VERSIONED);
			AdvancedCache cache = region.getCache();
			assertFalse( cache.getCacheConfiguration().jmxStatistics().enabled() );

			region = (EntityRegionImpl) factory.buildEntityRegion("com.acme.Person", p, MUTABLE_NON_VERSIONED);
			cache = region.getCache();
			assertFalse( cache.getCacheConfiguration().jmxStatistics().enabled() );

			final String query = "org.hibernate.cache.internal.StandardQueryCache";
			QueryResultsRegionImpl queryRegion = (QueryResultsRegionImpl) factory.buildQueryResultsRegion(query, p);
			cache = queryRegion.getCache();
			assertFalse( cache.getCacheConfiguration().jmxStatistics().enabled() );

			final String timestamps = "org.hibernate.cache.spi.UpdateTimestampsCache";
			ConfigurationBuilder builder = new ConfigurationBuilder();
			builder.clustering().stateTransfer().fetchInMemoryState(true);
			factory.getCacheManager().defineConfiguration( "timestamps", builder.build() );
			TimestampsRegionImpl timestampsRegion = (TimestampsRegionImpl)
					factory.buildTimestampsRegion(timestamps, p);
			cache = timestampsRegion.getCache();
			assertFalse( cache.getCacheConfiguration().jmxStatistics().enabled() );

			CollectionRegionImpl collectionRegion = (CollectionRegionImpl)
					factory.buildCollectionRegion("com.acme.Person.addresses", p, MUTABLE_NON_VERSIONED);
			cache = collectionRegion.getCache();
			assertFalse( cache.getCacheConfiguration().jmxStatistics().enabled() );
		} finally {
			factory.stop();
		}
	}

	@Test
	public void testDefaultPendingPutsCache() {
		Properties p = createProperties();
		InfinispanRegionFactory factory = createRegionFactory(p);
		try {
			Configuration ppConfig = factory.getCacheManager().getCacheConfiguration(DEF_PENDING_PUTS_RESOURCE);

			assertTrue(ppConfig.isTemplate());
			assertFalse(ppConfig.clustering().cacheMode().isClustered());
			assertTrue(ppConfig.simpleCache());
			assertEquals(TransactionMode.NON_TRANSACTIONAL, ppConfig.transaction().transactionMode());
			assertEquals(60000, ppConfig.expiration().maxIdle());
			assertFalse(ppConfig.jmxStatistics().enabled());
			assertFalse(ppConfig.jmxStatistics().available());
		} finally {
			factory.stop();
		}
	}

	@Test
	public void testCustomPendingPutsCache() {
		Properties p = createProperties();
		p.setProperty(INFINISPAN_CONFIG_RESOURCE_PROP, "alternative-infinispan-configs.xml");
		InfinispanRegionFactory factory = createRegionFactory(p);
		try {
			Configuration ppConfig = factory.getCacheManager().getCacheConfiguration(DEF_PENDING_PUTS_RESOURCE);
			assertEquals(120000, ppConfig.expiration().maxIdle());
		} finally {
			factory.stop();
		}
	}

	private TestInfinispanRegionFactory createRegionFactory(Properties p) {
		return createRegionFactory(null, p, null);
	}

	private TestInfinispanRegionFactory createRegionFactory(Properties p,
		   BiConsumer<TestInfinispanRegionFactory, EmbeddedCacheManager> hook) {
		return createRegionFactory(null, p, hook);
	}

	private TestInfinispanRegionFactory createRegionFactory(final EmbeddedCacheManager manager, Properties p,
		   BiConsumer<TestInfinispanRegionFactory, EmbeddedCacheManager> hook) {
		final TestInfinispanRegionFactory factory = new TestInfinispanRegionFactory(manager, hook);
		factory.start( CacheTestUtil.sfOptionsForStart(), p );
		return factory;
	}

	private static Properties createProperties() {
		final Properties properties = new Properties();
		// If configured in the environment, add configuration file name to properties.
		final String cfgFileName =
				  (String) Environment.getProperties().get( INFINISPAN_CONFIG_RESOURCE_PROP );
		if ( cfgFileName != null ) {
			properties.put( INFINISPAN_CONFIG_RESOURCE_PROP, cfgFileName );
		}
		return properties;
	}

	private static class TestInfinispanRegionFactory extends org.hibernate.test.cache.infinispan.util.TestInfinispanRegionFactory {
		private final EmbeddedCacheManager providedManager;
		private final BiConsumer<TestInfinispanRegionFactory, EmbeddedCacheManager> afterCacheManagerCreated;

		public TestInfinispanRegionFactory(EmbeddedCacheManager providedManager,
													  BiConsumer<TestInfinispanRegionFactory, EmbeddedCacheManager> afterCacheManagerCreated) {
			super(new Properties());
			this.providedManager = providedManager;
			this.afterCacheManagerCreated = afterCacheManagerCreated;
		}

		@Override
      protected org.infinispan.transaction.lookup.TransactionManagerLookup createTransactionManagerLookup(SessionFactoryOptions settings, Properties properties) {
         return new HibernateTransactionManagerLookup(null, null) {
            @Override
            public TransactionManager getTransactionManager() throws Exception {
               AbstractJtaPlatform jta = new JBossStandAloneJtaPlatform();
               jta.injectServices(ServiceRegistryBuilder.buildServiceRegistry());
               return jta.getTransactionManager();
            }
         };
      }

		@Override
      protected EmbeddedCacheManager createCacheManager(Properties properties, ServiceRegistry serviceRegistry) throws CacheException {
         EmbeddedCacheManager m;
			if (providedManager != null)
				m = providedManager;
			else
				m = super.createCacheManager(properties, serviceRegistry);
			// since data type cache configuration templates are defined when cache manager is created,
			// we have to use hooks and set the configuration before the whole factory starts
			if (afterCacheManagerCreated != null) {
				afterCacheManagerCreated.accept(this, m);
			}
			return m;
		}

		/* Used for testing */
      public String getBaseConfiguration(String regionName) {
         return baseConfigurations.get(regionName);
      }

		/* Used for testing */
      public String getBaseConfiguration(DataType dataType) {
         return baseConfigurations.get(dataType.key);
      }

		/* Used for testing */
      public Configuration getConfigurationOverride(String regionName) {
         return configOverrides.get(regionName).build(false);
      }

		/* Used for testing */
      public Configuration getConfigurationOverride(DataType dataType) {
         return configOverrides.get(dataType.key).build(false);
      }
	}
}
