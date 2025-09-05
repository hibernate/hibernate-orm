/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.persister.spi.PersisterFactory;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Steve Ebersole
 */
public class PersisterFactoryInitiator implements StandardServiceInitiator<PersisterFactory> {
	public static final PersisterFactoryInitiator INSTANCE = new PersisterFactoryInitiator();

	public static final String IMPL_NAME = "hibernate.persister.factory";

	@Override
	public Class<PersisterFactory> getServiceInitiated() {
		return PersisterFactory.class;
	}

	@Override
	public PersisterFactory initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		final Object customImpl = configurationValues.get( IMPL_NAME );
		if ( customImpl == null ) {
			return new PersisterFactoryImpl();
		}

		if ( customImpl instanceof PersisterFactory persisterFactory ) {
			return persisterFactory;
		}

		@SuppressWarnings("unchecked")
		final var customImplClass =
				customImpl instanceof Class
						? (Class<? extends PersisterFactory>) customImpl
						: locate( registry, customImpl.toString() );
		try {
			return customImplClass.newInstance();
		}
		catch (Exception e) {
			throw new ServiceException( "Could not initialize custom PersisterFactory impl [" + customImplClass.getName() + "]", e );
		}
	}

	private Class<? extends PersisterFactory> locate(ServiceRegistryImplementor registry, String className) {
		return registry.requireService( ClassLoaderService.class ).classForName( className );
	}
}
