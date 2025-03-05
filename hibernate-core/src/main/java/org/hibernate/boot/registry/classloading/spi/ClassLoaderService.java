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
	<T> Class<T> classForName(String className);

	@SuppressWarnings("unchecked")
	default <T> Class<T> classForTypeName(String className) {
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
	URL locateResource(String name);

	/**
	 * Locate a resource by name (classpath lookup) and gets its stream.
	 *
	 * @param name The resource name.
	 *
	 * @return The stream of the located resource; may return {@code null} to indicate the resource was not found
	 */
	InputStream locateResourceStream(String name);

	/**
	 * Locate a series of resource by name (classpath lookup).
	 *
	 * @param name The resource name.
	 *
	 * @return The list of URL matching; may return {@code null} to indicate the resource was not found
	 */
	List<URL> locateResources(String name);

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
	<S> Collection<S> loadJavaServices(Class<S> serviceContract);

	<T> T generateProxy(InvocationHandler handler, Class<?>... interfaces);

	/**
	 * Loading a Package from the ClassLoader.
	 *
	 * @return The Package.  {@code null} if no such Package is found, or if the
	 * ClassLoader call leads to an exception ({@link LinkageError}, e.g.).
	 */
	Package packageForNameOrNull(String packageName);

	interface Work<T> {
		T doWork(ClassLoader classLoader);
	}

	<T> T workWithClassLoader(Work<T> work);
}
