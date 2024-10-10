/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.archive.scan.internal;

import org.hibernate.boot.archive.spi.ArchiveContext;
import org.hibernate.boot.archive.spi.ArchiveEntry;
import org.hibernate.boot.archive.spi.ArchiveEntryHandler;

public final class NoopEntryHandler implements ArchiveEntryHandler {

	public static final ArchiveEntryHandler NOOP_INSTANCE = new NoopEntryHandler();

	private NoopEntryHandler() {
		//Use the singleton.
	}

	@Override
	public void handleEntry(ArchiveEntry entry, ArchiveContext context) {
	}
}
