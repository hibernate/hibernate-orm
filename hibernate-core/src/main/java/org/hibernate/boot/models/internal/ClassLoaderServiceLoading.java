/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.internal;

import java.net.URL;
import java.util.Collection;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.models.spi.ClassLoading;

/**
 * Adapts {@linkplain ClassLoaderService} to the {@linkplain ClassLoading} contract
 *
 * @author Steve Ebersole
 */
public class ClassLoaderServiceLoading implements ClassLoading {
	private final ClassLoaderService classLoaderService;

	public ClassLoaderServiceLoading(ClassLoaderService classLoaderService) {
		this.classLoaderService = classLoaderService;
	}

	@Override
	public <T> Class<T> classForName(String name) {
		return classLoaderService.classForName( name );
	}

	@Override
	public <T> Class<T> findClassForName(String name) {
		try {
			return classLoaderService.classForName( name );
		}
		catch (ClassLoadingException e) {
			return null;
		}
	}

	@Override
	public URL locateResource(String resourceName) {
		return classLoaderService.locateResource( resourceName );
	}

	@Override
	public <S> Collection<S> loadJavaServices(Class<S> serviceType) {
		return classLoaderService.loadJavaServices( serviceType );
	}
}
