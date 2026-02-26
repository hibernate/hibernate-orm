/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.scan.internal;

import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl;
import org.hibernate.boot.scan.spi.Scanner;
import org.hibernate.boot.scan.spi.ScanningResult;

import java.net.URL;

/// Standard implementation of [Scanner] used in cases
///
/// @author Steve Ebersole
public class StandardScanner implements Scanner {
	public StandardScanner() {
	}

	@Override
	public ScanningResult scan(URL... boundaries) {
		return ScanningResult.NONE;
	}

	@Override
	public ScanningResult jpaScan(ArchiveDescriptor rootArchive, JaxbPersistenceImpl.JaxbPersistenceUnitImpl jaxbUnit) {
		return ScanningResult.NONE;
	}
}
