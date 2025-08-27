/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
