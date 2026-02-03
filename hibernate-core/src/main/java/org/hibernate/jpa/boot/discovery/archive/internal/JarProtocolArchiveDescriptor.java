/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.discovery.archive.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.jpa.boot.discovery.archive.spi.ArchiveDescriptor;
import org.hibernate.jpa.boot.discovery.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.jpa.boot.discovery.archive.spi.ArchiveEntry;
import org.hibernate.internal.util.StringHelper;

import java.net.URL;
import java.util.function.Consumer;

/// An ArchiveDescriptor implementation for handling archives whose url reported a JAR protocol (i.e., jar://).
///
/// @author Steve Ebersole
public class JarProtocolArchiveDescriptor implements ArchiveDescriptor {
	private final ArchiveDescriptor delegateDescriptor;

	/// Constructs a JarProtocolArchiveDescriptor
	///
	/// @param archiveDescriptorFactory The factory creating this
	/// @param url The url to the JAR file
	/// @param incomingEntry The prefix for entries within the JAR url
	public JarProtocolArchiveDescriptor(
			ArchiveDescriptorFactory archiveDescriptorFactory,
			URL url,
			String incomingEntry) {
		if ( StringHelper.isNotEmpty( incomingEntry ) ) {
			throw new IllegalArgumentException( "jar:jar: not supported: " + url );
		}

		final String urlFile = url.getFile();
		final int subEntryIndex = urlFile.lastIndexOf( '!' );
		if ( subEntryIndex == -1 ) {
			throw new AssertionFailure( "JAR URL does not contain '!/' : " + url );
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
	public void visitClassEntries(Consumer<ArchiveEntry> entryConsumer) {
		delegateDescriptor.visitClassEntries( entryConsumer );
	}
}
