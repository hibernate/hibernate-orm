/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.scan.internal;

import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.scan.spi.ScanParameters;
import org.hibernate.boot.archive.scan.spi.ScanResult;
import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.boot.archive.spi.ArchiveContext;
import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.archive.spi.JarFileEntryUrlAdjuster;
import org.hibernate.service.ServiceRegistry;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Steve Ebersole
 */
public class StandardScanner implements Scanner {
	private final ArchiveDescriptorFactory archiveDescriptorFactory;
	private final ServiceRegistry serviceRegistry;

	private final Map<URL, ArchiveDescriptorInfo> archiveDescriptorCache = new HashMap<>();

	public StandardScanner(ServiceRegistry serviceRegistry) {
		this( StandardArchiveDescriptorFactory.INSTANCE, serviceRegistry );
	}

	public StandardScanner(ArchiveDescriptorFactory archiveDescriptorFactory) {
		this( archiveDescriptorFactory, null );
	}

	public StandardScanner(ArchiveDescriptorFactory archiveDescriptorFactory, ServiceRegistry serviceRegistry) {
		this.archiveDescriptorFactory = archiveDescriptorFactory != null
				? archiveDescriptorFactory
				: StandardArchiveDescriptorFactory.INSTANCE;
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public ScanResult scan(ScanEnvironment environment, ScanOptions options, ScanParameters parameters) {
		final ScanResultCollector collector = new ScanResultCollector( environment, options, parameters );

		if ( environment.getNonRootUrls() != null ) {
			final ArchiveContext context = buildArchiveContext( false, collector );
			for ( URL url : environment.getNonRootUrls() ) {
				final ArchiveDescriptor descriptor = buildArchiveDescriptor( url, environment, false );
				descriptor.visitArchive( context );
			}
		}

		if ( environment.getRootUrl() != null ) {
			final ArchiveContext context = buildArchiveContext( true, collector );
			final ArchiveDescriptor descriptor = buildArchiveDescriptor( environment.getRootUrl(), environment, true );
			descriptor.visitArchive( context );
		}

		return collector.toScanResult();
	}

	private ArchiveContext buildArchiveContext(boolean isRoot, ScanResultCollector collector) {
		return new ArchiveContextImpl(
				isRoot,
				serviceRegistry != null
						? new ClassFileArchiveEntryHandler( collector, serviceRegistry )
						: NoopEntryHandler.NOOP_INSTANCE,
				serviceRegistry != null
						? new PackageInfoArchiveEntryHandler( collector )
						: NoopEntryHandler.NOOP_INSTANCE,
				new NonClassFileArchiveEntryHandler( collector )
		);
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

	@SuppressWarnings("UnusedParameters")
	protected void validateReuse(ArchiveDescriptorInfo descriptor, boolean root) {
		// is it really reasonable that a single url be processed multiple times?
		// for now, throw an exception, mainly because I am interested in situations where this might happen
		throw new IllegalStateException( "ArchiveDescriptor reused; can URLs be processed multiple times?" );
	}
}
