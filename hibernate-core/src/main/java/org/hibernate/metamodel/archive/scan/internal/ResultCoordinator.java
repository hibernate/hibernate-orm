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
package org.hibernate.metamodel.archive.scan.internal;

import org.hibernate.metamodel.archive.scan.spi.ArchiveEntryHandlers;
import org.hibernate.metamodel.archive.scan.spi.ClassFileArchiveEntryHandler;
import org.hibernate.metamodel.archive.scan.spi.NonClassFileArchiveEntryHandler;
import org.hibernate.metamodel.archive.scan.spi.PackageInfoArchiveEntryHandler;
import org.hibernate.metamodel.archive.spi.ArchiveEntryHandler;

/**
 * @author Steve Ebersole
 */
public class ResultCoordinator implements ArchiveEntryHandlers {
	private final ClassFileArchiveEntryHandler classEntryHandler;
	private final PackageInfoArchiveEntryHandler packageEntryHandler;
	private final ArchiveEntryHandler fileEntryHandler;

	public ResultCoordinator(ScanResultCollector resultCollector) {
		this.classEntryHandler = new ClassFileArchiveEntryHandler( resultCollector );
		this.packageEntryHandler = new PackageInfoArchiveEntryHandler( resultCollector );
		this.fileEntryHandler = new NonClassFileArchiveEntryHandler( resultCollector );
	}

	@Override
	public ArchiveEntryHandler getClassFileHandler() {
		return classEntryHandler;
	}

	@Override
	public ArchiveEntryHandler getPackageInfoHandler() {
		return packageEntryHandler;
	}

	@Override
	public ArchiveEntryHandler getFileHandler() {
		return fileEntryHandler;
	}
}
