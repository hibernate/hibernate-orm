/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.scan.internal;

import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.scan.spi.ScanningContext;

import java.util.Map;

/// Simple implementation of [ScanningContext].
///
/// @author Steve Ebersole
public final class ScanningContextImpl implements ScanningContext {
	private final ArchiveDescriptorFactory archiveDescriptorFactory;
	private final Map<Object, Object> properties;

	public ScanningContextImpl(
			ArchiveDescriptorFactory archiveDescriptorFactory,
			@SuppressWarnings("rawtypes") Map properties) {
		this.archiveDescriptorFactory = archiveDescriptorFactory;
		//noinspection unchecked
		this.properties = properties;
	}

	@Override
	public Map<Object, Object> getProperties() {
		return properties;
	}

	@Override
	public ArchiveDescriptorFactory getArchiveDescriptorFactory() {
		return archiveDescriptorFactory;
	}
}
