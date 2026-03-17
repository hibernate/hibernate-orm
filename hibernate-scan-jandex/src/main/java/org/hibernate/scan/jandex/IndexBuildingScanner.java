/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.scan.jandex;

import org.hibernate.HibernateException;
import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveEntry;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl.JaxbPersistenceUnitImpl;
import org.hibernate.boot.models.internal.OrmAnnotationHelper;
import org.hibernate.boot.scan.internal.ResultCollector;
import org.hibernate.boot.scan.spi.Scanner;
import org.hibernate.boot.scan.spi.ScanningContext;
import org.hibernate.boot.scan.spi.ScanningResult;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.jboss.jandex.Indexer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author Steve Ebersole
 */
public class IndexBuildingScanner implements Scanner {
	private final ScanningContext scanningContext;

	public IndexBuildingScanner(ScanningContext scanningContext) {
		this.scanningContext = scanningContext;
	}

	@Override
	public ScanningResult scan(URL... boundaries) {
		var resultCollector = new ResultCollector();
		final var indexer = new Indexer();
		OrmAnnotationHelper.forEachOrmAnnotation( (descriptor) -> {
			indexClass( descriptor.getAnnotationType(), indexer );
		} );
		for ( URL boundary : boundaries ) {
			var archive = scanningContext.getArchiveDescriptorFactory().buildArchiveDescriptor(boundary);
			archive.visitClassEntries( (entry) -> indexClassEntry( entry, indexer ) );
		}
		var indexToUse = indexer.complete();
		IndexScanner.scanForClasses( indexToUse, resultCollector );
		return resultCollector.toResult();
	}

	@Override
	public ScanningResult jpaScan(ArchiveDescriptor rootArchive, JaxbPersistenceUnitImpl jaxbUnit) {
		var resultCollector = new ResultCollector();
		var indexer = new Indexer();
		OrmAnnotationHelper.forEachOrmAnnotation( (descriptor) -> {
			indexClass( descriptor.getAnnotationType(), indexer );
		} );

		if ( jaxbUnit.isExcludeUnlistedClasses() != Boolean.TRUE ) {
			rootArchive.visitClassEntries(  (entry) -> indexClassEntry( entry, indexer ) );
		}
		if ( CollectionHelper.isNotEmpty( jaxbUnit.getJarFiles() ) ) {
			jaxbUnit.getJarFiles().forEach( jarFileRef -> {
				final var jarFileArchive = rootArchive.resolveJarFileReference( jarFileRef );
				jarFileArchive.visitClassEntries(  (entry) -> indexClassEntry( entry, indexer ) );
			} );
		}
		var indexToUse = indexer.complete();

		IndexScanner.scanForClasses( indexToUse, resultCollector );
		return resultCollector.toResult();
	}


	private void indexClass(Class<?> clazz, Indexer indexer) {
		try {
			indexer.indexClass( clazz );
		}
		catch (IOException e) {
			throw new HibernateException( "Error indexing class for Jandex index - " + clazz.getName(), e );
		}
	}

	private void indexClassEntry(ArchiveEntry entry, Indexer indexer) {
		try (final InputStream stream = entry.getStreamAccess().accessInputStream()) {
			indexer.index( stream );
		}
		catch (IOException e) {
			throw new HibernateException( "Error accessing archive entry stream for indexing", e );
		}
	}
}
