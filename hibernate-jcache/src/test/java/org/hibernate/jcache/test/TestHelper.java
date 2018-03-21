/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jcache.test;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.jcache.JCacheHelper;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.cache.spi.support.RegionNameQualifier;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jcache.test.domain.Item;
import org.hibernate.jcache.test.domain.VersionedItem;
import org.hibernate.jcache.test.domain.Event;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
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
			org.hibernate.jcache.test.domain.Event.class.getName() + ".participants"
	};

	public static SessionFactoryImplementor buildStandardSessionFactory(boolean preBuildCaches) {
		return buildStandardSessionFactory( preBuildCaches, true );
	}

	public static SessionFactoryImplementor buildStandardSessionFactory(boolean preBuildCaches, boolean prefixCaches) {
		if ( preBuildCaches ) {
			final CacheManager cacheManager = locateStandardCacheManager();

			for ( String regionName : entityRegionNames ) {
				createCache( cacheManager, regionName, prefixCaches );
			}

			for ( String regionName : collectionRegionNames ) {
				createCache( cacheManager, regionName, prefixCaches );
			}

			createCache( cacheManager, TimestampsRegion.class.getName(), prefixCaches );
			createCache( cacheManager, QueryResultsRegion.class.getName(), prefixCaches );
		}

		final StandardServiceRegistryBuilder ssrb = getStandardServiceRegistryBuilder();

		final StandardServiceRegistry ssr = ssrb.build();

		return (SessionFactoryImplementor) new MetadataSources( ssr ).buildMetadata().buildSessionFactory();
	}

	public static StandardServiceRegistryBuilder getStandardServiceRegistryBuilder() {
		final StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder()
				.configure( "hibernate-config/hibernate.cfg.xml" )
				.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" )
				.applySetting( AvailableSettings.HBM2DDL_DATABASE_ACTION, Action.CREATE )
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" );

		if ( H2Dialect.class.equals( Dialect.getDialect().getClass() ) ) {
			ssrb.applySetting( AvailableSettings.URL, "jdbc:h2:mem:db-mvcc;MVCC=true" );
		}
		return ssrb;
	}

	public static void createCache(CacheManager cacheManager, String name) {
		createCache( cacheManager, name, true );
	}

	public static void createCache(CacheManager cacheManager, String name, boolean usePrefix) {
		if ( usePrefix ) {
			name = RegionNameQualifier.INSTANCE.qualify( "hibernate.test", name );
		}

		if ( cacheManager.getCache( name ) != null ) {
			cacheManager.destroyCache( name );
		}

		cacheManager.createCache( name, new MutableConfiguration<>() );
	}

	public static void createCache(String name) {
		createCache( locateStandardCacheManager(), name );
	}

	public static void visitAllRegions(Consumer<Cache> action) {
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
			createCache( cacheManager, TimestampsRegion.class.getName(), prefixRegions );
			createCache( cacheManager, QueryResultsRegion.class.getName(), prefixRegions );
		}
	}

	private TestHelper() {
	}
}
