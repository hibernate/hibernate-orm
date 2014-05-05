/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.archive.internal;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.hibernate.metamodel.archive.spi.AbstractArchiveDescriptorFactory;
import org.hibernate.metamodel.archive.spi.ArchiveDescriptor;
import org.hibernate.metamodel.archive.spi.ArchiveException;
import org.hibernate.internal.util.StringHelper;

import static org.hibernate.internal.UrlMessageBundle.URL_LOGGER;

/**
 * Standard implementation of ArchiveDescriptorFactory
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class StandardArchiveDescriptorFactory extends AbstractArchiveDescriptorFactory {
	/**
	 * Singleton access
	 */
	public static final StandardArchiveDescriptorFactory INSTANCE = new StandardArchiveDescriptorFactory();

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
					throw new ArchiveException( URL_LOGGER.fileDoesNotExist( filePart, url ) );
				}
			}
			catch (URISyntaxException e) {
				throw new ArchiveException( "Unable to visit JAR : " + url, e );
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
}
