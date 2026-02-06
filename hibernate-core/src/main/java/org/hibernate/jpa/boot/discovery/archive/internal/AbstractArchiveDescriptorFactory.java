/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.discovery.archive.internal;


import org.hibernate.jpa.boot.discovery.archive.spi.ArchiveDescriptor;
import org.hibernate.jpa.boot.discovery.archive.spi.ArchiveDescriptorFactory;

import java.net.URL;

/// Base support for implementations of [ArchiveDescriptorFactory].
///
/// @author Steve Ebersole
public abstract class AbstractArchiveDescriptorFactory implements ArchiveDescriptorFactory {
	@Override
	public ArchiveDescriptor buildArchiveDescriptor(URL url) {
		return buildArchiveDescriptor( url, "" );
	}

	@Override
	public URL getJarURLFromURLEntry(URL url, String entry) throws IllegalArgumentException {
		return ArchiveHelper.getJarURLFromURLEntry( url, entry );
	}
}
