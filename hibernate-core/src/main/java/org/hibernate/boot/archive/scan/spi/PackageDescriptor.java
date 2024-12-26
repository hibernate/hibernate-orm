/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.scan.spi;

import org.hibernate.boot.archive.spi.InputStreamAccess;

/**
 * Descriptor for a package (as indicated by a package-info.class file).
 *
 * @author Steve Ebersole
 */
public interface PackageDescriptor {
	/**
	 * Retrieves the package name.
	 *
	 * @return The package name
	 */
	String name();

	/**
	 * Retrieves access to the InputStream for the {@code package-info.class} file.
	 *
	 * @return Access to the InputStream for the {@code package-info.class} file.
	 */
	InputStreamAccess streamAccess();
}
