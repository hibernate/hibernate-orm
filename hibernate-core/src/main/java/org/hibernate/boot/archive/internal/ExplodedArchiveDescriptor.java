/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.internal;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.hibernate.boot.archive.spi.AbstractArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveContext;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.archive.spi.ArchiveEntry;
import org.hibernate.boot.archive.spi.ArchiveException;
import org.hibernate.boot.archive.spi.InputStreamAccess;

import static org.hibernate.internal.log.UrlMessageBundle.URL_LOGGER;

/**
 * Descriptor for exploded (directory) archives
 *
 * @author Steve Ebersole
 */
public class ExplodedArchiveDescriptor extends AbstractArchiveDescriptor {
	/**
	 * Constructs an ExplodedArchiveDescriptor
	 *
	 * @param archiveDescriptorFactory The factory creating this
	 * @param archiveUrl The directory URL
	 * @param entryBasePrefix the base (within the url) that described the prefix for entries within the archive
	 */
	public ExplodedArchiveDescriptor(
			ArchiveDescriptorFactory archiveDescriptorFactory,
			URL archiveUrl,
			String entryBasePrefix) {
		super( archiveDescriptorFactory, archiveUrl, entryBasePrefix );
	}

	@Override
	public void visitArchive(ArchiveContext context) {
		final File rootDirectory = resolveRootDirectory();
		if ( rootDirectory == null ) {
			return;
		}

		if ( rootDirectory.isDirectory() ) {
			processDirectory( rootDirectory, null, context );
		}
		else {
			//assume zipped file
			processZippedRoot( rootDirectory, context );
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
			URL_LOGGER.logMalformedUrl( getArchiveUrl(), e );
			return null;
		}

		if ( !archiveUrlDirectory.exists() ) {
			URL_LOGGER.logFileDoesNotExist( getArchiveUrl() );
			return null;
		}
		if ( !archiveUrlDirectory.isDirectory() ) {
			URL_LOGGER.logFileIsNotDirectory( getArchiveUrl() );
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
			ArchiveContext context) {
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
				processDirectory( localFile, path + localFile.getName(), context );
				continue;
			}

			final String name = localFile.getAbsolutePath();
			final String relativeName = path + localFile.getName();
			final InputStreamAccess inputStreamAccess = new FileInputStreamAccess( name, localFile );

			final ArchiveEntry entry = new ArchiveEntry() {
				@Override
				public String getName() {
					return name;
				}

				@Override
				public String getNameWithinArchive() {
					return relativeName;
				}

				@Override
				public InputStreamAccess getStreamAccess() {
					return inputStreamAccess;
				}
			};

			context.obtainArchiveEntryHandler( entry ).handleEntry( entry, context );
		}
	}

	private void processZippedRoot(File rootFile, ArchiveContext context) {
		try {
			final JarFile jarFile = new JarFile(rootFile);
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

				final ArchiveEntry entry = new ArchiveEntry() {
					@Override
					public String getName() {
						return name;
					}

					@Override
					public String getNameWithinArchive() {
						return relativeName;
					}

					@Override
					public InputStreamAccess getStreamAccess() {
						return inputStreamAccess;
					}
				};
				context.obtainArchiveEntryHandler( entry ).handleEntry( entry, context );
			}
		}
		catch (IOException e) {
			throw new ArchiveException( "Error accessing jar file [" + rootFile.getAbsolutePath() + "]", e );
		}
	}

}
