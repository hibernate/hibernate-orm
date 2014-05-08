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
package org.hibernate.metamodel.archive.spi;

import java.io.InputStream;
import java.net.URL;
import java.util.zip.ZipEntry;

import org.hibernate.metamodel.archive.internal.ByteArrayInputStreamAccess;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.archive.internal.ArchiveHelper;

/**
 * Base support for ArchiveDescriptor implementors.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractArchiveDescriptor implements ArchiveDescriptor {
	private final ArchiveDescriptorFactory archiveDescriptorFactory;
	private final URL archiveUrl;
	private final String entryBasePrefix;

	protected AbstractArchiveDescriptor(
			ArchiveDescriptorFactory archiveDescriptorFactory,
			URL archiveUrl,
			String entryBasePrefix) {
		this.archiveDescriptorFactory = archiveDescriptorFactory;
		this.archiveUrl = archiveUrl;
		this.entryBasePrefix = normalizeEntryBasePrefix( entryBasePrefix );
	}

	private static String normalizeEntryBasePrefix(String entryBasePrefix) {
		if ( StringHelper.isEmpty( entryBasePrefix ) || entryBasePrefix.length() == 1 ) {
			return null;
		}

		return entryBasePrefix.startsWith( "/" ) ? entryBasePrefix.substring( 1 ) : entryBasePrefix;
	}

	@SuppressWarnings("UnusedDeclaration")
	protected ArchiveDescriptorFactory getArchiveDescriptorFactory() {
		return archiveDescriptorFactory;
	}

	protected URL getArchiveUrl() {
		return archiveUrl;
	}

	protected String getEntryBasePrefix() {
		return entryBasePrefix;
	}

	protected String extractRelativeName(ZipEntry zipEntry) {
		final String entryName = extractName( zipEntry );
		return entryBasePrefix != null && entryName.contains( entryBasePrefix )
				? entryName.substring( entryBasePrefix.length() )
				: entryName;
	}

	protected String extractName(ZipEntry zipEntry) {
		return normalizePathName( zipEntry.getName() );
	}

	protected String normalizePathName(String pathName) {
		return pathName.startsWith( "/" ) ? pathName.substring( 1 ) : pathName;
	}

	protected InputStreamAccess buildByteBasedInputStreamAccess(final String name, InputStream inputStream) {
		// because of how jar InputStreams work we need to extract the bytes immediately.  However, we
		// do delay the creation of the ByteArrayInputStreams until needed
		final byte[] bytes = ArchiveHelper.getBytesFromInputStreamSafely( inputStream );
		return new ByteArrayInputStreamAccess( name, bytes );
	}

}
