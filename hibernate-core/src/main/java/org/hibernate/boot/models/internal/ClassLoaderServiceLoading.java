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
	@SuppressWarnings("unchecked")
	public <T> Class<T> classForName(String name) {
		return (Class<T>) switch ( name ) {
			case "void" -> void.class;
			case "boolean" -> boolean.class;
			case "byte" -> byte.class;
			case "char" -> char.class;
			case "short" -> short.class;
			case "int" -> int.class;
			case "float" -> float.class;
			case "long" -> long.class;
			case "double" -> double.class;
			default -> classLoaderService.classForName( name );
		};
	}

	@Override
	public <T> Class<T> findClassForName(String name) {
		try {
			return classForName( name );
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
