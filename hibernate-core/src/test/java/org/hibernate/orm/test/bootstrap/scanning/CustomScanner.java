/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.scanning;

import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl;
import org.hibernate.boot.scan.internal.StandardScanner;
import org.hibernate.boot.scan.spi.Scanner;
import org.hibernate.boot.scan.spi.ScanningResult;

import java.net.URL;

/**
 * @author Emmanuel Bernard
 */
public class CustomScanner implements Scanner {
	public static boolean isUsed = false;
	private Scanner delegate = new StandardScanner();

	public static boolean isUsed() {
		return isUsed;
	}

	public static void resetUsed() {
		isUsed = false;
	}

	@Override
	public ScanningResult scan(URL... boundaries) {
		isUsed = true;
		return delegate.scan( boundaries );
	}

	@Override
	public ScanningResult jpaScan(ArchiveDescriptor archiveDescriptor, JaxbPersistenceImpl.JaxbPersistenceUnitImpl jaxbUnit) {
		isUsed = true;
		return delegate.jpaScan(  archiveDescriptor, jaxbUnit );
	}
}
