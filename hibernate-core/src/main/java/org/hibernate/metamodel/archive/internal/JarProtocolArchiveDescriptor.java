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

import java.net.URL;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.metamodel.archive.spi.ArchiveContext;
import org.hibernate.metamodel.archive.spi.ArchiveDescriptor;
import org.hibernate.metamodel.archive.spi.ArchiveDescriptorFactory;

/**
 * An ArchiveDescriptor implementation for handling archives whose url reported a JAR protocol (i.e., jar://).
 *
 * @author Steve Ebersole
 */
public class JarProtocolArchiveDescriptor implements ArchiveDescriptor {
	private final ArchiveDescriptor delegateDescriptor;

	/**
	 * Constructs a JarProtocolArchiveDescriptor
	 *
	 * @param archiveDescriptorFactory The factory creating this
	 * @param url The url to the JAR file
	 * @param incomingEntry The prefix for entries within the JAR url
	 */
	public JarProtocolArchiveDescriptor(
			ArchiveDescriptorFactory archiveDescriptorFactory,
			URL url,
			String incomingEntry) {
		if ( incomingEntry != null && incomingEntry.length() > 0 ) {
			throw new IllegalArgumentException( "jar:jar: not supported: " + url );
		}

		final String urlFile = url.getFile();
		final int subEntryIndex = urlFile.lastIndexOf( "!" );
		if ( subEntryIndex == -1 ) {
			throw new AssertionFailure( "JAR URL does not contain '!/' :" + url );
		}

		final String subEntry;
		if ( subEntryIndex + 1 >= urlFile.length() ) {
			subEntry = "";
		}
		else {
			subEntry = urlFile.substring( subEntryIndex + 1 );
		}

		final URL fileUrl = archiveDescriptorFactory.getJarURLFromURLEntry( url, subEntry );
		delegateDescriptor = archiveDescriptorFactory.buildArchiveDescriptor( fileUrl, subEntry );
	}

	@Override
	public void visitArchive(ArchiveContext context) {
		delegateDescriptor.visitArchive( context );
	}
}
