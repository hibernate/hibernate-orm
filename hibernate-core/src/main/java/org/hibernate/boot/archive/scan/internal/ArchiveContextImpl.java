/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.scan.internal;

import org.hibernate.boot.archive.spi.ArchiveContext;
import org.hibernate.boot.archive.spi.ArchiveEntry;
import org.hibernate.boot.archive.spi.ArchiveEntryHandler;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Steve Ebersole
 */
public class ArchiveContextImpl implements ArchiveContext {
	private final boolean isRootUrl;

	private final ArchiveEntryHandler classEntryHandler;
	private final ArchiveEntryHandler packageEntryHandler;
	private final ArchiveEntryHandler fileEntryHandler;

	public ArchiveContextImpl(
			boolean isRootUrl,
			ScanResultCollector scanResultCollector,
			ServiceRegistry serviceRegistry) {
		this(
				isRootUrl,
				new ClassFileArchiveEntryHandler( scanResultCollector, serviceRegistry ),
				new PackageInfoArchiveEntryHandler( scanResultCollector ),
				new NonClassFileArchiveEntryHandler( scanResultCollector )
		);
	}

	public ArchiveContextImpl(
			boolean isRootUrl,
			ArchiveEntryHandler classEntryHandler,
			ArchiveEntryHandler packageEntryHandler,
			ArchiveEntryHandler fileEntryHandler) {
		this.isRootUrl = isRootUrl;
		this.classEntryHandler = classEntryHandler;
		this.packageEntryHandler = packageEntryHandler;
		this.fileEntryHandler = fileEntryHandler;
	}

	public ArchiveContextImpl(
			boolean isRootUrl,
			ArchiveEntryHandler classEntryHandler,
			ScanResultCollector scanResultCollector) {
		this(
				isRootUrl,
				classEntryHandler,
				new PackageInfoArchiveEntryHandler( scanResultCollector ),
				new NonClassFileArchiveEntryHandler( scanResultCollector )
		);
	}

	@Override
	public boolean isRootUrl() {
		return isRootUrl;
	}

	@Override
	public ArchiveEntryHandler obtainArchiveEntryHandler(ArchiveEntry entry) {
		final String nameWithinArchive = entry.getNameWithinArchive();

		if ( nameWithinArchive.endsWith( "package-info.class" ) ) {
			return packageEntryHandler;
		}
		else if ( nameWithinArchive.endsWith( "module-info.class" ) ) {
			//There's two reasons to skip this: the most important one is that Jandex
			//is unable to analyze them, so we need to dodge it.
			//Secondarily, we have no use for these so let's save the effort.
			return NoopEntryHandler.NOOP_INSTANCE;
		}
		else if ( nameWithinArchive.endsWith( ".class" ) ) {
			return classEntryHandler;
		}
		else {
			return fileEntryHandler;
		}
	}
}
