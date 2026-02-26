/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.scan.internal;

import org.hibernate.boot.scan.spi.Scanner;
import org.hibernate.boot.scan.spi.ScanningContext;
import org.hibernate.boot.scan.spi.ScanningProvider;

/**
 * @author Steve Ebersole
 */
public class ProvidedScannerProvider implements ScanningProvider {
	private final Scanner providedScanner;

	public ProvidedScannerProvider(Scanner providedScanner) {
		this.providedScanner = providedScanner;
	}

	@Override
	public Scanner builderScanner(ScanningContext scanningContext) {
		return providedScanner;
	}
}
