/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.discovery.archive.internal;

import org.hibernate.jpa.boot.discovery.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.jpa.boot.discovery.archive.spi.ArchiveEntry;
import org.hibernate.jpa.boot.discovery.archive.spi.ArchiveException;
import org.hibernate.jpa.boot.discovery.archive.spi.InputStreamAccess;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import static org.hibernate.internal.log.UrlMessageBundle.URL_MESSAGE_LOGGER;

/// An `ArchiveDescriptor` for exploded (directory) archives.
///
/// @author Steve Ebersole
public class ExplodedArchiveDescriptor extends AbstractArchiveDescriptor {
	/// Constructs an ExplodedArchiveDescriptor
	///
	/// @param archiveDescriptorFactory The factory creating this
	/// @param archiveUrl The directory URL
	/// @param entryBasePrefix the base (within the url) that described the prefix for entries within the archive
	public ExplodedArchiveDescriptor(
			ArchiveDescriptorFactory archiveDescriptorFactory,
			URL archiveUrl,
			String entryBasePrefix) {
		super( archiveDescriptorFactory, archiveUrl, entryBasePrefix );
	}

	@Override
	public void visitClassEntries(Consumer<ArchiveEntry> entryConsumer) {
		final File rootDirectory = resolveRootDirectory();
		if ( rootDirectory == null ) {
			return;
		}

		if ( rootDirectory.isDirectory() ) {
			processDirectory( rootDirectory, null, entryConsumer );
		}
		else {
			//assume zipped file
			processZippedRoot( rootDirectory, entryConsumer );
		}
	}

	private File resolveRootDirectory() {
		final File archiveUrlDirectory;
		try {
			final String filePart = getArchiveUrl().getFile();
			if ( filePart != null && filePart.indexOf( ' ' ) != -1 ) {
				//unescaped (from the container), keep as is
				archiveUrlDirectory = new File( filePart );
			}
			else {
				archiveUrlDirectory = new File( getArchiveUrl().toURI().getSchemeSpecificPart() );
			}
		}
		catch (URISyntaxException e) {
			URL_MESSAGE_LOGGER.logMalformedUrl( getArchiveUrl(), e );
			return null;
		}

		if ( !archiveUrlDirectory.exists() ) {
			URL_MESSAGE_LOGGER.logFileDoesNotExist( getArchiveUrl() );
			return null;
		}
		if ( !archiveUrlDirectory.isDirectory() ) {
			URL_MESSAGE_LOGGER.logFileIsNotDirectory( getArchiveUrl() );
			return null;
		}

		final String entryBase = getEntryBasePrefix();
		if ( entryBase != null && entryBase.length() > 0 && ! "/".equals( entryBase ) ) {
			return new File( archiveUrlDirectory, entryBase );
		}
		else {
			return archiveUrlDirectory;
		}
	}

	private void processDirectory(
			File directory,
			String path,
			Consumer<ArchiveEntry> entryConsumer) {
		if ( directory == null ) {
			return;
		}

		final File[] files = directory.listFiles();
		if ( files == null ) {
			return;
		}

		path = path == null ? "" : path + "/";
		for ( final File localFile : files ) {
			if ( !localFile.exists() ) {
				// should never happen conceptually, but...
				continue;
			}

			if ( localFile.isDirectory() ) {
				processDirectory( localFile, path + localFile.getName(), entryConsumer );
				continue;
			}

			final String name = localFile.getAbsolutePath();
			final String relativeName = path + localFile.getName();
			final InputStreamAccess inputStreamAccess = new FileInputStreamAccess( name, localFile );
			entryConsumer.accept( new ArchiveEntryImpl( name, relativeName, inputStreamAccess ) );
		}
	}

	private void processZippedRoot(File rootFile, Consumer<ArchiveEntry> entryConsumer) {
		try (final JarFile jarFile = new JarFile(rootFile)){
			final Enumeration<? extends ZipEntry> entries = jarFile.entries();
			while ( entries.hasMoreElements() ) {
				final ZipEntry zipEntry = entries.nextElement();
				if ( zipEntry.isDirectory() ) {
					continue;
				}

				final String name = extractName( zipEntry );
				final String relativeName = extractRelativeName( zipEntry );
				final InputStreamAccess inputStreamAccess;
				try {
					inputStreamAccess = buildByteBasedInputStreamAccess( name, jarFile.getInputStream( zipEntry ) );
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

				entryConsumer.accept( new ArchiveEntryImpl( name, relativeName, inputStreamAccess ) );
			}
		}
		catch (IOException e) {
			throw new ArchiveException( "Error accessing jar file [" + rootFile.getAbsolutePath() + "]", e );
		}
	}

}
