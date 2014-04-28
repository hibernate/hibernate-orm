/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.boot.scan.spi;

import org.hibernate.jpa.boot.archive.spi.ArchiveContext;
import org.hibernate.jpa.boot.archive.spi.ArchiveEntry;
import org.hibernate.jpa.boot.archive.spi.ArchiveEntryHandler;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

/**
* @author Steve Ebersole
*/
public class ArchiveContextImpl implements ArchiveContext {
	private final ScanEnvironment environment;
	private final boolean isRootUrl;
	private final ArchiveEntryHandlers entryHandlers;

	public ArchiveContextImpl(
			ScanEnvironment environment,
			boolean isRootUrl,
			ArchiveEntryHandlers entryHandlers) {
		this.environment = environment;
		this.isRootUrl = isRootUrl;
		this.entryHandlers = entryHandlers;
	}

	@Override
	@SuppressWarnings("deprecation")
	public PersistenceUnitDescriptor getPersistenceUnitDescriptor() {
		return environment.getPersistenceUnitDescriptor();
	}

	@Override
	public boolean isRootUrl() {
		return isRootUrl;
	}

	@Override
	public ArchiveEntryHandler obtainArchiveEntryHandler(ArchiveEntry entry) {
		final String nameWithinArchive = entry.getNameWithinArchive();

		if ( nameWithinArchive.endsWith( "package-info.class" ) ) {
			return entryHandlers.getPackageInfoHandler();
		}
		else if ( nameWithinArchive.endsWith( ".class" ) ) {
			return entryHandlers.getClassFileHandler();
		}
		else {
			return entryHandlers.getFileHandler();
		}
	}
}
