package org.hibernate.jpa.test.packaging;

import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.scan.internal.StandardScanner;
import org.hibernate.jpa.boot.scan.spi.ScanOptions;
import org.hibernate.jpa.boot.scan.spi.ScanResult;
import org.hibernate.jpa.boot.scan.spi.Scanner;

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
	public ScanResult scan(PersistenceUnitDescriptor persistenceUnit, ScanOptions options) {
		isUsed = true;
		return delegate.scan( persistenceUnit, options );
	}
}
