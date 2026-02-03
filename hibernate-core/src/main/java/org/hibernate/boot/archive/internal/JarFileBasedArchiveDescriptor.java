/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.internal;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.archive.spi.ArchiveEntry;
import org.hibernate.boot.archive.spi.ArchiveException;
import org.hibernate.boot.archive.spi.InputStreamAccess;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import static org.hibernate.internal.log.UrlMessageBundle.URL_MESSAGE_LOGGER;

/// An `ArchiveDescriptor` implementation leveraging the [JarFile] API for processing.
///
/// @author Steve Ebersole
public class JarFileBasedArchiveDescriptor extends AbstractArchiveDescriptor {
	/// Constructs a JarFileBasedArchiveDescriptor
	///
	/// @param archiveDescriptorFactory The factory creating this
	/// @param archiveUrl The url to the JAR file
	/// @param entry The prefix for entries within the JAR url
	public JarFileBasedArchiveDescriptor(
			ArchiveDescriptorFactory archiveDescriptorFactory,
			URL archiveUrl,
			String entry) {
		super( archiveDescriptorFactory, archiveUrl, entry );
	}

	@Override
	public void visitClassEntries(Consumer<ArchiveEntry> entryConsumer) {
		final JarFile jarFile = resolveJarFileReference();
		if ( jarFile == null ) {
			return;
		}

		try {
			final Enumeration<? extends ZipEntry> zipEntries = jarFile.entries();
			while ( zipEntries.hasMoreElements() ) {
				final ZipEntry zipEntry = zipEntries.nextElement();
				final String entryName = extractName( zipEntry );
				if ( !entryName.endsWith( ".class" ) ) {
					continue;
				}

				if ( getEntryBasePrefix() != null && ! entryName.startsWith( getEntryBasePrefix() ) ) {
					continue;
				}
				if ( zipEntry.isDirectory() ) {
					continue;
				}

				if ( entryName.equals( getEntryBasePrefix() ) ) {
					// exact match, might be a nested jar entry (ie from jar:file:..../foo.ear!/bar.jar)
					//
					// This algorithm assumes that the zipped file is only the URL root (including entry), not
					// just any random entry
					try (	final InputStream is = new BufferedInputStream( jarFile.getInputStream( zipEntry ) );
							final JarInputStream jarInputStream = new JarInputStream( is )) {
						ZipEntry subZipEntry = jarInputStream.getNextEntry();
						while ( subZipEntry != null ) {
							if ( ! subZipEntry.isDirectory() ) {

								final String name = extractName( subZipEntry );
								final String relativeName = extractRelativeName( subZipEntry );

								entryConsumer.accept( new ArchiveEntryImpl(
										name,
										relativeName,
										URI.create( "jar:" + archiveUrl.toExternalForm() + "!/" + relativeName ),
										buildByteBasedInputStreamAccess( name, jarInputStream )
								) );
							}

							subZipEntry = jarInputStream.getNextEntry();
						}
					}
					catch (Exception e) {
						throw new ArchiveException( "Error accessing JarFile entry [" + zipEntry.getName() + "]", e );
					}
				}
				else {
					final String name = extractName( zipEntry );
					final String relativeName = extractRelativeName( zipEntry );
					final InputStreamAccess inputStreamAccess;
					try (InputStream is = jarFile.getInputStream( zipEntry )) {
						inputStreamAccess = buildByteBasedInputStreamAccess( name, is );
					}
					catch (IOException e) {
						throw new ArchiveException(
								String.format(
										"Unable to access stream from jar file [%s] for entry [%s]",
										jarFile.getName(),
										zipEntry.getName()
								)
						);
					}

					entryConsumer.accept( new ArchiveEntryImpl(
							name,
							relativeName,
							URI.create( "jar:" + archiveUrl.toExternalForm() + "!/" + relativeName ),
							inputStreamAccess
					) );
				}
			}
		}
		finally {
			try {
				jarFile.close();
			}
			catch ( Exception ignore ) {
			}
		}
	}

	private JarFile resolveJarFileReference() {
		try {
			final String filePart = getArchiveUrl().getFile();
			if ( filePart != null && filePart.indexOf( ' ' ) != -1 ) {
				// unescaped (from the container), keep as is
				return new JarFile( getArchiveUrl().getFile() );
			}
			else {
				return new JarFile( getArchiveUrl().toURI().getSchemeSpecificPart() );
			}
		}
		catch (IOException e) {
			URL_MESSAGE_LOGGER.logUnableToFindFileByUrl( getArchiveUrl(), e );
		}
		catch (URISyntaxException e) {
			URL_MESSAGE_LOGGER.logMalformedUrl( getArchiveUrl(), e );
		}
		return null;
	}

	@Override
	public @Nullable ArchiveEntry findEntry(String path) {
		final JarFile jarFile = resolveJarFileReference();
		if ( jarFile == null ) {
			return null;
		}

		try {
			final JarEntry jarEntry = jarFile.getJarEntry( path );
			if ( jarEntry == null ) {
				return null;
			}
			final String name = extractName( jarEntry );
			final String relativeName = extractRelativeName( jarEntry );
			final InputStreamAccess inputStreamAccess;
			try (InputStream is = jarFile.getInputStream( jarEntry )) {
				inputStreamAccess = buildByteBasedInputStreamAccess( name, is );
			}
			catch (IOException e) {
				throw new ArchiveException(
						String.format(
								"Unable to access stream from jar file [%s] for entry [%s]",
								jarFile.getName(),
								jarEntry.getName()
						)
				);
			}


			return new ArchiveEntryImpl(
					name,
					relativeName,
					URI.create( "jar:" + archiveUrl.toExternalForm() + "!/" + relativeName ),
					inputStreamAccess
			);
		}
		finally {
			try {
				jarFile.close();
			}
			catch ( Exception ignore ) {
			}
		}
	}

	@Override @NonNull
	public ArchiveDescriptor resolveJarFileReference(@NonNull String jarFileReference) {
		// try it as a relative reference
		final ArchiveEntry entry = findEntry( jarFileReference );
		if ( entry != null ) {
			try {
				return new NestedJarDescriptor( entry.getUri().toURL() );
			}
			catch (MalformedURLException e) {
				throw new ArchiveException( "Unable to convert relative jar-file reference to URL [" + jarFileReference + "]", e );
			}
		}

		var standardResolution = ArchiveHelper.standardJarFileReferenceResolution( jarFileReference, archiveDescriptorFactory );
		if ( standardResolution != null ) {
			return standardResolution;
		}

		throw new ArchiveException( "Unable to resolve <jar-file/> reference - " + jarFileReference );
	}
}
