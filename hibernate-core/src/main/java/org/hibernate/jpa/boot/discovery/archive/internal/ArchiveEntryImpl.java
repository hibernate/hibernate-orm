/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.discovery.archive.internal;

import org.hibernate.jpa.boot.discovery.archive.spi.ArchiveEntry;
import org.hibernate.jpa.boot.discovery.archive.spi.InputStreamAccess;

/// Standard implementation of [ArchiveEntry]
///
/// @author Steve Ebersole
public record ArchiveEntryImpl(String name, String relativeName, InputStreamAccess streamAccess) implements ArchiveEntry {
	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getNameWithinArchive() {
		return relativeName;
	}

	@Override
	public InputStreamAccess getStreamAccess() {
		return streamAccess;
	}
}
