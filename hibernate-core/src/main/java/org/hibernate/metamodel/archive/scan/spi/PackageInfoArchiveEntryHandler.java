/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.archive.scan.spi;

import org.hibernate.metamodel.archive.scan.internal.PackageDescriptorImpl;
import org.hibernate.metamodel.archive.scan.internal.ScanResultCollector;
import org.hibernate.metamodel.archive.spi.ArchiveContext;
import org.hibernate.metamodel.archive.spi.ArchiveEntry;
import org.hibernate.metamodel.archive.spi.ArchiveEntryHandler;

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
