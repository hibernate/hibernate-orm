/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.archive.scan.spi;

import org.hibernate.archive.scan.internal.PackageDescriptorImpl;
import org.hibernate.archive.scan.internal.ScanResultCollector;
import org.hibernate.boot.archive.scan.spi.PackageDescriptor;
import org.hibernate.boot.archive.spi.ArchiveContext;
import org.hibernate.boot.archive.spi.ArchiveEntry;
import org.hibernate.boot.archive.spi.ArchiveEntryHandler;

/**
 * Defines handling and filtering for package-info file entries within an archive
 *
 * @author Steve Ebersole
 */
public class PackageInfoArchiveEntryHandler implements ArchiveEntryHandler {
	private final ScanResultCollector resultCollector;

	public PackageInfoArchiveEntryHandler(ScanResultCollector resultCollector) {
		this.resultCollector = resultCollector;
	}

	@Override
	public void handleEntry(ArchiveEntry entry, ArchiveContext context) {
		if ( entry.getNameWithinArchive().equals( "package-info.class" ) ) {
			// the old code skipped package-info in the root package/dir...
			return;
		}

		resultCollector.handlePackage( toPackageDescriptor( entry ), context.isRootUrl() );
	}

	protected PackageDescriptor toPackageDescriptor(ArchiveEntry entry) {
		final String packageInfoFilePath = entry.getNameWithinArchive();
		final String packageName = packageInfoFilePath.substring( 0, packageInfoFilePath.lastIndexOf( '/' ) )
				.replace( '/', '.' );

		return new PackageDescriptorImpl( packageName, entry.getStreamAccess() );
	}
}
