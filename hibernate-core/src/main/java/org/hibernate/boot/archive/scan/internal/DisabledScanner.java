/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.scan.internal;

import java.util.Collections;

import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.scan.spi.ScanParameters;
import org.hibernate.boot.archive.scan.spi.ScanResult;
import org.hibernate.boot.archive.scan.spi.Scanner;

/**
 * Implementation of Scanner that does nothing. Used for optimizing startup
 * time when metadata scanning is not needed.
 *
 * @author Petteri Pitkanen
 */
public class DisabledScanner implements Scanner {
	private static final ScanResult emptyScanResult = new ScanResultImpl(
		Collections.emptySet(),
		Collections.emptySet(),
		Collections.emptySet()
	);

	@Override
	public ScanResult scan(final ScanEnvironment environment, final ScanOptions options, final ScanParameters parameters) {
		return emptyScanResult;
	}
}
