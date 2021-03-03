/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.boot;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;

/**
 * @author Steve Ebersole
 */
public class ExtraJavaServicesClassLoaderService extends ClassLoaderServiceImpl {
	private final List<JavaServiceDescriptor<?>> extraJavaServices;

	public ExtraJavaServicesClassLoaderService(List<JavaServiceDescriptor<?>> extraJavaServices) {
		this.extraJavaServices = extraJavaServices;
	}

	@Override
	public <S> Collection<S> loadJavaServices(Class<S> serviceContract) {
		final Collection<S> baseServices = super.loadJavaServices( serviceContract );
		final List<S> services = new ArrayList<>( baseServices );

		applyExtraJavaServices( serviceContract, services );

		return services;
	}

	private <S> void applyExtraJavaServices(Class<S> serviceContract, List<S> services) {
		extraJavaServices.forEach(
				(javaServiceDescriptor) -> {
					if ( serviceContract.isAssignableFrom( javaServiceDescriptor.role ) ) {
						try {
							final Object serviceInstance = javaServiceDescriptor.impl.getDeclaredConstructor().newInstance();
							//noinspection unchecked
							services.add( (S) serviceInstance );
						}
						catch (NoSuchMethodException | IllegalAccessException e) {
							throw new RuntimeException( "Unable to access constructor for specified 'extra' Java service : " + javaServiceDescriptor.impl.getName(), e );
						}
						catch (InstantiationException | InvocationTargetException e) {
							throw new RuntimeException( "Unable to instantiate specified 'extra' Java service : " + javaServiceDescriptor.impl.getName(), e );
						}
					}
				}
		);
	}

	public static class JavaServiceDescriptor<ROLE> {
		private final Class<ROLE> role;
		private final Class<? extends ROLE> impl;

		public JavaServiceDescriptor(Class<ROLE> role, Class<? extends ROLE> impl) {
			this.role = role;
			this.impl = impl;
		}

		public Class<ROLE> getRole() {
			return role;
		}

		public Class<? extends ROLE> getImpl() {
			return impl;
		}
	}
}
