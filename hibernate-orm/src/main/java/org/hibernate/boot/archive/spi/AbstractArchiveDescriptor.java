/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.spi;

import java.io.InputStream;
import java.net.URL;
import java.util.zip.ZipEntry;

import org.hibernate.boot.archive.internal.ByteArrayInputStreamAccess;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.boot.archive.internal.ArchiveHelper;

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
