/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.internal;

import java.net.MalformedURLException;
import java.net.URI;
import org.hibernate.boot.archive.spi.ArchiveException;

import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.internal.util.StringHelper;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/// Standard implementation of ArchiveDescriptorFactory
///
/// @author Emmanuel Bernard
/// @author Steve Ebersole
public class StandardArchiveDescriptorFactory implements ArchiveDescriptorFactory {

	/// Singleton access
	public static final StandardArchiveDescriptorFactory INSTANCE = new StandardArchiveDescriptorFactory();

	@Override
	public ArchiveDescriptor buildArchiveDescriptor(URL url) {
		return buildArchiveDescriptor( url, "" );
	}

	@Override
	public ArchiveDescriptor buildArchiveDescriptor(URL url, String entry) {
		final String external = url.toExternalForm();
		final int separator = external.indexOf( "!/" );
		if ( separator >= 0 ) {
			try {
				final var container = URI.create( external.substring( external.startsWith( "jar:" ) ? 4 : 0, separator ) ).toURL();
				final String path = external.substring( separator + 2 ).replaceAll( "/+$", "" );
				if ( path.isEmpty() ) {
					return buildArchiveDescriptor( container, entry );
				}
				if ( "file".equals( container.getProtocol() ) && new File( extractLocalFilePath( container ) ).isDirectory() ) {
					return buildArchiveDescriptor( new File( new File( extractLocalFilePath( container ) ), path ).toURI().toURL(), entry );
				}
				if ( path.endsWith( ".jar" ) || path.endsWith( ".par" ) || path.endsWith( ".war" ) ) {
					return new NestedJarDescriptor( URI.create( "jar:" + container + "!/" + path ).toURL() );
				}
				return buildArchiveDescriptor( container, path + ( StringHelper.isEmpty( entry ) ? "/" : "/" + entry ) );
			}
			catch (MalformedURLException e) {
				throw new ArchiveException( "Invalid archive boundary " + url, e );
			}
		}
		final String protocol = url.getProtocol();


		if ( StringHelper.isEmpty( protocol )
				|| "file".equals( protocol )
				|| "vfszip".equals( protocol )
				|| "vfsfile".equals( protocol ) ) {
			final File file = new File( extractLocalFilePath( url ) );
			if ( file.isDirectory() ) {
				return new ExplodedArchiveDescriptor( this, url, entry );
			}
			else {
				return new JarFileBasedArchiveDescriptor( this, url, entry );
			}
		}

		//let's assume the url can return the jar as a zip stream
		return new JarInputStreamBasedArchiveDescriptor( this, url, entry );

	}

	protected String extractLocalFilePath(URL url) {
		final String filePart = url.getFile();
		if ( filePart != null && filePart.indexOf( ' ' ) != -1 ) {
			//unescaped (from the container), keep as is
			return filePart;
		}
		else {
			try {
				return url.toURI().getSchemeSpecificPart();
			}
			catch (URISyntaxException e) {
				throw new IllegalArgumentException(
						"Unable to visit JAR " + url + ". Cause: " + e.getMessage(), e
				);
			}
		}
	}
}
