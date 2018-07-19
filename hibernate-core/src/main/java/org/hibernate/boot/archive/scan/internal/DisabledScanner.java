/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.scan.internal;

import java.util.Collections;

import org.hibernate.boot.archive.scan.spi.ClassDescriptor;
import org.hibernate.boot.archive.scan.spi.MappingFileDescriptor;
import org.hibernate.boot.archive.scan.spi.PackageDescriptor;
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
		Collections.<PackageDescriptor>emptySet(),
		Collections.<ClassDescriptor>emptySet(),
		Collections.<MappingFileDescriptor>emptySet()
	);

	@Override
	public ScanResult scan(final ScanEnvironment environment, final ScanOptions options, final ScanParameters parameters) {
		return emptyScanResult;
	}
}
