/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.scan.internal;

import org.hibernate.boot.archive.scan.spi.ScanOptions;

/**
 * @author Steve Ebersole
 */
public class StandardScanOptions implements ScanOptions {
	private final boolean detectClassesInRoot;
	private final boolean detectClassesInNonRoot;
	private final boolean detectHibernateMappingFiles;

	public StandardScanOptions() {
		this( "hbm,class", false );
	}

	public StandardScanOptions(String explicitDetectionSetting, boolean persistenceUnitExcludeUnlistedClassesValue) {
		if ( explicitDetectionSetting == null ) {
			detectHibernateMappingFiles = true;
			detectClassesInRoot = ! persistenceUnitExcludeUnlistedClassesValue;
			detectClassesInNonRoot = true;
		}
		else {
			detectHibernateMappingFiles = explicitDetectionSetting.contains( "hbm" );
			detectClassesInRoot = explicitDetectionSetting.contains( "class" );
			detectClassesInNonRoot = detectClassesInRoot;
		}
	}

	@Override
	public boolean canDetectUnlistedClassesInRoot() {
		return detectClassesInRoot;
	}

	@Override
	public boolean canDetectUnlistedClassesInNonRoot() {
		return detectClassesInNonRoot;
	}

	@Override
	public boolean canDetectHibernateMappingFiles() {
		return detectHibernateMappingFiles;
	}
}
