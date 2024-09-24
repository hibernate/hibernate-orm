/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.scan.spi;

import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;

/**
 * Defines the contract for Hibernate to be able to scan for classes, packages and resources inside a
 * persistence unit.
 * <p>
 * Constructors are expected in one of 2 forms:<ul>
 *     <li>no-arg</li>
 *     <li>single arg, of type {@link org.hibernate.boot.archive.spi.ArchiveDescriptorFactory}</li>
 * </ul>
 * <p>
 * If a ArchiveDescriptorFactory is specified in the configuration, but the Scanner
 * to be used does not accept a ArchiveDescriptorFactory an exception will be thrown.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public interface Scanner {
	/**
	 * Perform the scanning against the described environment using the
	 * defined options, and return the scan results.
	 *
	 * @param environment The scan environment.
	 * @param options The options to control the scanning.
	 * @param params The parameters for scanning
	 */
	ScanResult scan(ScanEnvironment environment, ScanOptions options, ScanParameters params);

	default void setArchiveDescriptorFactory(ArchiveDescriptorFactory archiveDescriptorFactory){
		throw new UnsupportedOperationException();
	}
}
