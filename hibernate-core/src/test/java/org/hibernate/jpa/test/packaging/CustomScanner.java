/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.packaging;

import org.hibernate.boot.archive.scan.internal.StandardScanner;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.scan.spi.ScanParameters;
import org.hibernate.boot.archive.scan.spi.ScanResult;
import org.hibernate.boot.archive.scan.spi.Scanner;

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
	public ScanResult scan(
			ScanEnvironment environment,
			ScanOptions options,
			ScanParameters parameters) {
		isUsed = true;
		return delegate.scan( environment, options, parameters );
	}
}

