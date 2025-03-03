/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.scan.internal;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.archive.scan.spi.ClassDescriptor;
import org.hibernate.boot.archive.scan.spi.MappingFileDescriptor;
import org.hibernate.boot.archive.scan.spi.PackageDescriptor;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.scan.spi.ScanParameters;
import org.hibernate.boot.archive.scan.spi.ScanResult;
import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.archive.spi.ArchiveEntry;
import org.hibernate.boot.archive.spi.InputStreamAccess;

/**
 * Implementation of Scanner that does no scanning.
 * It simply searches for {@code META-INF/orm.xml} path(s) relative
 * to the {@linkplain ScanEnvironment#getRootUrl() root} and
 * {@linkplain ScanEnvironment#getNonRootUrls() non-root} URLs.
 * Used for optimizing startup time when full scanning is not needed.
 *
 * @author Petteri Pitkanen
 */
public class DisabledScanner implements Scanner {

	private final ArchiveDescriptorFactory archiveDescriptorFactory;

	public DisabledScanner() {
		this( StandardArchiveDescriptorFactory.INSTANCE );
	}

	private DisabledScanner(ArchiveDescriptorFactory archiveDescriptorFactory) {
		this.archiveDescriptorFactory = archiveDescriptorFactory;
	}

	@Override
	public ScanResult scan(final ScanEnvironment environment, final ScanOptions options, final ScanParameters parameters) {
		final Set<MappingFileDescriptor> discoveredMappingFiles = new HashSet<>();

		if ( environment.getNonRootUrls() != null ) {
			for ( URL url : environment.getNonRootUrls() ) {
				final ArchiveDescriptor archiveDescriptor = archiveDescriptorFactory.buildArchiveDescriptor( url );
				final ArchiveEntry entry = archiveDescriptor.findEntry( "META-INF/orm.xml" );
				if ( entry != null ) {
					discoveredMappingFiles.add( new MappingFileDescriptorImpl( entry.getNameWithinArchive(), entry.getStreamAccess() ) );
				}
			}
		}

		if ( environment.getRootUrl() != null ) {
			final ArchiveDescriptor archiveDescriptor = archiveDescriptorFactory.buildArchiveDescriptor( environment.getRootUrl() );
			final ArchiveEntry entry = archiveDescriptor.findEntry( "META-INF/orm.xml" );
			if ( entry != null ) {
				discoveredMappingFiles.add( new MappingFileDescriptorImpl( entry.getNameWithinArchive(), entry.getStreamAccess() ) );
			}
		}

		return new ScanResult() {
			@Override
			public Set<PackageDescriptor> getLocatedPackages() {
				return Collections.emptySet();
			}

			@Override
			public Set<ClassDescriptor> getLocatedClasses() {
				return Collections.emptySet();
			}

			@Override
			public Set<MappingFileDescriptor> getLocatedMappingFiles() {
				return discoveredMappingFiles;
			}
		};
	}

	public static class MappingFileDescriptorImpl implements MappingFileDescriptor {
		private final String name;
		private final InputStreamAccess streamAccess;

		public MappingFileDescriptorImpl(String name, InputStreamAccess streamAccess) {
			this.name = name;
			this.streamAccess = streamAccess;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public InputStreamAccess getStreamAccess() {
			return streamAccess;
		}
	}
}
