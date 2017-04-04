/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.spi;

/**
 * Handler for archive entries, based on the classified type of the entry
 *
 * @author Steve Ebersole
 */
public interface ArchiveEntryHandler {
	/**
	 * Handle the entry
	 *
	 * @param entry The entry to handle
	 * @param context The visitation context
	 */
	public void handleEntry(ArchiveEntry entry, ArchiveContext context);
}
