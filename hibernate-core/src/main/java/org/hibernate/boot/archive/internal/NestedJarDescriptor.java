/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.internal;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveEntry;
import org.hibernate.boot.archive.spi.ArchiveException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static org.hibernate.boot.archive.internal.ArchiveHelper.buildByteBasedInputStreamAccess;

/// [ArchiveDescriptor] implementation for describing non-root jar
/// references nested within a jar.
///
/// @author Steve Ebersole
public class NestedJarDescriptor implements ArchiveDescriptor {
	private final URL archiveUrl;

	public NestedJarDescriptor(URL archiveUrl) {
		this.archiveUrl = archiveUrl;
	}

	@Override
	public URL getUrl() {
		return archiveUrl;
	}

	@Override
	public void visitClassEntries(Consumer<ArchiveEntry> entryConsumer) {
		try (final InputStream is = new BufferedInputStream( archiveUrl.openStream() );
			final JarInputStream jarInputStream = new JarInputStream( is )) {

			JarEntry jarEntry;
			while ( ( jarEntry = jarInputStream.getNextJarEntry() ) != null ) {
				final String jarEntryName = jarEntry.getName();

				if ( !jarEntryName.endsWith( ".class" ) ) {
					continue;
				}

				if ( jarEntry.isDirectory() ) {
					continue;
				}

				try {
					entryConsumer.accept( new ArchiveEntryImpl(
							jarEntryName,
							jarEntryName,
							new URI( archiveUrl + "!/" + jarEntryName ),
							buildByteBasedInputStreamAccess( jarEntryName, jarInputStream )
					) );
				}
				catch (URISyntaxException e) {
					throw new ArchiveException(
							String.format( Locale.ROOT,
									"Unable to create archive entry URI: %s - %s",
									archiveUrl,
									jarEntry.getName()
							),
							e
					);
				}
			}
		}
		catch (IOException e) {
			throw new ArchiveException( "Error accessing nested jar archive [" + archiveUrl + "]", e );
		}
	}

	@Override
	public @Nullable ArchiveEntry findEntry(String relativePath) {
		try (final InputStream is = new BufferedInputStream( archiveUrl.openStream() );
			final JarInputStream jarInputStream = new JarInputStream( is )) {
			JarEntry jarEntry;
			while ( ( jarEntry = jarInputStream.getNextJarEntry() ) != null ) {
				final String jarEntryName = jarEntry.getName();
				if ( relativePath.equals( jarEntryName ) ) {
					try {
						return new ArchiveEntryImpl(
								jarEntryName,
								jarEntryName,
								new URI( archiveUrl + "!/" + jarEntryName ),
								buildByteBasedInputStreamAccess( jarEntryName, jarInputStream )
						);
					}
					catch (URISyntaxException e) {
						throw new ArchiveException(
								String.format( Locale.ROOT,
										"Unable to create archive entry URI: %s - %s",
										archiveUrl,
										jarEntry.getName()
								),
								e
						);
					}
				}
			}
		}
		catch (IOException e) {
			throw new ArchiveException( "Error accessing nested jar archive [" + archiveUrl + "]", e );
		}

		return null;
	}

	@Override @NonNull
	public ArchiveDescriptor resolveJarFileReference(@NonNull String jarFileReference) {
		throw new UnsupportedOperationException( "Not supported." );
	}
}
