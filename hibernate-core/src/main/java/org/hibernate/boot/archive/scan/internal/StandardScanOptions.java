/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
