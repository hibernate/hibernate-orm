/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot;

import java.net.URL;

/**
 * Abstraction for locating class-path resources
 *
 * @see ResourceStreamLocator
 *
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface ResourceLocator {

	/**
	 * Locate the named resource
	 *
	 * @param resourceName The resource name to locate
	 *
	 * @return The located URL, or {@code null} if no match found
	 */
	URL locateResource(String resourceName);
}
