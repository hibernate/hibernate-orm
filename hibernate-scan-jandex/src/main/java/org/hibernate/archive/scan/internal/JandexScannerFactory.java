/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.archive.scan.internal;

import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.boot.archive.scan.spi.ScannerFactory;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.service.ServiceRegistry;

public class JandexScannerFactory implements ScannerFactory {
	@Override
	public Scanner createScanner(ArchiveDescriptorFactory archiveDescriptorFactory, ServiceRegistry serviceRegistry) {
		if ( archiveDescriptorFactory == null ) {
			return new JandexScanner();
		}
		return new JandexScanner( archiveDescriptorFactory );
	}
}
