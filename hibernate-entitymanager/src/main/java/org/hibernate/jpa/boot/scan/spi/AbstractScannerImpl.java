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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.jpa.boot.archive.spi.ArchiveContext;
import org.hibernate.jpa.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.jpa.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.jpa.boot.archive.spi.ArchiveEntry;
import org.hibernate.jpa.boot.archive.spi.ArchiveEntryHandler;
import org.hibernate.jpa.boot.internal.ClassDescriptorImpl;
import org.hibernate.jpa.boot.internal.MappingFileDescriptorImpl;
import org.hibernate.jpa.boot.internal.PackageDescriptorImpl;
import org.hibernate.jpa.boot.spi.ClassDescriptor;
import org.hibernate.jpa.boot.spi.MappingFileDescriptor;
import org.hibernate.jpa.boot.spi.PackageDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

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
	public ScanResult scan(PersistenceUnitDescriptor persistenceUnit, ScanOptions scanOptions) {
		final ResultCollector resultCollector = new ResultCollector( scanOptions );

		if ( persistenceUnit.getJarFileUrls() != null ) {
			for ( URL url : persistenceUnit.getJarFileUrls() ) {
				final ArchiveDescriptor descriptor = buildArchiveDescriptor( url, false, scanOptions );
				final ArchiveContext context = buildArchiveContext( persistenceUnit, false, resultCollector );
				descriptor.visitArchive( context );
			}
		}

		if ( persistenceUnit.getPersistenceUnitRootUrl() != null ) {
			final ArchiveDescriptor descriptor = buildArchiveDescriptor( persistenceUnit.getPersistenceUnitRootUrl(), true, scanOptions );
			final ArchiveContext context = buildArchiveContext( persistenceUnit, true, resultCollector );
			descriptor.visitArchive( context );
		}

		return ScanResultImpl.from( resultCollector );
	}

	private ArchiveContext buildArchiveContext(
			PersistenceUnitDescriptor persistenceUnit,
			boolean isRoot,
			ArchiveEntryHandlers entryHandlers) {
		return new ArchiveContextImpl( persistenceUnit, isRoot, entryHandlers );
	}

	protected static interface ArchiveEntryHandlers {
		public ArchiveEntryHandler getClassFileHandler();
		public ArchiveEntryHandler getPackageInfoHandler();
		public ArchiveEntryHandler getFileHandler();
	}

	private ArchiveDescriptor buildArchiveDescriptor(URL url, boolean isRootUrl, ScanOptions scanOptions) {
		final ArchiveDescriptor descriptor;
		final ArchiveDescriptorInfo descriptorInfo = archiveDescriptorCache.get( url );
		if ( descriptorInfo == null ) {
			descriptor = archiveDescriptorFactory.buildArchiveDescriptor( url );
			archiveDescriptorCache.put(
					url,
					new ArchiveDescriptorInfo( descriptor, isRootUrl, scanOptions )
			);
		}
		else {
			validateReuse( descriptorInfo, isRootUrl, scanOptions );
			descriptor = descriptorInfo.archiveDescriptor;
		}
		return descriptor;
	}

	public static class ResultCollector
			implements ArchiveEntryHandlers,
					   PackageInfoArchiveEntryHandler.Callback,
					   ClassFileArchiveEntryHandler.Callback,
					   NonClassFileArchiveEntryHandler.Callback {
		private final ClassFileArchiveEntryHandler classFileHandler;
		private final PackageInfoArchiveEntryHandler packageInfoHandler;
		private final NonClassFileArchiveEntryHandler fileHandler;

		private final Set<PackageDescriptor> packageDescriptorSet = new HashSet<PackageDescriptor>();
		private final Set<ClassDescriptor> classDescriptorSet = new HashSet<ClassDescriptor>();
		private final Set<MappingFileDescriptor> mappingFileSet = new HashSet<MappingFileDescriptor>();

		public ResultCollector(ScanOptions scanOptions) {
			this.classFileHandler = new ClassFileArchiveEntryHandler( scanOptions, this );
			this.packageInfoHandler = new PackageInfoArchiveEntryHandler( scanOptions, this );
			this.fileHandler = new NonClassFileArchiveEntryHandler( scanOptions, this );
		}

		@Override
		public ArchiveEntryHandler getClassFileHandler() {
			return classFileHandler;
		}

		@Override
		public ArchiveEntryHandler getPackageInfoHandler() {
			return packageInfoHandler;
		}

		@Override
		public ArchiveEntryHandler getFileHandler() {
			return fileHandler;
		}

		@Override
		public void locatedPackage(PackageDescriptor packageDescriptor) {
			if ( PackageDescriptorImpl.class.isInstance( packageDescriptor ) ) {
				packageDescriptorSet.add( packageDescriptor );
			}
			else {
				// to make sure we have proper equals/hashcode
				packageDescriptorSet.add(
						new PackageDescriptorImpl(
								packageDescriptor.getName(),
								packageDescriptor.getStreamAccess()
						)
				);
			}
		}

		@Override
		public void locatedClass(ClassDescriptor classDescriptor) {
			if ( ClassDescriptorImpl.class.isInstance( classDescriptor ) ) {
				classDescriptorSet.add( classDescriptor );
			}
			else {
				// to make sure we have proper equals/hashcode
				classDescriptorSet.add(
						new ClassDescriptorImpl(
								classDescriptor.getName(),
								classDescriptor.getStreamAccess()
						)
				);
			}
		}

		@Override
		public void locatedMappingFile(MappingFileDescriptor mappingFileDescriptor) {
			if ( MappingFileDescriptorImpl.class.isInstance( mappingFileDescriptor ) ) {
				mappingFileSet.add( mappingFileDescriptor );
			}
			else {
				// to make sure we have proper equals/hashcode
				mappingFileSet.add(
						new MappingFileDescriptorImpl(
								mappingFileDescriptor.getName(),
								mappingFileDescriptor.getStreamAccess()
						)
				);
			}
		}

		public Set<PackageDescriptor> getPackageDescriptorSet() {
			return packageDescriptorSet;
		}

		public Set<ClassDescriptor> getClassDescriptorSet() {
			return classDescriptorSet;
		}

		public Set<MappingFileDescriptor> getMappingFileSet() {
			return mappingFileSet;
		}
	}

	// This needs to be protected and attributes/constructor visible in case
	// a custom scanner needs to override validateReuse.
	protected static class ArchiveDescriptorInfo {
		public final ArchiveDescriptor archiveDescriptor;
		public final boolean isRoot;
		public final ScanOptions scanOptions;

		public ArchiveDescriptorInfo(
				ArchiveDescriptor archiveDescriptor,
				boolean isRoot,
				ScanOptions scanOptions) {
			this.archiveDescriptor = archiveDescriptor;
			this.isRoot = isRoot;
			this.scanOptions = scanOptions;
		}
	}

	protected void validateReuse(ArchiveDescriptorInfo descriptor, boolean root, ScanOptions options) {
		// is it really reasonable that a single url be processed multiple times?
		// for now, throw an exception, mainly because I am interested in situations where this might happen
		throw new IllegalStateException( "ArchiveDescriptor reused; can URLs be processed multiple times?" );
	}

	public static class ArchiveContextImpl implements ArchiveContext {
		private final PersistenceUnitDescriptor persistenceUnitDescriptor;
		private final boolean isRootUrl;
		private final ArchiveEntryHandlers entryHandlers;

		public ArchiveContextImpl(
				PersistenceUnitDescriptor persistenceUnitDescriptor,
				boolean isRootUrl,
				ArchiveEntryHandlers entryHandlers) {
			this.persistenceUnitDescriptor = persistenceUnitDescriptor;
			this.isRootUrl = isRootUrl;
			this.entryHandlers = entryHandlers;
		}

		@Override
		public PersistenceUnitDescriptor getPersistenceUnitDescriptor() {
			return persistenceUnitDescriptor;
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

	private static class ScanResultImpl implements ScanResult {
		private final Set<PackageDescriptor> packageDescriptorSet;
		private final Set<ClassDescriptor> classDescriptorSet;
		private final Set<MappingFileDescriptor> mappingFileSet;

		private ScanResultImpl(
				Set<PackageDescriptor> packageDescriptorSet,
				Set<ClassDescriptor> classDescriptorSet,
				Set<MappingFileDescriptor> mappingFileSet) {
			this.packageDescriptorSet = packageDescriptorSet;
			this.classDescriptorSet = classDescriptorSet;
			this.mappingFileSet = mappingFileSet;
		}

		private static ScanResult from(ResultCollector resultCollector) {
			return new ScanResultImpl(
					Collections.unmodifiableSet( resultCollector.packageDescriptorSet ),
					Collections.unmodifiableSet( resultCollector.classDescriptorSet ),
					Collections.unmodifiableSet( resultCollector.mappingFileSet )
			);
		}

		@Override
		public Set<PackageDescriptor> getLocatedPackages() {
			return packageDescriptorSet;
		}

		@Override
		public Set<ClassDescriptor> getLocatedClasses() {
			return classDescriptorSet;
		}

		@Override
		public Set<MappingFileDescriptor> getLocatedMappingFiles() {
			return mappingFileSet;
		}
	}

}
