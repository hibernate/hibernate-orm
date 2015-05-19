/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.internal;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import org.hibernate.boot.archive.spi.AbstractArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveContext;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.archive.spi.ArchiveEntry;
import org.hibernate.boot.archive.spi.ArchiveEntryHandler;
import org.hibernate.boot.archive.spi.ArchiveException;
import org.hibernate.boot.archive.spi.InputStreamAccess;

import static org.hibernate.internal.log.UrlMessageBundle.URL_LOGGER;

/**
 * An ArchiveDescriptor implementation leveraging the {@link java.util.jar.JarFile} API for processing.
 *
 * @author Steve Ebersole
 */
public class JarFileBasedArchiveDescriptor extends AbstractArchiveDescriptor {
	/**
	 * Constructs a JarFileBasedArchiveDescriptor
	 *
	 * @param archiveDescriptorFactory The factory creating this
	 * @param archiveUrl The url to the JAR file
	 * @param entry The prefix for entries within the JAR url
	 */
	public JarFileBasedArchiveDescriptor(
			ArchiveDescriptorFactory archiveDescriptorFactory,
			URL archiveUrl,
			String entry) {
		super( archiveDescriptorFactory, archiveUrl, entry );
	}

	@Override
	public void visitArchive(ArchiveContext context) {
		final JarFile jarFile = resolveJarFileReference();
		if ( jarFile == null ) {
			return;
		}

		final Enumeration<? extends ZipEntry> zipEntries = jarFile.entries();
		while ( zipEntries.hasMoreElements() ) {
			final ZipEntry zipEntry = zipEntries.nextElement();
			final String entryName = extractName( zipEntry );

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
				try {
					final InputStream is = new BufferedInputStream( jarFile.getInputStream( zipEntry ) );
					try {
						final JarInputStream jarInputStream = new JarInputStream( is );
						ZipEntry subZipEntry = jarInputStream.getNextEntry();
						while ( subZipEntry != null ) {
							if ( ! subZipEntry.isDirectory() ) {

								final String name = extractName( subZipEntry );
								final String relativeName = extractRelativeName( subZipEntry );
								final InputStreamAccess inputStreamAccess = buildByteBasedInputStreamAccess( name, jarInputStream );

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

								final ArchiveEntryHandler entryHandler = context.obtainArchiveEntryHandler( entry );
								entryHandler.handleEntry( entry, context );
							}

							subZipEntry = jarInputStream.getNextEntry();
						}
					}
					finally {
						is.close();
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

				final ArchiveEntryHandler entryHandler = context.obtainArchiveEntryHandler( entry );
				entryHandler.handleEntry( entry, context );
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
			URL_LOGGER.logUnableToFindFileByUrl( getArchiveUrl(), e );
		}
		catch (URISyntaxException e) {
			URL_LOGGER.logMalformedUrl( getArchiveUrl(), e );
		}
		return null;
	}
}
