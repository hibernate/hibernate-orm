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
import org.hibernate.jpa.boot.internal.MappingFileDescriptorImpl;
import org.hibernate.jpa.boot.spi.MappingFileDescriptor;

/**
 * Defines handling and filtering for all non-class file (package-info is also a class file...) entries within an archive
 *
 * @author Steve Ebersole
 */
public class NonClassFileArchiveEntryHandler implements ArchiveEntryHandler {
	private final ScanOptions scanOptions;
	private final Callback callback;

	/**
	 * Contract for the thing interested in being notified about accepted mapping file descriptors.
	 */
	public static interface Callback {
		public void locatedMappingFile(MappingFileDescriptor mappingFileDescriptor);
	}

	public NonClassFileArchiveEntryHandler(ScanOptions scanOptions, Callback callback) {
		this.scanOptions = scanOptions;
		this.callback = callback;
	}

	@Override
	public void handleEntry(ArchiveEntry entry, ArchiveContext context) {
		if ( acceptAsMappingFile( entry, context) ) {
			notifyMatchedMappingFile( entry );
		}
	}

	@SuppressWarnings("SimplifiableIfStatement")
	private boolean acceptAsMappingFile(ArchiveEntry entry, ArchiveContext context) {
		if ( entry.getNameWithinArchive().endsWith( "hbm.xml" ) ) {
			return scanOptions.canDetectHibernateMappingFiles();
		}

		// todo : should really do this case-insensitively
		// use getNameWithinArchive, not getName -- ensure paths are normalized (Windows, etc.)
		if ( entry.getNameWithinArchive().endsWith( "META-INF/orm.xml" ) ) {
			if ( context.getPersistenceUnitDescriptor().getMappingFileNames().contains( "META-INF/orm.xml" ) ) {
				// if the user explicitly listed META-INF/orm.xml, only except the root one
				//
				// not sure why exactly, but this is what the old code does
				return context.isRootUrl();
			}
			return true;
		}

		return context.getPersistenceUnitDescriptor().getMappingFileNames().contains( entry.getNameWithinArchive() );
	}

	protected final void notifyMatchedMappingFile(ArchiveEntry entry) {
		callback.locatedMappingFile( toMappingFileDescriptor( entry ) );
	}

	protected MappingFileDescriptor toMappingFileDescriptor(ArchiveEntry entry) {
		return new MappingFileDescriptorImpl( entry.getNameWithinArchive(), entry.getStreamAccess() );
	}
}
