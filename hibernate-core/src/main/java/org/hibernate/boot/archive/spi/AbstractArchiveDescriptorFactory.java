/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.spi;

import java.net.URL;

import org.hibernate.boot.archive.internal.ArchiveHelper;

/**
 * @author Steve Ebersole
 */
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
