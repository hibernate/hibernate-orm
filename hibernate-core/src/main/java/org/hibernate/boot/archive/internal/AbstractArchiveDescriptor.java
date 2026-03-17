/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.internal;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.archive.spi.InputStreamAccess;

import java.io.InputStream;
import java.net.URL;
import java.util.zip.ZipEntry;

/// Base support for ArchiveDescriptor implementors.
///
/// @author Steve Ebersole
public abstract class AbstractArchiveDescriptor implements ArchiveDescriptor {
	protected final ArchiveDescriptorFactory archiveDescriptorFactory;
	protected final URL archiveUrl;
	protected final String entryBasePrefix;

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

	@Override
	public URL getUrl() {
		return archiveUrl;
	}

	@SuppressWarnings("unused")
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
		return ArchiveHelper.buildByteBasedInputStreamAccess( name, inputStream );
	}

}
