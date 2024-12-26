/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.scanning;

import org.hibernate.boot.archive.scan.internal.StandardScanner;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.scan.spi.ScanParameters;
import org.hibernate.boot.archive.scan.spi.ScanResult;
import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Emmanuel Bernard
 */
public class CustomScanner implements Scanner {
	public static boolean isUsed = false;

	private final Scanner delegate;

	public CustomScanner(ServiceRegistry serviceRegistry) {
		delegate = new StandardScanner( serviceRegistry );
	}

	public static boolean isUsed() {
		return isUsed;
	}

	public static void resetUsed() {
		isUsed = false;
	}

	@Override
	public ScanResult scan(
			ScanEnvironment environment,
			ScanOptions options,
			ScanParameters parameters) {
		isUsed = true;
		return delegate.scan( environment, options, parameters );
	}
}
