/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.registry.classloading.spi;

import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import org.hibernate.service.Service;
import org.hibernate.service.spi.Stoppable;

/**
 * A service for interacting with class loaders.
 *
 * @author Steve Ebersole
 */
public interface ClassLoaderService extends Service, Stoppable {
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
	 * <p/>
	 * NOTE : the terms service here is used differently than {@link Service}.  Instead here we are talking about
	 * services as defined by {@link java.util.ServiceLoader}.
	 *
	 * @param serviceContract The java type defining the service contract
	 * @param <S> The type of the service contract
	 *     
	 * @return The ordered set of discovered services.
	 */
	<S> Collection<S> loadJavaServices(Class<S> serviceContract);

	<T> T generateProxy(InvocationHandler handler, Class... interfaces);

	interface Work<T> {
		T doWork(ClassLoader classLoader);
	}

	<T> T workWithClassLoader(Work<T> work);
}
