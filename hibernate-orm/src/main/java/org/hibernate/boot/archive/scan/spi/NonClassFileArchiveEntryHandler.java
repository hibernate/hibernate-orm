/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.scan.spi;

import org.hibernate.boot.archive.scan.internal.MappingFileDescriptorImpl;
import org.hibernate.boot.archive.scan.internal.ScanResultCollector;
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
