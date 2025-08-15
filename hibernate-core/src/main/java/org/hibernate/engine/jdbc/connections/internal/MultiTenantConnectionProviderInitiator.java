/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.engine.jdbc.connections.spi.DataSourceBasedMultiTenantConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.resource.beans.internal.Helper;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.jboss.logging.Logger;

import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_CONNECTION_PROVIDER;

/**
 * A service initiator for the {@link MultiTenantConnectionProvider} service.
 *
 * @author Steve Ebersole
 */
public class MultiTenantConnectionProviderInitiator implements StandardServiceInitiator<MultiTenantConnectionProvider<?>> {
	private static final Logger log = Logger.getLogger( MultiTenantConnectionProviderInitiator.class );

	/**
	 * Singleton access
	 */
	public static final MultiTenantConnectionProviderInitiator INSTANCE = new MultiTenantConnectionProviderInitiator();

	@Override
	public Class<MultiTenantConnectionProvider<?>> getServiceInitiated() {
		//noinspection unchecked
		return (Class<MultiTenantConnectionProvider<?>>) (Class<?>) MultiTenantConnectionProvider.class;
	}

	@Override
	public MultiTenantConnectionProvider<?> initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		if ( !configurationValues.containsKey( MULTI_TENANT_CONNECTION_PROVIDER ) ) {
			return Helper.getBean(
				Helper.getBeanContainer( registry ),
				MultiTenantConnectionProvider.class,
				true,
				true,
				null
			);
		}

		final Object configValue = configurationValues.get( MULTI_TENANT_CONNECTION_PROVIDER );
		if ( configValue == null ) {
			// if they also specified the data source *name*, then let's assume they want
			// DataSourceBasedMultiTenantConnectionProviderImpl
			final Object dataSourceConfigValue = configurationValues.get( JdbcSettings.DATASOURCE );
			return dataSourceConfigValue instanceof String
					? new DataSourceBasedMultiTenantConnectionProviderImpl<>()
					: null;
		}
		else if ( configValue instanceof MultiTenantConnectionProvider<?> multiTenantConnectionProvider ) {
			return multiTenantConnectionProvider;
		}
		else {
			final var implClass = multiTenantConnectionProviderClass( registry, configValue );
			try {
				return implClass.newInstance();
			}
			catch (Exception e) {
				log.warn( "Unable to instantiate specified class [" + implClass.getName() + "]", e );
				throw new ServiceException( "Unable to instantiate specified multi-tenant connection provider [" + implClass.getName() + "]" );
			}
		}
	}

	private static Class<MultiTenantConnectionProvider<?>> multiTenantConnectionProviderClass(
			ServiceRegistryImplementor registry, Object configValue) {
		if ( configValue instanceof Class ) {
			@SuppressWarnings("unchecked")
			final var clazz = (Class<MultiTenantConnectionProvider<?>>) configValue;
			return clazz;
		}
		else {
			final String className = configValue.toString();
			final var classLoaderService = registry.requireService( ClassLoaderService.class );
			try {
				return classLoaderService.classForName( className );
			}
			catch (ClassLoadingException cle) {
				log.warn( "Unable to locate specified class [" + className + "]", cle );
				throw new ServiceException( "Unable to locate specified multi-tenant connection provider [" + className + "]" );
			}
		}
	}
}
