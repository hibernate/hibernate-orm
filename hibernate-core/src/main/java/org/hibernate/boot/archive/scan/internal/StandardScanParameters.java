/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.scan.internal;

import org.hibernate.boot.archive.scan.spi.ScanParameters;

/**
 * @author Steve Ebersole
 */
public class StandardScanParameters implements ScanParameters {
	/**
	 * Singleton access
	 */
	public static final StandardScanParameters INSTANCE = new StandardScanParameters();

	private StandardScanParameters() {
	}
}
