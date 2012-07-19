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
package org.hibernate.service.jdbc.connections.internal;

import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.classloading.spi.ClassLoadingException;
import org.hibernate.service.jdbc.connections.spi.DataSourceBasedMultiTenantConnectionProviderImpl;
import org.hibernate.service.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.spi.BasicServiceInitiator;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Steve Ebersole
 */
public class MultiTenantConnectionProviderInitiator implements BasicServiceInitiator<MultiTenantConnectionProvider> {
	public static final MultiTenantConnectionProviderInitiator INSTANCE = new MultiTenantConnectionProviderInitiator();
	private static final Logger log = Logger.getLogger( MultiTenantConnectionProviderInitiator.class );

	@Override
	public Class<MultiTenantConnectionProvider> getServiceInitiated() {
		return MultiTenantConnectionProvider.class;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public MultiTenantConnectionProvider initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final MultiTenancyStrategy strategy = MultiTenancyStrategy.determineMultiTenancyStrategy(  configurationValues );
		if ( strategy == MultiTenancyStrategy.NONE || strategy == MultiTenancyStrategy.DISCRIMINATOR ) {
			// nothing to do, but given the separate hierarchies have to handle this here.
		}

		final Object configValue = configurationValues.get( AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER );
		if ( configValue == null ) {
			// if they also specified the data source *name*, then lets assume they want
			// org.hibernate.service.jdbc.connections.spi.DataSourceBasedMultiTenantConnectionProviderImpl
			final Object dataSourceConfigValue = configurationValues.get( AvailableSettings.DATASOURCE );
			if ( dataSourceConfigValue != null && String.class.isInstance( dataSourceConfigValue ) ) {
				return new DataSourceBasedMultiTenantConnectionProviderImpl();
			}

			return null;
		}

		if ( MultiTenantConnectionProvider.class.isInstance( configValue ) ) {
			return (MultiTenantConnectionProvider) configValue;
		}
		else {
			final Class<MultiTenantConnectionProvider> implClass;
			if ( Class.class.isInstance( configValue ) ) {
				implClass = (Class) configValue;
			}
			else {
				final String className = configValue.toString();
				final ClassLoaderService classLoaderService = registry.getService( ClassLoaderService.class );
				try {
					implClass = classLoaderService.classForName( className );
				}
				catch (ClassLoadingException cle) {
					log.warn( "Unable to locate specified class [" + className + "]", cle );
					throw new ServiceException( "Unable to locate specified multi-tenant connection provider [" + className + "]" );
				}
			}

			try {
				return implClass.newInstance();
			}
			catch (Exception e) {
				log.warn( "Unable to instantiate specified class [" + implClass.getName() + "]", e );
				throw new ServiceException( "Unable to instantiate specified multi-tenant connection provider [" + implClass.getName() + "]" );
			}
		}
	}
}
