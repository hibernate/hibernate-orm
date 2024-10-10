/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.DataSourceBasedMultiTenantConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.internal.Helper;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.jboss.logging.Logger;

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
		if ( !configurationValues.containsKey( AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER ) ) {
			final BeanContainer beanContainer = Helper.allowExtensionsInCdi( registry ) ? registry.requireService( ManagedBeanRegistry.class ).getBeanContainer() : null;
			if (beanContainer != null) {
				return beanContainer.getBean(
						MultiTenantConnectionProvider.class,
						new BeanContainer.LifecycleOptions() {
							@Override
							public boolean canUseCachedReferences() {
								return true;
							}

							@Override
							public boolean useJpaCompliantCreation() {
								return true;
							}
						},
						new BeanInstanceProducer() {

							@Override
							public <B> B produceBeanInstance(Class<B> beanType) {
								return null;
							}

							@Override
							public <B> B produceBeanInstance(String name, Class<B> beanType) {
								return null;
							}

						}
				).getBeanInstance();
			}
			return null;
		}

		final Object configValue = configurationValues.get( AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER );
		if ( configValue == null ) {
			// if they also specified the data source *name*, then lets assume they want
			// DataSourceBasedMultiTenantConnectionProviderImpl
			final Object dataSourceConfigValue = configurationValues.get( AvailableSettings.DATASOURCE );
			if ( dataSourceConfigValue instanceof String ) {
				return new DataSourceBasedMultiTenantConnectionProviderImpl<>();
			}

			return null;
		}

		if ( configValue instanceof MultiTenantConnectionProvider<?> ) {
			return (MultiTenantConnectionProvider<?>) configValue;
		}
		else {
			final Class<MultiTenantConnectionProvider<?>> implClass;
			if ( configValue instanceof Class ) {
				@SuppressWarnings("unchecked")
				Class<MultiTenantConnectionProvider<?>> clazz = (Class<MultiTenantConnectionProvider<?>>) configValue;
				implClass = clazz;
			}
			else {
				final String className = configValue.toString();
				final ClassLoaderService classLoaderService = registry.requireService( ClassLoaderService.class );
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
