/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.discovery.archive.internal;

import org.hibernate.jpa.boot.discovery.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.jpa.boot.discovery.archive.spi.ArchiveEntry;
import org.hibernate.jpa.boot.discovery.archive.spi.ArchiveException;
import org.hibernate.jpa.boot.discovery.archive.spi.InputStreamAccess;

import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import static org.hibernate.internal.log.UrlMessageBundle.URL_MESSAGE_LOGGER;

/// An `ArchiveDescriptor` that works on archives accessible through a [JarInputStream].
///
/// @implNote This is less efficient implementation than [JarFileBasedArchiveDescriptor].
///
/// @author Emmanuel Bernard
/// @author Steve Ebersole
public class JarInputStreamBasedArchiveDescriptor extends AbstractArchiveDescriptor {

	/// Constructs a JarInputStreamBasedArchiveDescriptor
	///
	/// @param archiveDescriptorFactory The factory creating this
	/// @param url The url to the JAR file
	/// @param entry The prefix for entries within the JAR url
	public JarInputStreamBasedArchiveDescriptor(
			ArchiveDescriptorFactory archiveDescriptorFactory,
			URL url,
			String entry) {
		super( archiveDescriptorFactory, url, entry );
	}

	@Override
	public void visitClassEntries(Consumer<ArchiveEntry> entryConsumer) {
		final JarInputStream jarInputStream;
		try {
			jarInputStream = new JarInputStream( getArchiveUrl().openStream() );
		}
		catch (Exception e) {
			//really should catch IOException but Eclipse is buggy and raise NPE...
			URL_MESSAGE_LOGGER.logUnableToFindFileByUrl( getArchiveUrl(), e );
			return;
		}

		try {
			JarEntry jarEntry;
			while ( ( jarEntry = jarInputStream.getNextJarEntry() ) != null ) {
				final String jarEntryName = jarEntry.getName();

				if ( !jarEntryName.endsWith( ".class" ) ) {
					continue;
				}

				if ( getEntryBasePrefix() != null && ! jarEntryName.startsWith( getEntryBasePrefix() ) ) {
					continue;
				}

				if ( jarEntry.isDirectory() ) {
					continue;
				}

				if ( jarEntryName.equals( getEntryBasePrefix() ) ) {
					// exact match, might be a nested jar entry (ie from jar:file:..../foo.ear!/bar.jar)
					//
					// This algorithm assumes that the zipped file is only the URL root (including entry), not
					// just any random entry
					try {
						final JarInputStream subJarInputStream = new JarInputStream( jarInputStream );
						try {
							ZipEntry subZipEntry = jarInputStream.getNextEntry();
							while (subZipEntry != null) {
								if ( ! subZipEntry.isDirectory() ) {
									final String subName = extractName( subZipEntry );
									final InputStreamAccess inputStreamAccess = buildByteBasedInputStreamAccess( subName, subJarInputStream );

									entryConsumer.accept( new ArchiveEntryImpl( subName, subName, inputStreamAccess ) );
								}

								subZipEntry = jarInputStream.getNextJarEntry();
							}
						}
						finally {
							subJarInputStream.close();
						}
					}
					catch (Exception e) {
						throw new ArchiveException( "Error accessing nested jar", e );
					}
				}
				else {
					final String entryName = extractName( jarEntry );
					final InputStreamAccess inputStreamAccess = buildByteBasedInputStreamAccess( entryName, jarInputStream );

					final String relativeName = extractRelativeName( jarEntry );
					entryConsumer.accept(  new ArchiveEntryImpl(relativeName, relativeName, inputStreamAccess ) );
				}
			}

			jarInputStream.close();
		}
		catch (IOException ioe) {
			throw new ArchiveException(
					String.format( "Error accessing JarInputStream [%s]", getArchiveUrl() ),
					ioe
			);
		}
	}
}
