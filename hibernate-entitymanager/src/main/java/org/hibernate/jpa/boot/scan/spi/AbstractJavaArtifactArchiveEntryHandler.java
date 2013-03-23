/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
import org.hibernate.jpa.boot.archive.spi.ArchiveEntryHandler;

/**
 * Base class for commonality between handling class file entries and handling package-info file entries.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractJavaArtifactArchiveEntryHandler implements ArchiveEntryHandler {
	private final ScanOptions scanOptions;

	protected AbstractJavaArtifactArchiveEntryHandler(ScanOptions scanOptions) {
		this.scanOptions = scanOptions;
	}

	/**
	 * Check to see if the incoming name (class/package name) is either:<ul>
	 *     <li>explicitly listed in a {@code <class/>} entry within the {@code <persistence-unit/>}</li>
	 *     <li>whether the scan options indicate that we are allowed to detect this entry</li>
	 * </ul>
	 *
	 * @param context Information about the archive.  Mainly whether it is the root of the PU
	 * @param name The class/package name
	 *
	 * @return {@code true} if the named class/package is either detectable or explicitly listed; {@code false}
	 * otherwise.
	 */
	protected boolean isListedOrDetectable(ArchiveContext context, String name) {
		// IMPL NOTE : protect the isExplicitlyListed call unless needed, since it can take time in a PU
		// with lots of listed classes.  The other conditions are simple boolean flag checks.
		if ( context.isRootUrl() ) {
			return scanOptions.canDetectUnlistedClassesInRoot() || isExplicitlyListed( context, name );
		}
		else {
			return scanOptions.canDetectUnlistedClassesInNonRoot() || isExplicitlyListed( context, name );
		}
	}

	private boolean isExplicitlyListed(ArchiveContext context, String name) {
		return context.getPersistenceUnitDescriptor().getManagedClassNames().contains( name );
	}
}
