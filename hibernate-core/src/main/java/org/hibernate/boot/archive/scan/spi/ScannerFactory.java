/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
	 * Create a scanner
	 * @param archiveDescriptorFactory
	 * @return
	 */
	Scanner getScanner(ArchiveDescriptorFactory archiveDescriptorFactory);
}
