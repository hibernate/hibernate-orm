/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.scan.spi;

import org.hibernate.Incubating;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.service.JavaServiceLoadable;
import org.hibernate.service.Service;

/**
 * Support for scanning various sources to detect {@code managed resources}
 * for a {@code persistence unit}.
 *
 */
@Incubating
@JavaServiceLoadable
public interface ScannerFactory extends Service {
	/**
	 * Create a scanner.
	 */
	Scanner getScanner(ArchiveDescriptorFactory archiveDescriptorFactory);
}
