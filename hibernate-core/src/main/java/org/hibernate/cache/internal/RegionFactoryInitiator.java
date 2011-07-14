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

import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.service.classloading.spi.ClassLoaderService;
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
		if ( impl == null ) {
			return new NoCachingRegionFactory();
		}

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
