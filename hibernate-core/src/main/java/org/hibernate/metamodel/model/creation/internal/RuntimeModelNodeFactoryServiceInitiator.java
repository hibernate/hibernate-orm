/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.creation.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelNodeFactory;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Steve Ebersole
 */
public class RuntimeModelNodeFactoryServiceInitiator implements StandardServiceInitiator<RuntimeModelNodeFactory> {
	public static final RuntimeModelNodeFactoryServiceInitiator INSTANCE = new RuntimeModelNodeFactoryServiceInitiator();

	public static final String IMPL_NAME = "hibernate.persister.factory";

	@Override
	public Class<RuntimeModelNodeFactory> getServiceInitiated() {
		return RuntimeModelNodeFactory.class;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public RuntimeModelNodeFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final Object customImpl = configurationValues.get( IMPL_NAME );
		if ( customImpl == null ) {
			return new RuntimeModelNodeFactoryImpl();
		}

		if ( RuntimeModelNodeFactory.class.isInstance( customImpl ) ) {
			return (RuntimeModelNodeFactory) customImpl;
		}

		final Class<? extends RuntimeModelNodeFactory> customImplClass = Class.class.isInstance( customImpl )
				? ( Class<? extends RuntimeModelNodeFactory> ) customImpl
				: locate( registry, customImpl.toString() );
		try {
			return customImplClass.newInstance();
		}
		catch (Exception e) {
			throw new ServiceException( "Could not initialize custom PersisterFactory impl [" + customImplClass.getName() + "]", e );
		}
	}

	private Class<? extends RuntimeModelNodeFactory> locate(ServiceRegistryImplementor registry, String className) {
		return registry.getService( ClassLoaderService.class ).classForName( className );
	}
}
