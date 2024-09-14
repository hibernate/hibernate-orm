/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot;

import java.io.InputStream;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Abstraction for locating class-path resources
 *
 * @see ResourceLocator
 *
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface ResourceStreamLocator {

	/**
	 * Locate the named resource
	 *
	 * @param resourceName The resource name to locate
	 *
	 * @return The located resource's InputStream, or {@code null} if no match found
	 */
	@Nullable InputStream locateResourceStream(String resourceName);
}
