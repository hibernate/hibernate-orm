/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.scan.internal;

import java.util.Collections;
import java.util.Set;

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
	private static final ScanResult emptyScanResult = new ScanResult() {
		@Override
		public Set<PackageDescriptor> getLocatedPackages() {
			return Collections.emptySet();
		}

		@Override
		public Set<ClassDescriptor> getLocatedClasses() {
			return Collections.emptySet();
		}

		@Override
		public Set<MappingFileDescriptor> getLocatedMappingFiles() {
			return Collections.emptySet();
		}
	};

	@Override
	public ScanResult scan(final ScanEnvironment environment, final ScanOptions options, final ScanParameters parameters) {
		return emptyScanResult;
	}
}
