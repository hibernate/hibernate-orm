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
import org.hibernate.metamodel.model.creation.spi.RuntimeModelDescriptorClassResolver;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Steve Ebersole
 */
public class PersisterClassResolverInitiator implements StandardServiceInitiator<RuntimeModelDescriptorClassResolver> {
	public static final PersisterClassResolverInitiator INSTANCE = new PersisterClassResolverInitiator();
	public static final String IMPL_NAME = "hibernate.persister.resolver";

	@Override
	public Class<RuntimeModelDescriptorClassResolver> getServiceInitiated() {
		return RuntimeModelDescriptorClassResolver.class;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public RuntimeModelDescriptorClassResolver initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final Object customImpl = configurationValues.get( IMPL_NAME );
		if ( customImpl == null ) {
			return new StandardRuntimeModelDescriptorClassResolver();
		}

		if ( RuntimeModelDescriptorClassResolver.class.isInstance( customImpl ) ) {
			return (RuntimeModelDescriptorClassResolver) customImpl;
		}

		final Class<? extends RuntimeModelDescriptorClassResolver> customImplClass = Class.class.isInstance( customImpl )
				? (Class<? extends RuntimeModelDescriptorClassResolver>) customImpl
				: locate( registry, customImpl.toString() );

		try {
			return customImplClass.newInstance();
		}
		catch (Exception e) {
			throw new ServiceException( "Could not initialize custom PersisterClassResolver impl [" + customImplClass.getName() + "]", e );
		}
	}

	private Class<? extends RuntimeModelDescriptorClassResolver> locate(ServiceRegistryImplementor registry, String className) {
		return registry.getService( ClassLoaderService.class ).classForName( className );
	}
}
