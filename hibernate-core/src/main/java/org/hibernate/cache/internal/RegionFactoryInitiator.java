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

import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.config.spi.ConfigurationService;
import org.hibernate.service.config.spi.StandardConverters;
import org.hibernate.service.spi.BasicServiceInitiator;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Initiator for the {@link RegionFactory} service.
 *
 * @author Hardy Ferentschik
 */
public class RegionFactoryInitiator implements BasicServiceInitiator<RegionFactory> {
	public static final RegionFactoryInitiator INSTANCE = new RegionFactoryInitiator();
	public static final String DEF_CACHE_REG_FACTORY = NoCachingRegionFactory.class.getName();
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
	@SuppressWarnings( { "unchecked" })
	public RegionFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final Object impl = configurationValues.get( IMPL_NAME );
		boolean isCacheEnabled = isCacheEnabled(registry);
		if(!isCacheEnabled){
			LOG.debugf( "Second level cache has been disabled, so using % as cache region factory", NoCachingRegionFactory.class.getName() );
			return NoCachingRegionFactory.INSTANCE;
		}
		if ( impl == null ) {
			LOG.debugf(
					"No 'hibernate.cache.region.factory_class' is provided, so using %s as default",
					NoCachingRegionFactory.class.getName()
			);
			return NoCachingRegionFactory.INSTANCE;
		}
		LOG.debugf( "Cache region factory : %s", impl.toString() );
		if ( getServiceInitiated().isInstance( impl ) ) {
			return (RegionFactory) impl;
		}

		Class<? extends RegionFactory> customImplClass = null;
		if ( Class.class.isInstance( impl ) ) {
			customImplClass = (Class<? extends RegionFactory>) impl;
		}
		else {
			customImplClass = registry.getService( ClassLoaderService.class )
					.classForName( mapLegacyNames( impl.toString() ) );
		}

		try {
			return customImplClass.newInstance();
		}
		catch ( Exception e ) {
			throw new ServiceException(
					"Could not initialize custom RegionFactory impl [" + customImplClass.getName() + "]", e
			);
		}
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
				StandardConverters.BOOLEAN
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
