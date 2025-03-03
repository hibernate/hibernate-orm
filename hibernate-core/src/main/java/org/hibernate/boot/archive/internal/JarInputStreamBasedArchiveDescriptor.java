/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.internal;

import java.io.IOException;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.boot.archive.spi.AbstractArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveContext;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.archive.spi.ArchiveEntry;
import org.hibernate.boot.archive.spi.ArchiveException;
import org.hibernate.boot.archive.spi.InputStreamAccess;

import static org.hibernate.internal.log.UrlMessageBundle.URL_MESSAGE_LOGGER;

/**
 * An {@code ArchiveDescriptor} that works on archives accessible through a {@link JarInputStream}.
 *
 * @implNote This is less efficient implementation than {@link JarFileBasedArchiveDescriptor}.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class JarInputStreamBasedArchiveDescriptor extends AbstractArchiveDescriptor {

	/**
	 * Constructs a JarInputStreamBasedArchiveDescriptor
	 *
	 * @param archiveDescriptorFactory The factory creating this
	 * @param url The url to the JAR file
	 * @param entry The prefix for entries within the JAR url
	 */
	public JarInputStreamBasedArchiveDescriptor(
			ArchiveDescriptorFactory archiveDescriptorFactory,
			URL url,
			String entry) {
		super( archiveDescriptorFactory, url, entry );
	}

	@Override
	public void visitArchive(ArchiveContext context) {
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

									final ArchiveEntry entry = new ArchiveEntry() {
										@Override
										public String getName() {
											return subName;
										}

										@Override
										public String getNameWithinArchive() {
											return subName;
										}

										@Override
										public InputStreamAccess getStreamAccess() {
											return inputStreamAccess;
										}
									};

									context.obtainArchiveEntryHandler( entry ).handleEntry( entry, context );
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
					final InputStreamAccess inputStreamAccess
							= buildByteBasedInputStreamAccess( entryName, jarInputStream );

					final String relativeName = extractRelativeName( jarEntry );

					final ArchiveEntry entry = new ArchiveEntry() {
						@Override
						public String getName() {
							return entryName;
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

			jarInputStream.close();
		}
		catch (IOException ioe) {
			throw new ArchiveException(
					String.format( "Error accessing JarInputStream [%s]", getArchiveUrl() ),
					ioe
			);
		}
	}

	@Override
	public @Nullable ArchiveEntry findEntry(String path) {
		final JarInputStream jarInputStream;
		try {
			jarInputStream = new JarInputStream( getArchiveUrl().openStream() );
		}
		catch (Exception e) {
			//really should catch IOException but Eclipse is buggy and raise NPE...
			URL_MESSAGE_LOGGER.logUnableToFindFileByUrl( getArchiveUrl(), e );
			return null;
		}

		try {
			JarEntry jarEntry;
			while ( ( jarEntry = jarInputStream.getNextJarEntry() ) != null ) {
				final String jarEntryName = jarEntry.getName();
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
									if ( path.equals( subName ) ) {
										final InputStreamAccess inputStreamAccess = buildByteBasedInputStreamAccess(
												subName,
												subJarInputStream
										);

										return new ArchiveEntry() {
											@Override
											public String getName() {
												return subName;
											}

											@Override
											public String getNameWithinArchive() {
												return subName;
											}

											@Override
											public InputStreamAccess getStreamAccess() {
												return inputStreamAccess;
											}
										};
									}
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
				else if ( path.equals( jarEntryName ) ) {
					final String entryName = extractName( jarEntry );
					final InputStreamAccess inputStreamAccess
							= buildByteBasedInputStreamAccess( entryName, jarInputStream );

					final String relativeName = extractRelativeName( jarEntry );

					return new ArchiveEntry() {
						@Override
						public String getName() {
							return entryName;
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
				}
			}

		}
		catch (IOException ioe) {
			throw new ArchiveException(
					String.format( "Error accessing JarInputStream [%s]", getArchiveUrl() ),
					ioe
			);
		}
		finally {
			try {
				jarInputStream.close();
			}
			catch (IOException e) {
				// Ignore
			}
		}
		return null;
	}
}
