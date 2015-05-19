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
	@SuppressWarnings( {"unchecked"})
	public PersisterClassResolver initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final Object customImpl = configurationValues.get( IMPL_NAME );
		if ( customImpl == null ) {
			return new StandardPersisterClassResolver();
		}

		if ( PersisterClassResolver.class.isInstance( customImpl ) ) {
			return (PersisterClassResolver) customImpl;
		}

		final Class<? extends PersisterClassResolver> customImplClass = Class.class.isInstance( customImpl )
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
		return registry.getService( ClassLoaderService.class ).classForName( className );
	}
}
