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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
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

			if ( !localFile.getName().endsWith( ".class" ) ) {
				continue;
			}

			final String name = localFile.getAbsolutePath();
			final String relativeName = path + localFile.getName();
			final InputStreamAccess inputStreamAccess = new FileInputStreamAccess( name, localFile );
			entryConsumer.accept( new ArchiveEntryImpl( name, relativeName, localFile.toURI(), inputStreamAccess ) );
		}
	}

	private void processZippedRoot(File rootFile, Consumer<ArchiveEntry> entryConsumer) {
		final String entryUriBase = getArchiveUrl().toString();

		try (final JarFile jarFile = new JarFile(rootFile)) {
			final Enumeration<? extends ZipEntry> entries = jarFile.entries();
			while ( entries.hasMoreElements() ) {
				final ZipEntry zipEntry = entries.nextElement();
				if ( zipEntry.isDirectory() ) {
					continue;
				}

				final String name = extractName( zipEntry );
				if ( !name.endsWith(  ".class" ) ) {
					continue;
				}
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

				// todo (jpa4) : for now, pass null
				entryConsumer.accept( new ArchiveEntryImpl(
						name,
						relativeName,
						new URL( "jar:" + entryUriBase + "!/" + relativeName ).toURI(),
						inputStreamAccess
				) );
			}
		}
		catch (IOException e) {
			throw new ArchiveException( "Error accessing jar file [" + rootFile.getAbsolutePath() + "]", e );
		}
		catch (URISyntaxException e) {
			throw new ArchiveException( "Unable to create archive entry URI", e );
		}
	}


	@Override
	public @Nullable ArchiveEntry findEntry(String relativePath) {
		final File file = resolveRelativePath( relativePath );
		if ( file == null ) {
			return null;
		}
		final String name = file.getAbsolutePath();
		final InputStreamAccess inputStreamAccess = new FileInputStreamAccess( name, file );
		return new ArchiveEntryImpl( name, relativePath, file.toURI(), inputStreamAccess );
	}

	private File resolveRelativePath(String relativePath) {
		final File rootDirectory = resolveRootDirectory();
		if ( rootDirectory == null ) {
			return null;
		}

		final File localFile = new File( rootDirectory, relativePath );
		if ( !localFile.exists() ) {
			return null;
		}

		return localFile;
	}

	@Override @NonNull
	public ArchiveDescriptor resolveJarFileReference(@NonNull String jarFileReference) {
		// try it as a relative reference
		final ArchiveEntry entry = findEntry( jarFileReference );
		if ( entry != null ) {
			try {
				return archiveDescriptorFactory.buildArchiveDescriptor( entry.getUri().toURL() );
			}
			catch (MalformedURLException e) {
				throw new ArchiveException( "Unable to convert relative <jar-file/> reference to URL [" + jarFileReference + "]", e );
			}
		}

		var standardResolution = ArchiveHelper.standardJarFileReferenceResolution( jarFileReference, archiveDescriptorFactory );
		if ( standardResolution != null ) {
			return standardResolution;
		}

		try {
			var file = new File( resolveRootDirectory(), jarFileReference );
			if ( file.exists() ) {
				return archiveDescriptorFactory.buildArchiveDescriptor( file.toURI().toURL() );
			}
		}
		catch (MalformedURLException e) {
			throw new ArchiveException( "Unable to convert jar File to URL [" + jarFileReference + "]", e );
		}

		throw new ArchiveException( "Unable to resolve <jar-file/> reference - " + jarFileReference );
	}
}
