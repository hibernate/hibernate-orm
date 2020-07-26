/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.scan.internal;

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
