/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.archive.scan.internal;

import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.archive.scan.internal.ArchiveContextImpl;
import org.hibernate.boot.archive.scan.internal.ArchiveDescriptorInfo;
import org.hibernate.boot.archive.scan.internal.NonClassFileArchiveEntryHandler;
import org.hibernate.boot.archive.scan.internal.PackageInfoArchiveEntryHandler;
import org.hibernate.boot.archive.scan.internal.ScanResultCollector;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.scan.spi.ScanParameters;
import org.hibernate.boot.archive.scan.spi.ScanResult;
import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.boot.archive.spi.ArchiveContext;
import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.archive.spi.JarFileEntryUrlAdjuster;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Standard implementation of the Scanner contract, supporting typical archive walking support where
 * the urls we are processing can be treated using normal file handling.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public class JandexScanner implements Scanner {
	private final ArchiveDescriptorFactory archiveDescriptorFactory;
	private final Map<URL, ArchiveDescriptorInfo> archiveDescriptorCache = new HashMap<>();

	public JandexScanner() {
		this( StandardArchiveDescriptorFactory.INSTANCE );
	}

	public JandexScanner(ArchiveDescriptorFactory archiveDescriptorFactory) {
		this.archiveDescriptorFactory = archiveDescriptorFactory;
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

	private static ArchiveContext buildArchiveContext(boolean isRoot, ScanResultCollector collector) {
		return new ArchiveContextImpl(
				isRoot,
				new JandexClassEntryHandler( collector ),
				new PackageInfoArchiveEntryHandler( collector ),
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
