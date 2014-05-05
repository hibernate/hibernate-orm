package org.hibernate.jpa.test.packaging;

import org.hibernate.metamodel.archive.scan.internal.StandardScanner;
import org.hibernate.metamodel.archive.scan.spi.ScanEnvironment;
import org.hibernate.metamodel.archive.scan.spi.ScanOptions;
import org.hibernate.metamodel.archive.scan.spi.ScanParameters;
import org.hibernate.metamodel.archive.scan.spi.Scanner;

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
	public org.hibernate.metamodel.archive.scan.spi.ScanResult scan(
			ScanEnvironment environment,
			ScanOptions options,
			ScanParameters parameters) {
		isUsed = true;
		return delegate.scan( environment, options, parameters );
	}
}
