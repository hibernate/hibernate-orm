/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.internal;

import java.net.URL;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.boot.archive.spi.ArchiveContext;
import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;

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
		final int subEntryIndex = urlFile.lastIndexOf( '!' );
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
