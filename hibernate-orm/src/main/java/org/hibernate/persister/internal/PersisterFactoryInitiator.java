/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	@SuppressWarnings( {"unchecked"})
	public PersisterFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final Object customImpl = configurationValues.get( IMPL_NAME );
		if ( customImpl == null ) {
			return new PersisterFactoryImpl();
		}

		if ( PersisterFactory.class.isInstance( customImpl ) ) {
			return (PersisterFactory) customImpl;
		}

		final Class<? extends PersisterFactory> customImplClass = Class.class.isInstance( customImpl )
				? ( Class<? extends PersisterFactory> ) customImpl
				: locate( registry, customImpl.toString() );
		try {
			return customImplClass.newInstance();
		}
		catch (Exception e) {
			throw new ServiceException( "Could not initialize custom PersisterFactory impl [" + customImplClass.getName() + "]", e );
		}
	}

	private Class<? extends PersisterFactory> locate(ServiceRegistryImplementor registry, String className) {
		return registry.getService( ClassLoaderService.class ).classForName( className );
	}
}
