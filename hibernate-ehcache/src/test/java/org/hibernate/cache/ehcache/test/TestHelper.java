/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.ehcache.test;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Stream;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.ehcache.internal.EhcacheRegionFactory;
import org.hibernate.cache.ehcache.test.domain.Event;
import org.hibernate.cache.ehcache.test.domain.Item;
import org.hibernate.cache.ehcache.test.domain.VersionedItem;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.support.RegionNameQualifier;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.tool.schema.Action;

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

	public static SessionFactoryImplementor buildStandardSessionFactory() {
		return buildStandardSessionFactory( ignored -> { } );
	}

	public static SessionFactoryImplementor buildStandardSessionFactory(Consumer<StandardServiceRegistryBuilder> additionalSettings) {
		final StandardServiceRegistryBuilder ssrb = getStandardServiceRegistryBuilder();

		additionalSettings.accept( ssrb );

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

	public static String prefix(String regionName) {
		return RegionNameQualifier.INSTANCE.qualify( "hibernate.test", regionName );
	}

	public static Cache getCache(SessionFactoryImplementor sessionFactoryImplementor, String regionName) {
		final RegionFactory regionFactory = sessionFactoryImplementor.getCache().getRegionFactory();
		final EhcacheRegionFactory ehcacheRegionFactory = (EhcacheRegionFactory) regionFactory;
		final CacheManager cacheManager = ehcacheRegionFactory.getCacheManager();
		regionName = prefix( regionName );
		return cacheManager.getCache( regionName );
	}

	private TestHelper() {
	}
}
