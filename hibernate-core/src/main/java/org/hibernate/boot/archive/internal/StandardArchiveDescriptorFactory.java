/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.internal;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.internal.util.StringHelper;

/**
 * Standard implementation of ArchiveDescriptorFactory
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class StandardArchiveDescriptorFactory implements ArchiveDescriptorFactory {
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
			final File file;
			try {
				final String filePart = url.getFile();
				if ( filePart != null && filePart.indexOf( ' ' ) != -1 ) {
					//unescaped (from the container), keep as is
					file = new File( url.getFile() );
				}
				else {
					file = new File( url.toURI().getSchemeSpecificPart() );
				}

				if ( ! file.exists() ) {
					throw new IllegalArgumentException(
							String.format(
									"File [%s] referenced by given URL [%s] does not exist",
									filePart,
									url.toExternalForm()
							)
					);
				}
			}
			catch (URISyntaxException e) {
				throw new IllegalArgumentException(
						"Unable to visit JAR " + url + ". Cause: " + e.getMessage(), e
				);
			}

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

	@Override
	public URL getJarURLFromURLEntry(URL url, String entry) throws IllegalArgumentException {
		return ArchiveHelper.getJarURLFromURLEntry( url, entry );
	}

	@Override
	public URL getURLFromPath(String jarPath) {
		return ArchiveHelper.getURLFromPath( jarPath );
	}
}
