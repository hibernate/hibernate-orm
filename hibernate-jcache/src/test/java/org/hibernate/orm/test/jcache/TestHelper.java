/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jcache;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.jcache.ConfigSettings;
import org.hibernate.cache.jcache.JCacheHelper;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.support.RegionNameQualifier;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.jcache.domain.Item;
import org.hibernate.orm.test.jcache.domain.VersionedItem;
import org.hibernate.orm.test.jcache.domain.Event;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.hibernate.tool.schema.Action;

import static org.hibernate.cache.jcache.JCacheHelper.locateStandardCacheManager;

/**
 * @author Steve Ebersole
 */
public class TestHelper {
	public static String[] entityRegionNames = new String[] {
			Item.class.getName(),
			VersionedItem.class.getName(),
			Event.class.getName()
	};
	public static String[] collectionRegionNames = new String[] {
			Event.class.getName() + ".participants"
	};

	public static String[] allDomainRegionNames =
			Stream.concat( Arrays.stream( entityRegionNames ), Arrays.stream( collectionRegionNames ) )
					.toArray( String[]::new );

	public static String[] queryRegionNames = new String[] {
			RegionFactory.DEFAULT_QUERY_RESULTS_REGION_UNQUALIFIED_NAME,
			RegionFactory.DEFAULT_UPDATE_TIMESTAMPS_REGION_UNQUALIFIED_NAME
	};
	public static String[] queryRegionLegacyNames1 = new String[] {
			"org.hibernate.cache.spi.QueryResultsRegion",
			"org.hibernate.cache.spi.TimestampsRegion"
	};
	public static String[] queryRegionLegacyNames2 = new String[] {
			"org.hibernate.cache.internal.StandardQueryCache",
			"org.hibernate.cache.spi.UpdateTimestampsCache"
	};

	public static void preBuildAllCaches() {
		preBuildAllCaches( true );
	}

	public static void preBuildAllCaches(boolean prefixCaches) {
		preBuildDomainCaches( prefixCaches );

		final CacheManager cacheManager = locateStandardCacheManager();

		for ( String regionName : queryRegionNames ) {
			createCache( cacheManager, regionName, prefixCaches );
		}
	}

	public static void preBuildDomainCaches() {
		preBuildDomainCaches( true );
	}

	public static void preBuildDomainCaches(boolean prefixCaches) {
		final CacheManager cacheManager = locateStandardCacheManager();

		for ( String regionName : entityRegionNames ) {
			createCache( cacheManager, regionName, prefixCaches );
		}

		for ( String regionName : collectionRegionNames ) {
			createCache( cacheManager, regionName, prefixCaches );
		}
	}

	public static SessionFactoryImplementor buildStandardSessionFactory() {
		return buildStandardSessionFactory( ignored -> { } );
	}

	public static SessionFactoryImplementor buildSessionFactoryWithMissingCacheStrategy(String missingCacheStrategy) {
		final StandardServiceRegistry ssr = getStandardServiceRegistryBuilder()
				.applySetting( ConfigSettings.MISSING_CACHE_STRATEGY, missingCacheStrategy )
				.build();
		try {
			return (SessionFactoryImplementor) new MetadataSources( ssr ).buildMetadata().buildSessionFactory();
		}
		catch (Throwable t) {
			ssr.close();
			throw t;
		}
	}

	public static SessionFactoryImplementor buildStandardSessionFactory(Consumer<StandardServiceRegistryBuilder> additionalSettings) {
		final StandardServiceRegistryBuilder ssrb = getStandardServiceRegistryBuilder();

		additionalSettings.accept( ssrb );

		final StandardServiceRegistry ssr = ssrb.build();
		try {
			return (SessionFactoryImplementor) new MetadataSources( ssr ).buildMetadata().buildSessionFactory();
		}
		catch (Throwable t) {
			ssr.close();
			throw t;
		}
	}

	public static StandardServiceRegistryBuilder getStandardServiceRegistryBuilder() {
		final StandardServiceRegistryBuilder ssrb = ServiceRegistryUtil.serviceRegistryBuilder()
				.configure( "hibernate-config/hibernate.cfg.xml" )
				.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" )
				.applySetting( AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, Action.CREATE_DROP )
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" );

		if ( H2Dialect.class.equals( DialectContext.getDialect().getClass() ) ) {
			ssrb.applySetting( AvailableSettings.URL, "jdbc:h2:mem:db-mvcc;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE" );
		}
		return ssrb;
	}

	public static void createCache(CacheManager cacheManager, String name) {
		createCache( cacheManager, name, true );
	}

	public static void createCache(CacheManager cacheManager, String name, boolean usePrefix) {
		if ( usePrefix ) {
			name = prefix( name );
		}

		if ( cacheManager.getCache( name ) != null ) {
			cacheManager.destroyCache( name );
		}

		cacheManager.createCache( name, new MutableConfiguration<>() );
	}

	public static void createCache(String name) {
		createCache( locateStandardCacheManager(), name );
	}

	public static String prefix(String regionName) {
		return RegionNameQualifier.INSTANCE.qualify( "hibernate.test", regionName );
	}

	public static Cache<?, ?> getCache(String regionName) {
		final CacheManager cacheManager = JCacheHelper.locateStandardCacheManager();
		regionName = prefix( regionName );
		return cacheManager.getCache( regionName );
	}

	public static void visitDomainRegions(Consumer<Cache> action) {
		final CacheManager cacheManager = JCacheHelper.locateStandardCacheManager();

		for ( String regionName : entityRegionNames ) {
			action.accept( cacheManager.getCache( regionName ) );
		}

		for ( String regionName : collectionRegionNames ) {
			action.accept( cacheManager.getCache( regionName ) );
		}
	}

	public static void createRegions(Metadata metadata, boolean queryRegions) {
		createRegions( metadata, queryRegions, true );
	}

	public static void createRegions(Metadata metadata, boolean queryRegions, boolean prefixRegions) {
		Set<String> names = new HashSet<>();

		final CacheManager cacheManager = locateStandardCacheManager();

		for ( PersistentClass persistentClass : metadata.getEntityBindings() ) {
			if ( persistentClass.getRootClass().isCached() ) {
				if ( ! names.add( persistentClass.getRootClass().getCacheRegionName() ) ) {
					continue;
				}

				createCache( cacheManager, persistentClass.getRootClass().getCacheRegionName(), prefixRegions );
			}

			if ( persistentClass.hasNaturalId() ) {
				if ( persistentClass.getNaturalIdCacheRegionName() != null ) {
					if ( ! names.add( persistentClass.getNaturalIdCacheRegionName() ) ) {
						continue;
					}

					createCache( cacheManager, persistentClass.getNaturalIdCacheRegionName(), prefixRegions );
				}
			}
		}

		for ( Collection collection : metadata.getCollectionBindings() ) {
			if ( collection.getCacheRegionName() == null
					|| collection.getCacheConcurrencyStrategy() == null ) {
				continue;
			}

			if ( ! names.add( collection.getCacheRegionName() ) ) {
				continue;
			}

			createCache( cacheManager, collection.getCacheRegionName(), prefixRegions );
		}

		if ( queryRegions ) {
			for ( String regionName : queryRegionNames ) {
				createCache( cacheManager, regionName, prefixRegions );
			}
		}
	}

	private TestHelper() {
	}
}
