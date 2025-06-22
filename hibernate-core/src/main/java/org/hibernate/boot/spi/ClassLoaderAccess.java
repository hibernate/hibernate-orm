/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import java.net.URL;

/**
 * During the process of building the metamodel, access to the {@link ClassLoader} is
 * strongly discouraged. However, sometimes it is needed. This contract helps mitigate
 * access to the {@link ClassLoader} in these cases.
 *
 * @author Steve Ebersole
 *
 * @since 5.0
 */
public interface ClassLoaderAccess {
	/**
	 * Obtain a {@link Class} reference by name
	 *
	 * @param name The name of the class
	 * @return The {@code Class} object with the given name
	 */
	<T> Class<T> classForName(String name);

	/**
	 * Locate a resource by name
	 *
	 * @param resourceName The name of the resource to resolve
	 * @return The located resource;
	 *         may return {@code null} to indicate the resource was not found
	 */
	URL locateResource(String resourceName);
}
