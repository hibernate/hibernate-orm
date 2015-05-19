/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.scan.spi;

import java.util.Set;

/**
 * Defines the result of scanning
 *
 * @author Steve Ebersole
 */
public interface ScanResult {
	/**
	 * Returns descriptors for all packages discovered as part of the scan
	 *
	 * @return Descriptors for discovered packages
	 */
	public Set<PackageDescriptor> getLocatedPackages();

	/**
	 * Returns descriptors for all classes discovered as part of the scan
	 *
	 * @return Descriptors for discovered classes
	 */
	public Set<ClassDescriptor> getLocatedClasses();

	/**
	 * Returns descriptors for all mapping files discovered as part of the scan
	 *
	 * @return Descriptors for discovered mapping files
	 */
	public Set<MappingFileDescriptor> getLocatedMappingFiles();
}
