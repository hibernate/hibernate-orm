/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.internal;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.archive.spi.JarFileEntryUrlAdjuster;
import org.hibernate.internal.util.StringHelper;

import org.jboss.logging.Logger;

/**
 * Standard implementation of ArchiveDescriptorFactory
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class StandardArchiveDescriptorFactory implements ArchiveDescriptorFactory, JarFileEntryUrlAdjuster {
	private static final Logger log = Logger.getLogger( StandardArchiveDescriptorFactory.class );

	/**
	 * Singleton access
	 */
	public static final StandardArchiveDescriptorFactory INSTANCE = new StandardArchiveDescriptorFactory();

	@Override
	public ArchiveDescriptor buildArchiveDescriptor(URL url) {
		return buildArchiveDescriptor( url, "" );
	}

	@Override
	public ArchiveDescriptor buildArchiveDescriptor(URL url, String entry) {
		final String protocol = url.getProtocol();
		if ( "jar".equals( protocol ) ) {
			return new JarProtocolArchiveDescriptor( this, url, entry );
		}
		else if ( StringHelper.isEmpty( protocol )
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
		else {
			//let's assume the url can return the jar as a zip stream
			return new JarInputStreamBasedArchiveDescriptor( this, url, entry );
		}
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

	@Override
	public URL getJarURLFromURLEntry(URL url, String entry) throws IllegalArgumentException {
		return ArchiveHelper.getJarURLFromURLEntry( url, entry );
	}

	@Override
	public URL getURLFromPath(String jarPath) {
		return ArchiveHelper.getURLFromPath( jarPath );
	}

	@Override
	public URL adjustJarFileEntryUrl(URL url, URL rootUrl) {
		final String protocol = url.getProtocol();
		final boolean check = StringHelper.isEmpty( protocol )
				|| "file".equals( protocol )
				|| "vfszip".equals( protocol )
				|| "vfsfile".equals( protocol );
		if ( !check ) {
			return url;
		}

		final String filePart = extractLocalFilePath( url );
		if ( filePart.startsWith( "/" ) || new File(url.getFile()).isAbsolute() ) {
			// the URL is already an absolute form
			return url;
		}
		else {
			// prefer to resolve the relative URL relative to the root PU URL per
			// JPA 2.0 clarification.
			final File rootUrlFile = new File( extractLocalFilePath( rootUrl ) );
			try {
				if ( rootUrlFile.isDirectory() ) {
					// The PU root is a directory (exploded).  Here we can just build
					// the relative File reference and use the Filesystem API to convert
					// to URI and then a URL
					final File combined = new File( rootUrlFile, filePart );
					// make sure it exists..
					if ( combined.exists() ) {
						return combined.toURI().toURL();
					}
				}
				else {
					// The PU root is an archive.  Here we have to build a JAR URL to properly
					// handle the nested entry reference (the !/ part).
					return new URL(
							"jar:" + protocol + "://" + rootUrlFile.getAbsolutePath() + "!/" + filePart
					);
				}
			}
			catch (MalformedURLException e) {
				// allow to pass through to return the original URL
				log.debugf(
						e,
						"Unable to adjust relative <jar-file/> URL [%s] relative to root URL [%s]",
						filePart,
						rootUrlFile.getAbsolutePath()
				);
			}

			return url;
		}
	}
}
