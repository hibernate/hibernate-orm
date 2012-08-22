/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.cache.internal;

import org.jboss.logging.Logger;

import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.config.spi.ConfigurationService;
import org.hibernate.service.config.spi.StandardConverters;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;

/**
 * Initiator for the {@link RegionFactory} service.
 *
 * @author Hardy Ferentschik
 */
public class RegionFactoryInitiator implements SessionFactoryServiceInitiator<RegionFactory> {
	public static final RegionFactoryInitiator INSTANCE = new RegionFactoryInitiator();
	private static final String DEFAULT_IMPL = NoCachingRegionFactory.class.getName();
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			RegionFactoryInitiator.class.getName()
	);

	/**
	 * Property name to use to configure the full qualified class name for the {@code RegionFactory}
	 */
	public static final String IMPL_NAME = "hibernate.cache.region.factory_class";

	@Override
	public Class<RegionFactory> getServiceInitiated() {
		return RegionFactory.class;
	}

	@Override
	public RegionFactory initiateService(SessionFactoryImplementor sessionFactory, Configuration configuration, ServiceRegistryImplementor registry) {
		return initiateService(sessionFactory, registry);
	}

	@Override
	public RegionFactory initiateService(SessionFactoryImplementor sessionFactory, MetadataImplementor metadata, ServiceRegistryImplementor registry) {
		return initiateService(sessionFactory, registry);
	}

	private RegionFactory initiateService(SessionFactoryImplementor sessionFactory, ServiceRegistryImplementor registry){
		final Object impl = registry.getService( ConfigurationService.class ).getSettings().get( IMPL_NAME );
		boolean isCacheEnabled = isCacheEnabled( registry );
		RegionFactory factory;
		if ( !isCacheEnabled ) {
			LOG.debugf(
					"Second level cache has been disabled, so using % as cache region factory",
					NoCachingRegionFactory.class.getName()
			);
			factory = NoCachingRegionFactory.INSTANCE;
		}
		else if ( impl == null ) {
			LOG.debugf(
					"No 'hibernate.cache.region.factory_class' is provided, so using %s as default",
					NoCachingRegionFactory.class.getName()
			);
			factory = NoCachingRegionFactory.INSTANCE;
		}
		else {
			LOG.debugf( "Cache region factory : %s", impl.toString() );
			if ( getServiceInitiated().isInstance( impl ) ) {
				factory = (RegionFactory) impl;
			}
			else {
				Class<? extends RegionFactory> customImplClass = null;
				if ( Class.class.isInstance( impl ) ) {
					customImplClass = (Class<? extends RegionFactory>) impl;
				}
				else {
					customImplClass = registry.getService( ClassLoaderService.class )
							.classForName( mapLegacyNames( impl.toString() ) );
				}

				try {
					factory = customImplClass.newInstance();
				}
				catch ( Exception e ) {
					throw new ServiceException(
							"Could not initialize custom RegionFactory impl [" + customImplClass.getName() + "]", e
					);
				}
			}
		}

		return  factory;
	}

	private static boolean isCacheEnabled(ServiceRegistryImplementor serviceRegistry) {
		final ConfigurationService configurationService = serviceRegistry.getService( ConfigurationService.class );
		final boolean useSecondLevelCache = configurationService.getSetting(
				AvailableSettings.USE_SECOND_LEVEL_CACHE,
				StandardConverters.BOOLEAN,
				true
		);
		final boolean useQueryCache = configurationService.getSetting(
				AvailableSettings.USE_QUERY_CACHE,
				StandardConverters.BOOLEAN,
				false
		);
		return useSecondLevelCache || useQueryCache;
	}

	// todo this shouldn't be public (nor really static):
	// hack for org.hibernate.cfg.SettingsFactory.createRegionFactory()
	public static String mapLegacyNames(final String name) {
		if ( "org.hibernate.cache.EhCacheRegionFactory".equals( name ) ) {
			return "org.hibernate.cache.ehcache.EhCacheRegionFactory";
		}

		if ( "org.hibernate.cache.SingletonEhCacheRegionFactory".equals( name ) ) {
			return "org.hibernate.cache.ehcache.SingletonEhCacheRegionFactory";
		}

		return name;
	}
}
