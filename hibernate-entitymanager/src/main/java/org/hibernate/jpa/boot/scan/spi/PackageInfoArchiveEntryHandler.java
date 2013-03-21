/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.boot.scan.spi;

import org.hibernate.jpa.boot.archive.spi.ArchiveContext;
import org.hibernate.jpa.boot.archive.spi.ArchiveEntry;
import org.hibernate.jpa.boot.archive.spi.ArchiveEntryHandler;
import org.hibernate.jpa.boot.internal.PackageDescriptorImpl;
import org.hibernate.jpa.boot.spi.PackageDescriptor;

import static java.io.File.separatorChar;

/**
 * @author Steve Ebersole
 */
public class PackageInfoArchiveEntryHandler implements ArchiveEntryHandler {
	@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
	private final ScanOptions scanOptions;
	private final Callback callback;

	public static interface Callback {
		public void locatedPackage(PackageDescriptor packageDescriptor);
	}

	public PackageInfoArchiveEntryHandler(ScanOptions scanOptions, Callback callback) {
		this.scanOptions = scanOptions;
		this.callback = callback;
	}

	@Override
	public void handleEntry(ArchiveEntry entry, ArchiveContext context) {
		if ( entry.getNameWithinArchive().equals( "package-info.class" ) ) {
			// the old code skipped package-info in the root package/dir...
			return;
		}
		notifyMatchedPackage( toPackageDescriptor( entry ) );
	}

	protected PackageDescriptor toPackageDescriptor(ArchiveEntry entry) {
		final String packageInfoFilePath = entry.getNameWithinArchive();
		final String packageName = packageInfoFilePath.substring( 0, packageInfoFilePath.lastIndexOf( '/' ) )
				.replace( separatorChar, '.' );

		return new PackageDescriptorImpl( packageName, entry.getStreamAccess() );
	}

	protected final void notifyMatchedPackage(PackageDescriptor packageDescriptor) {
		callback.locatedPackage( packageDescriptor );
	}
}
