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
import org.hibernate.metamodel.model.creation.spi.RuntimeModelDescriptorFactory;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Steve Ebersole
 */
public class RuntimeModelDescriptorFactoryServiceInitiator implements StandardServiceInitiator<RuntimeModelDescriptorFactory> {
	public static final RuntimeModelDescriptorFactoryServiceInitiator INSTANCE = new RuntimeModelDescriptorFactoryServiceInitiator();

	public static final String LEGACY_IMPL_NAME = "hibernate.persister.factory";
	public static final String IMPL_NAME = "hibernate.model.descriptor_factory";

	@Override
	public Class<RuntimeModelDescriptorFactory> getServiceInitiated() {
		return RuntimeModelDescriptorFactory.class;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public RuntimeModelDescriptorFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final Object customImpl = configurationValues.get( IMPL_NAME );
		if ( customImpl == null ) {
			return new RuntimeModelDescriptorFactoryImpl();
		}

		if ( RuntimeModelDescriptorFactory.class.isInstance( customImpl ) ) {
			return (RuntimeModelDescriptorFactory) customImpl;
		}

		final Class<? extends RuntimeModelDescriptorFactory> customImplClass = Class.class.isInstance( customImpl )
				? ( Class<? extends RuntimeModelDescriptorFactory> ) customImpl
				: locate( registry, customImpl.toString() );
		try {
			return customImplClass.newInstance();
		}
		catch (Exception e) {
			throw new ServiceException( "Could not initialize custom PersisterFactory impl [" + customImplClass.getName() + "]", e );
		}
	}

	private Class<? extends RuntimeModelDescriptorFactory> locate(ServiceRegistryImplementor registry, String className) {
		return registry.getService( ClassLoaderService.class ).classForName( className );
	}
}
