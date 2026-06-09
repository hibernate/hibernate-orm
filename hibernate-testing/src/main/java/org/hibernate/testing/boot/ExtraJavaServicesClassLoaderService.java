/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.boot;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.jspecify.annotations.NonNull;

/**
 * @author Steve Ebersole
 */
public class ExtraJavaServicesClassLoaderService extends ClassLoaderServiceImpl {
	private final List<JavaServiceDescriptor<?>> extraJavaServices;

	public ExtraJavaServicesClassLoaderService(List<JavaServiceDescriptor<?>> extraJavaServices) {
		this.extraJavaServices = extraJavaServices;
	}

	@Override
	public <S> @NonNull Collection<S> loadJavaServices(@NonNull Class<S> serviceContract) {
		final var baseServices = super.loadJavaServices( serviceContract );
		final List<S> services = new ArrayList<>( baseServices );
		applyExtraJavaServices( serviceContract, services );
		return services;
	}

	private <S> void applyExtraJavaServices(Class<S> serviceContract, List<S> services) {
		extraJavaServices.forEach(
				javaServiceDescriptor -> {
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

	public record JavaServiceDescriptor<ROLE>(Class<ROLE> role, Class<? extends ROLE> impl) {
	}
}
