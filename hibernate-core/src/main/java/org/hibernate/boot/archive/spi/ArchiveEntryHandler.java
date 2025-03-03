/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	void handleEntry(ArchiveEntry entry, ArchiveContext context);
}
