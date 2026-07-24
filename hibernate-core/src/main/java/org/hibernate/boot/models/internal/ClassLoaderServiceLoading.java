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
import org.hibernate.models.UnknownClassException;

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
		try {
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
		catch (ClassLoadingException e) {
			if ( isMissingClass( e.getCause() ) ) {
				throw new UnknownClassException( e.getMessage(), e );
			}
			throw e;
		}
	}

	private static boolean isMissingClass(Throwable failure) {
		if ( !(failure instanceof ClassNotFoundException) ) {
			return false;
		}
		if ( failure.getCause() != null && !isMissingClass( failure.getCause() ) ) {
			return false;
		}
		// The aggregated loader retains failures from individual loaders as suppressed exceptions.
		for ( var suppressed : failure.getSuppressed() ) {
			if ( !isMissingClass( suppressed ) ) {
				return false;
			}
		}
		return true;
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
