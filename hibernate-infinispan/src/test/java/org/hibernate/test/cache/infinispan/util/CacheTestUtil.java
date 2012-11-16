/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat, Inc. and/or it's affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc. and/or it's affiliates.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.cache.infinispan.util;

import java.util.Properties;

import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.Settings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.test.cache.infinispan.functional.SingleNodeTestCase;

/**
 * Utilities for cache testing.
 *
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 */
public class CacheTestUtil {

	public static Configuration buildConfiguration(String regionPrefix,
												   Class regionFactory, boolean use2ndLevel, boolean useQueries) {
		Configuration cfg = new Configuration();
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
		cfg.setProperty( Environment.USE_STRUCTURED_CACHE, "true" );
		cfg.setProperty( AvailableSettings.JTA_PLATFORM, BatchModeJtaPlatform.class.getName() );

		cfg.setProperty( Environment.CACHE_REGION_FACTORY, regionFactory.getName() );
		cfg.setProperty( Environment.CACHE_REGION_PREFIX, regionPrefix );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, String.valueOf( use2ndLevel ) );
		cfg.setProperty( Environment.USE_QUERY_CACHE, String.valueOf( useQueries ) );

		return cfg;
	}

	public static InfinispanRegionFactory startRegionFactory(ServiceRegistry serviceRegistry,
															 Configuration cfg, CacheTestSupport testSupport) {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) cfg.buildSessionFactory( serviceRegistry );
		InfinispanRegionFactory factory = (InfinispanRegionFactory) sessionFactory.getServiceRegistry()
				.getService( RegionFactory.class );
		testSupport.registerFactory( factory, sessionFactory );
		return factory;
	}

	public static void stopRegionFactory(InfinispanRegionFactory factory,
										 CacheTestSupport testSupport) {
		if ( factory != null ) {
			testSupport.unregisterFactory( factory ).close();
		}
	}

	/**
	 * Prevent instantiation.
	 */
	private CacheTestUtil() {
	}

}
