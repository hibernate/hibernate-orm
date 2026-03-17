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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;
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

									entryConsumer.accept( new ArchiveEntryImpl( subName, subName, new URI(archiveUrl + "!/" + subName), inputStreamAccess ) );
								}

								subZipEntry = jarInputStream.getNextJarEntry();
							}
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
						finally {
							subJarInputStream.close();
						}
					}
					catch (ArchiveException e) {
						throw e;
					}
					catch (Exception e) {
						throw new ArchiveException( "Error accessing nested jar", e );
					}
				}
				else {
					try {
						final String entryName = extractName( jarEntry );
						final InputStreamAccess inputStreamAccess = buildByteBasedInputStreamAccess( entryName, jarInputStream );

						final String relativeName = extractRelativeName( jarEntry );
						entryConsumer.accept( new ArchiveEntryImpl(
								relativeName,
								relativeName,
								new URI( archiveUrl + "!/" + relativeName ),
								inputStreamAccess
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
										return new ArchiveEntryImpl(
												subName,
												subName,
												URI.create("jar:" + getArchiveUrl().toURI() + "!/" + subName),
												buildByteBasedInputStreamAccess( subName, subJarInputStream )
										);
									}
								}
								subZipEntry = jarInputStream.getNextJarEntry();
							}
						}
						finally {
							subJarInputStream.close();
						}
					}
					catch (URISyntaxException e) {
						throw new ArchiveException( "Unable to create archive entry URI: " + jarEntryName, e );
					}
					catch (Exception e) {
						throw new ArchiveException( "Error accessing nested jar", e );
					}
				}
				else if ( path.equals( jarEntryName ) ) {
					final String entryName = extractName( jarEntry );
					final String relativeName = extractRelativeName( jarEntry );

					try {
						return new ArchiveEntryImpl(
								entryName,
								relativeName,
								URI.create( "jar:" + getArchiveUrl().toURI() + "!/" + entryName ),
								buildByteBasedInputStreamAccess( entryName, jarInputStream )
						);
					}
					catch (URISyntaxException e) {
						throw new ArchiveException( "Unable to create archive entry URI: " + jarEntryName, e );
					}
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
