/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	public String getName();

	/**
	 * Retrieves access to the InputStream for the {@code package-info.class} file.
	 *
	 * @return Access to the InputStream for the {@code package-info.class} file.
	 */
	public InputStreamAccess getStreamAccess();
}
