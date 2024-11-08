/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.archive.scan.internal;

import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.boot.archive.scan.spi.ScannerFactory;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;

public class StandardScannerFactory implements ScannerFactory {
	@Override
	public Scanner getScanner(ArchiveDescriptorFactory archiveDescriptorFactory) {
		if ( archiveDescriptorFactory == null ) {
			return new StandardScanner();
		}
		return new StandardScanner( archiveDescriptorFactory );
	}
}
