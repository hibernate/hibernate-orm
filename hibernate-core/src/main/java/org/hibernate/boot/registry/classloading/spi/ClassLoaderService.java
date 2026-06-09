/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.registry.classloading.spi;

import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.boot.ResourceLocator;
import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.service.Service;
import org.hibernate.service.spi.Stoppable;

/**
 * A service for interacting with class loaders.
 *
 * @author Steve Ebersole
 */
public interface ClassLoaderService extends ResourceLocator, ResourceStreamLocator, Service, Stoppable {
	/**
	 * Locate a class by name.
	 *
	 * @param className The name of the class to locate
	 * @param <T> The returned class type.
	 *
	 * @return The class reference
	 *
	 * @throws ClassLoadingException Indicates the class could not be found
	 */
	@Nonnull
	<T> Class<T> classForName(@Nonnull String className);

	@SuppressWarnings("unchecked")
	@Nonnull
	default <T> Class<T> classForTypeName(@Nonnull String className) {
		return (Class<T>) switch ( className ) {
			case "boolean" -> boolean.class;
			case "byte" -> byte.class;
			case "char" -> char.class;
			case "short" -> short.class;
			case "int" -> int.class;
			case "float" -> float.class;
			case "long" -> long.class;
			case "double" -> double.class;
			default -> classForName( className );
		};
	}

	/**
	 * Locate a resource by name (classpath lookup).
	 *
	 * @param name The resource name.
	 *
	 * @return The located URL; may return {@code null} to indicate the resource was not found
	 */
	@Nullable URL locateResource(@Nonnull String name);

	/**
	 * Locate a resource by name (classpath lookup) and gets its stream.
	 *
	 * @param name The resource name.
	 *
	 * @return The stream of the located resource; may return {@code null} to indicate the resource was not found
	 */
	@Nullable InputStream locateResourceStream(@Nonnull String name);

	/**
	 * Locate a series of resource by name (classpath lookup).
	 *
	 * @param name The resource name.
	 *
	 * @return The list of URL matching; may return an empty list to indicate the resource was not found
	 */
	@Nonnull  List<URL> locateResources(@Nonnull String name);

	/**
	 * Discovers and instantiates implementations of the named service contract.
	 *
	 * @apiNote The term "service" here does not refer to a {@link Service}.
	 *          Here it refers to a Java {@link java.util.ServiceLoader}.
	 *
	 * @param serviceContract The java type defining the service contract
	 * @param <S> The type of the service contract
	 *
	 * @return The ordered set of discovered services.
	 *
	 * @see org.hibernate.service.JavaServiceLoadable
	 */
	@Nonnull
	<S> Collection<S> loadJavaServices(@Nonnull Class<S> serviceContract);

	@Nonnull
	<T> T generateProxy(@Nonnull InvocationHandler handler, @Nonnull Class<?>... interfaces);

	/**
	 * Loading a Package from the ClassLoader.
	 *
	 * @return The Package.  {@code null} if no such Package is found, or if the
	 * ClassLoader call leads to an exception ({@link LinkageError}, e.g.).
	 */
	@Nullable
	Package packageForNameOrNull(@Nonnull String packageName);

	interface Work<T> {
		T doWork(@Nonnull ClassLoader classLoader);
	}

	<T> T workWithClassLoader(@Nonnull Work<T> work);
}
