/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.internal;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.HibernateException;
import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.archive.spi.ArchiveException;
import org.hibernate.boot.archive.spi.InputStreamAccess;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;

import static org.hibernate.boot.BootLogging.BOOT_LOGGER;

/// Helper for dealing with archives
///
/// @author Emmanuel Bernard
/// @author Steve Ebersole
public class ArchiveHelper {

	private ArchiveHelper() {
	}

	/// Resolves a named `<jar-file/>` reference assuming the name is a URL.
	/// `null` will be returned if the reference is not a URL.
	public static ArchiveDescriptor resolveJarFileReferenceAsUrl(
			String jarFileReference,
			ArchiveDescriptorFactory archiveDescriptorFactory) {
		if ( jarFileReference.indexOf( ':' ) < 1 ) {
			return null;
		}
		try {
			var url = new URL( jarFileReference );
			return archiveDescriptorFactory.buildArchiveDescriptor( url );
		}
		catch (MalformedURLException ignore) {
		}
		return null;
	}

	public static InputStreamAccess buildByteBasedInputStreamAccess(String name, InputStream inputStream) {
		// because of how jar InputStreams work we need to extract the bytes immediately.  However, we
		// do delay the creation of the ByteArrayInputStreams until needed
		final byte[] bytes = ArchiveHelper.getBytesFromInputStreamSafely( inputStream );
		return new ByteArrayInputStreamAccess( name, bytes );
	}


	/// Get the JAR URL of the JAR containing the given entry.
	/// Method used in a non-managed environment.
	///
	/// @param url URL pointing to the known file in the JAR
	/// @param entry file known to be in the JAR
	/// @return the JAR URL
	/// @throws IllegalArgumentException if none URL is found
	public static URL getJarURLFromURLEntry(URL url, String entry) throws IllegalArgumentException {
		URL jarUrl;
		String file = url.getFile();
		if ( ! entry.startsWith( "/" ) ) {
			entry = "/" + entry;
		}
		file = file.substring( 0, file.length() - entry.length() );
		if ( file.endsWith( "!" ) ) {
			file = file.substring( 0, file.length() - 1 );
		}
		try {
			final String protocol = url.getProtocol();

			if ( "jar".equals( protocol ) || "wsjar".equals( protocol ) ) {
				//Original URL is like jar:protocol
				//WebSphere has it's own way
				jarUrl = new URL( file );
				if ( "file".equals( jarUrl.getProtocol() ) ) {
					if ( file.indexOf( ' ' ) != -1 ) {
						//not escaped, need to voodoo; goes by toURI to escape the path
						jarUrl = new File( jarUrl.getFile() ).toURI().toURL();
					}
				}
			}
			else if ( "zip".equals( protocol )
					//OC4J prevent ejb.jar access (ie everything without path)
					|| "code-source".equals( url.getProtocol() )
					//if no wrapping is done
					|| "file".equals( protocol ) ) {
				//we have extracted the zip file, so it should be read as a file
				if ( file.indexOf( ' ' ) != -1 ) {
					//not escaped, need to voodoo; goes by toURI to escape the path
					jarUrl = new File(file).toURI().toURL();
				}
				else {
					jarUrl = new File(file).toURL();
				}
			}
			else {
				try {
					//We reconstruct the URL probably to make it work in some specific environments
					//Forgot the exact details, sorry (and the Git history does not help)
					jarUrl = new URL( protocol, url.getHost(), url.getPort(), file );
				}
				//HHH-6442: Arquilian
				catch ( final MalformedURLException e ) {
					//Just use the provided URL as-is, likely it has a URLStreamHandler
					//associated w/ the instance
					jarUrl = url;
				}
			}
		}
		catch (MalformedURLException e) {
			throw new IllegalArgumentException(
					"Unable to determine JAR Url from " + url + ". Cause: " + e.getMessage()
			);
		}
		BOOT_LOGGER.jarUrlFromUrlEntry( String.valueOf(url), String.valueOf(jarUrl) );
		return jarUrl;
	}


	/// Attempt to resolve a `<jar-file/>` reference using a [ClassLoader].
	/// This form should only be used when we are parsing the `persistence.xml` ourselves
	/// and do not have an [ArchiveDescriptor].
	///
	/// @see org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor
	public static URL resolveJarFileReference(String jarFileReference, URLClassLoader urlClassLoader) {
		assert jarFileReference != null;
		if ( jarFileReference.startsWith( "file://" ) ) {
			final var trimmed = jarFileReference.substring( "file://".length() );
			return asFileReference( jarFileReference, trimmed );
		}
		else {
			// try it as a File reference
			var asFileReference = tryAsFileReference( jarFileReference, jarFileReference );
			if ( asFileReference != null ) {
				return asFileReference;
			}

			// try it as a path relative to the container
			final URL relativeReference = urlClassLoader.getResource( jarFileReference );
			if ( relativeReference != null ) {
				return relativeReference;
			}
		}

		throw new HibernateException( "Could not resolve jar-file: " + jarFileReference );
	}

	private static URL tryAsFileReference(String jarFileReference, String trimmed) {
		final File jarFile = new File( trimmed );
		try {
			if ( jarFile.exists() ) {
				return jarFile.toURI().toURL();
			}
		}
		catch (MalformedURLException ignore) {
		}
		return null;
	}


	public static URL asFileReference(String jarFileReference, String trimmed) {
		final File jarFile = new File( trimmed );
		if ( !jarFile.exists() ) {
			throw new HibernateException( "Could not find specified jar-file: " + jarFileReference );
		}
		try {
			return jarFile.toURI().toURL();
		}
		catch (MalformedURLException e) {
			throw new HibernateException( "Could not access specified jar-file: " + jarFileReference, e );
		}
	}

	/// Extracts the bytes out of an InputStream.  This form is the same as [#getBytesFromInputStream]
	/// except that any [IOException] is wrapped as (runtime) [ArchiveException]
	///
	/// @param inputStream The stream from which to extract bytes.
	///
	/// @return The bytes
	///
	/// @throws ArchiveException Indicates a problem accessing the stream
	public static byte[] getBytesFromInputStreamSafely(InputStream inputStream) throws ArchiveException {
		try {
			return getBytesFromInputStream( inputStream );
		}
		catch (IOException e) {
			throw new ArchiveException( "Unable to extract bytes from InputStream", e );
		}
	}

	/// Extracts the bytes out of an InputStream.
	///
	/// @param inputStream The stream from which to extract bytes.
	///
	/// @return The bytes
	///
	/// @throws IOException Indicates a problem accessing the stream
	///
	/// @see #getBytesFromInputStreamSafely(InputStream)
	public static byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
		// Optimized by HHH-7835
		int size;
		final List<byte[]> data = new LinkedList<>();
		final int bufferSize = 4096;
		byte[] tmpByte = new byte[bufferSize];
		int offset = 0;
		int total = 0;
		for ( ;; ) {
			size = inputStream.read( tmpByte, offset, bufferSize - offset );
			if ( size == -1 ) {
				break;
			}

			offset += size;

			if ( offset == tmpByte.length ) {
				data.add( tmpByte );
				tmpByte = new byte[bufferSize];
				offset = 0;
				total += tmpByte.length;
			}
		}

		final byte[] result = new byte[total + offset];
		int count = 0;
		for ( byte[] arr : data ) {
			System.arraycopy( arr, 0, result, count * arr.length, arr.length );
			count++;
		}
		System.arraycopy( tmpByte, 0, result, count * tmpByte.length, offset );

		return result;
	}

	@Nullable
	public static ArchiveDescriptor standardJarFileReferenceResolution(
			@NonNull String jarFileReference,
			ArchiveDescriptorFactory archiveDescriptorFactory) {

		// try it as a URL
		final var asUrl = resolveJarFileReferenceAsUrl( jarFileReference, archiveDescriptorFactory );
		if ( asUrl != null ) {
			return asUrl;
		}

		// try it as a File path
		try {
			var file = new File( jarFileReference );
			if ( file.exists() ) {
				return archiveDescriptorFactory.buildArchiveDescriptor( file.toURI().toURL() );
			}
		}
		catch (MalformedURLException e) {
			throw new ArchiveException( "Unable to convert jar File to URL [" + jarFileReference + "]", e );
		}

		return null;
	}
}
