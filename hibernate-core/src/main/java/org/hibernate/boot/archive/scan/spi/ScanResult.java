/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	Set<PackageDescriptor> getLocatedPackages();

	/**
	 * Returns descriptors for all classes discovered as part of the scan
	 *
	 * @return Descriptors for discovered classes
	 */
	Set<ClassDescriptor> getLocatedClasses();

	/**
	 * Returns descriptors for all mapping files discovered as part of the scan
	 *
	 * @return Descriptors for discovered mapping files
	 */
	Set<MappingFileDescriptor> getLocatedMappingFiles();
}
