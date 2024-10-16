/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.archive.scan.spi;

import org.hibernate.archive.scan.internal.MappingFileDescriptorImpl;
import org.hibernate.archive.scan.internal.ScanResultCollector;
import org.hibernate.boot.archive.spi.ArchiveContext;
import org.hibernate.boot.archive.spi.ArchiveEntry;
import org.hibernate.boot.archive.spi.ArchiveEntryHandler;

/**
 * Defines handling and filtering for all non-class file (package-info is also a class file...) entries within an archive
 *
 * @author Steve Ebersole
 */
public class NonClassFileArchiveEntryHandler implements ArchiveEntryHandler {
	private final ScanResultCollector resultCollector;

	public NonClassFileArchiveEntryHandler(ScanResultCollector resultCollector) {
		this.resultCollector = resultCollector;
	}

	@Override
	public void handleEntry(ArchiveEntry entry, ArchiveContext context) {
		resultCollector.handleMappingFile(
				new MappingFileDescriptorImpl( entry.getNameWithinArchive(), entry.getStreamAccess() ),
				context.isRootUrl()
		);
	}
}
