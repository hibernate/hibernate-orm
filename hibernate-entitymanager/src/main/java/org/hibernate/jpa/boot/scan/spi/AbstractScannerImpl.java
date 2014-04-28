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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.jpa.boot.archive.spi.ArchiveContext;
import org.hibernate.jpa.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.jpa.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.jpa.boot.scan.internal.ResultCoordinator;
import org.hibernate.jpa.boot.scan.internal.ScanResultCollector;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractScannerImpl implements Scanner {
	private final ArchiveDescriptorFactory archiveDescriptorFactory;
	private final Map<URL, ArchiveDescriptorInfo> archiveDescriptorCache = new HashMap<URL, ArchiveDescriptorInfo>();

	protected AbstractScannerImpl(ArchiveDescriptorFactory archiveDescriptorFactory) {
		this.archiveDescriptorFactory = archiveDescriptorFactory;
	}

	@Override
	public ScanResult scan(ScanEnvironment environment, ScanOptions options) {
		final ScanResultCollector collector = new ScanResultCollector( environment, options );
		final ResultCoordinator resultCoordinator = new ResultCoordinator( collector );

		if ( environment.getNonRootUrls() != null ) {
			final ArchiveContext context = new ArchiveContextImpl( environment, false, resultCoordinator );
			for ( URL url : environment.getNonRootUrls() ) {
				final ArchiveDescriptor descriptor = buildArchiveDescriptor( url, false );
				descriptor.visitArchive( context );
			}
		}

		if ( environment.getRootUrl() != null ) {
			final ArchiveContext context = new ArchiveContextImpl( environment, true, resultCoordinator );
			final ArchiveDescriptor descriptor = buildArchiveDescriptor( environment.getRootUrl(), true );
			descriptor.visitArchive( context );
		}

		return collector.toScanResult();
	}


	private ArchiveDescriptor buildArchiveDescriptor(URL url, boolean isRootUrl) {
		final ArchiveDescriptor descriptor;
		final ArchiveDescriptorInfo descriptorInfo = archiveDescriptorCache.get( url );
		if ( descriptorInfo == null ) {
			descriptor = archiveDescriptorFactory.buildArchiveDescriptor( url );
			archiveDescriptorCache.put(
					url,
					new ArchiveDescriptorInfo( descriptor, isRootUrl )
			);
		}
		else {
			validateReuse( descriptorInfo, isRootUrl );
			descriptor = descriptorInfo.archiveDescriptor;
		}
		return descriptor;
	}

	// This needs to be protected and attributes/constructor visible in case
	// a custom scanner needs to override validateReuse.
	protected static class ArchiveDescriptorInfo {
		public final ArchiveDescriptor archiveDescriptor;
		public final boolean isRoot;

		public ArchiveDescriptorInfo(ArchiveDescriptor archiveDescriptor, boolean isRoot) {
			this.archiveDescriptor = archiveDescriptor;
			this.isRoot = isRoot;
		}
	}

	@SuppressWarnings("UnusedParameters")
	protected void validateReuse(ArchiveDescriptorInfo descriptor, boolean root) {
		// is it really reasonable that a single url be processed multiple times?
		// for now, throw an exception, mainly because I am interested in situations where this might happen
		throw new IllegalStateException( "ArchiveDescriptor reused; can URLs be processed multiple times?" );
	}

}
