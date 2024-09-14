/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
