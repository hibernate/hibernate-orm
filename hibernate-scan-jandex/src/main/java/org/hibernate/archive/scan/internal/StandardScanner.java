/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.archive.scan.internal;

import org.hibernate.archive.scan.spi.AbstractScannerImpl;
import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;

/**
 * Standard implementation of the Scanner contract, supporting typical archive walking support where
 * the urls we are processing can be treated using normal file handling.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public class StandardScanner extends AbstractScannerImpl {
	public StandardScanner() {
		this( StandardArchiveDescriptorFactory.INSTANCE );
	}

	public StandardScanner(ArchiveDescriptorFactory value) {
		super( value );
	}
}
