/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.persister.spi.PersisterClassResolver;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Steve Ebersole
 */
public class PersisterClassResolverInitiator implements StandardServiceInitiator<PersisterClassResolver> {
	public static final PersisterClassResolverInitiator INSTANCE = new PersisterClassResolverInitiator();
	public static final String IMPL_NAME = "hibernate.persister.resolver";

	@Override
	public Class<PersisterClassResolver> getServiceInitiated() {
		return PersisterClassResolver.class;
	}

	@Override
	public PersisterClassResolver initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		final Object customImpl = configurationValues.get( IMPL_NAME );
		if ( customImpl == null ) {
			return new StandardPersisterClassResolver();
		}

		if ( customImpl instanceof PersisterClassResolver ) {
			return (PersisterClassResolver) customImpl;
		}

		@SuppressWarnings("unchecked")
		final Class<? extends PersisterClassResolver> customImplClass = customImpl instanceof Class
				? (Class<? extends PersisterClassResolver>) customImpl
				: locate( registry, customImpl.toString() );

		try {
			return customImplClass.newInstance();
		}
		catch (Exception e) {
			throw new ServiceException( "Could not initialize custom PersisterClassResolver impl [" + customImplClass.getName() + "]", e );
		}
	}

	private Class<? extends PersisterClassResolver> locate(ServiceRegistryImplementor registry, String className) {
		return registry.requireService( ClassLoaderService.class ).classForName( className );
	}
}
