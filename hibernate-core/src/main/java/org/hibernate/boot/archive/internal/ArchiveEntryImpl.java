/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.internal;

import org.hibernate.boot.archive.spi.ArchiveEntry;
import org.hibernate.boot.archive.spi.InputStreamAccess;

import java.net.URI;

/// Standard implementation of [ArchiveEntry]
///
/// @author Steve Ebersole
public record ArchiveEntryImpl(
		String name,
		String relativeName,
		URI uri,
		InputStreamAccess streamAccess) implements ArchiveEntry {
	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getNameWithinArchive() {
		return relativeName;
	}

	@Override
	public URI getUri() {
		return uri;
	}

	@Override
	public InputStreamAccess getStreamAccess() {
		return streamAccess;
	}
}
