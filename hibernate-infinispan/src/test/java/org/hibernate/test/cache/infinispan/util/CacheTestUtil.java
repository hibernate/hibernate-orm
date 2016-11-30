/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.util;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.hibernate.boot.internal.SessionFactoryBuilderImpl;
import org.hibernate.boot.internal.SessionFactoryOptionsImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.boot.ServiceRegistryTestingImpl;

/**
 * Utilities for cache testing.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 */
public class CacheTestUtil {
	@SuppressWarnings("unchecked")
	public static Map buildBaselineSettings(
			String regionPrefix,
			Class regionFactory,
			boolean use2ndLevel,
			boolean useQueries,
			Class<? extends JtaPlatform> jtaPlatform) {
		Map settings = new HashMap();

		settings.put( AvailableSettings.GENERATE_STATISTICS, "true" );
		settings.put( AvailableSettings.USE_STRUCTURED_CACHE, "true" );
		if (jtaPlatform == null) {
			settings.put(Environment.TRANSACTION_COORDINATOR_STRATEGY, JdbcResourceLocalTransactionCoordinatorBuilderImpl.class.getName());
		} else {
			settings.put(Environment.TRANSACTION_COORDINATOR_STRATEGY, JtaTransactionCoordinatorBuilderImpl.class.getName());
			settings.put(AvailableSettings.JTA_PLATFORM, jtaPlatform);
		}
		settings.put( AvailableSettings.CACHE_REGION_FACTORY, regionFactory.getName() );
		settings.put( AvailableSettings.CACHE_REGION_PREFIX, regionPrefix );
		settings.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, String.valueOf( use2ndLevel ) );
		settings.put( AvailableSettings.USE_QUERY_CACHE, String.valueOf( useQueries ) );

		return settings;
	}

	public static StandardServiceRegistryBuilder buildBaselineStandardServiceRegistryBuilder(
			  String regionPrefix,
			  Class regionFactory,
			  boolean use2ndLevel,
			  boolean useQueries,
			  Class<? extends JtaPlatform> jtaPlatform) {
		StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder();

		ssrb.applySettings(
				  buildBaselineSettings( regionPrefix, regionFactory, use2ndLevel, useQueries, jtaPlatform )
		);

		return ssrb;
	}

	public static StandardServiceRegistryBuilder buildCustomQueryCacheStandardServiceRegistryBuilder(
			  String regionPrefix,
			  String queryCacheName,
			  Class<? extends JtaPlatform> jtaPlatform) {
		final StandardServiceRegistryBuilder ssrb = buildBaselineStandardServiceRegistryBuilder(
				  regionPrefix, InfinispanRegionFactory.class, true, true, jtaPlatform
		);
		ssrb.applySetting( InfinispanRegionFactory.QUERY_CACHE_RESOURCE_PROP, queryCacheName );
		return ssrb;
	}

	public static InfinispanRegionFactory createRegionFactory(Class<? extends InfinispanRegionFactory> clazz, Properties properties) {
		try {
			try {
				Constructor<? extends InfinispanRegionFactory> constructor = clazz.getConstructor(Properties.class);
				return constructor.newInstance(properties);
			}
			catch (NoSuchMethodException e) {
				return clazz.newInstance();
			}
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static InfinispanRegionFactory startRegionFactory(ServiceRegistry serviceRegistry) {
		try {
			final ConfigurationService cfgService = serviceRegistry.getService( ConfigurationService.class );
			final Properties properties = toProperties( cfgService.getSettings() );

			String factoryType = cfgService.getSetting( AvailableSettings.CACHE_REGION_FACTORY, StandardConverters.STRING );
			Class clazz = Thread.currentThread().getContextClassLoader().loadClass( factoryType );
			InfinispanRegionFactory regionFactory;
			if (clazz == InfinispanRegionFactory.class) {
				regionFactory = new TestInfinispanRegionFactory(properties);
			}
			else {
				if (InfinispanRegionFactory.class.isAssignableFrom(clazz)) {
					regionFactory = createRegionFactory(clazz, properties);
				} else {
					throw new IllegalArgumentException(clazz + " is not InfinispanRegionFactory");
				}
			}

			final SessionFactoryOptionsImpl sessionFactoryOptions = new SessionFactoryOptionsImpl(
					  new SessionFactoryBuilderImpl.SessionFactoryOptionsStateStandardImpl(
								 (StandardServiceRegistry) serviceRegistry
					  )
			);

			regionFactory.start( sessionFactoryOptions, properties );

			return regionFactory;
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static InfinispanRegionFactory startRegionFactory(
			  ServiceRegistry serviceRegistry,
			  CacheTestSupport testSupport) {
		InfinispanRegionFactory factory = startRegionFactory( serviceRegistry );
		testSupport.registerFactory( factory );
		return factory;
	}

	public static void stopRegionFactory(
			  InfinispanRegionFactory factory,
			  CacheTestSupport testSupport) {
		testSupport.unregisterFactory( factory );
		factory.stop();
	}

	public static Properties toProperties(Map map) {
		if ( map == null ) {
			return null;
		}

		if ( map instanceof Properties ) {
			return (Properties) map;
		}

		Properties properties = new Properties();
		properties.putAll( map );
		return properties;
	}

	public static SessionFactoryOptions sfOptionsForStart() {
		return new SessionFactoryOptionsImpl(
				new SessionFactoryBuilderImpl.SessionFactoryOptionsStateStandardImpl(
						ServiceRegistryTestingImpl.forUnitTesting()
				)
		);
	}

	/**
	 * Prevent instantiation.
	 */
	private CacheTestUtil() {
	}
}
