/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.scan.spi;

import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.service.Service;

public interface ScannerFactory extends Service {
	Scanner getScanner(ArchiveDescriptorFactory archiveDescriptorFactory);
}
