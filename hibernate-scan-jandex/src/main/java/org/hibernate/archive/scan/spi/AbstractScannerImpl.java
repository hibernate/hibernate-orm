/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.archive.scan.spi;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.archive.scan.internal.NoopEntryHandler;
import org.hibernate.archive.scan.internal.ScanResultCollector;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.scan.spi.ScanParameters;
import org.hibernate.boot.archive.scan.spi.ScanResult;
import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.boot.archive.spi.ArchiveContext;
import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.archive.spi.ArchiveEntry;
import org.hibernate.boot.archive.spi.ArchiveEntryHandler;
import org.hibernate.boot.archive.spi.JarFileEntryUrlAdjuster;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractScannerImpl implements Scanner {
	private final ArchiveDescriptorFactory archiveDescriptorFactory;
	private final Map<URL, ArchiveDescriptorInfo> archiveDescriptorCache = new HashMap<>();

	protected AbstractScannerImpl(ArchiveDescriptorFactory archiveDescriptorFactory) {
		this.archiveDescriptorFactory = archiveDescriptorFactory;
	}

	@Override
	public ScanResult scan(ScanEnvironment environment, ScanOptions options, ScanParameters parameters) {
		final ScanResultCollector collector = new ScanResultCollector( environment, options, parameters );

		if ( environment.getNonRootUrls() != null ) {
			final ArchiveContext context = new ArchiveContextImpl( false, collector );
			for ( URL url : environment.getNonRootUrls() ) {
				final ArchiveDescriptor descriptor = buildArchiveDescriptor( url, environment, false );
				descriptor.visitArchive( context );
			}
		}

		if ( environment.getRootUrl() != null ) {
			final ArchiveContext context = new ArchiveContextImpl( true, collector );
			final ArchiveDescriptor descriptor = buildArchiveDescriptor( environment.getRootUrl(), environment, true );
			descriptor.visitArchive( context );
		}

		return collector.toScanResult();
	}


	private ArchiveDescriptor buildArchiveDescriptor(
			URL url,
			ScanEnvironment environment,
			boolean isRootUrl) {
		final ArchiveDescriptor descriptor;
		final ArchiveDescriptorInfo descriptorInfo = archiveDescriptorCache.get( url );
		if ( descriptorInfo == null ) {
			if ( !isRootUrl && archiveDescriptorFactory instanceof JarFileEntryUrlAdjuster ) {
				url = ( (JarFileEntryUrlAdjuster) archiveDescriptorFactory ).adjustJarFileEntryUrl( url, environment.getRootUrl() );
			}
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

	/**
	 * Handle &lt;jar-file/&gt; references from a persistence.xml file.
	 *
	 * JPA allows for  to be specific
	 * @return The resolved non-root URL
	 */
	protected URL resolveNonRootUrl(URL url) {
		return null;
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


	public static class ArchiveContextImpl implements ArchiveContext {
		private final boolean isRootUrl;

		private final ClassFileArchiveEntryHandler classEntryHandler;
		private final PackageInfoArchiveEntryHandler packageEntryHandler;
		private final ArchiveEntryHandler fileEntryHandler;

		public ArchiveContextImpl(boolean isRootUrl, ScanResultCollector scanResultCollector) {
			this.isRootUrl = isRootUrl;

			this.classEntryHandler = new ClassFileArchiveEntryHandler( scanResultCollector );
			this.packageEntryHandler = new PackageInfoArchiveEntryHandler( scanResultCollector );
			this.fileEntryHandler = new NonClassFileArchiveEntryHandler( scanResultCollector );
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
}
