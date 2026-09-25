package org.hibernate.scan.jandex;

import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.IndexView;

import org.hibernate.HibernateException;
import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveEntry;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl.JaxbPersistenceUnitImpl;
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
	private final IndexView annotationMetadata;

	public IndexBuildingScanner(ScanningContext scanningContext) {
		this( scanningContext, null );
	}

	IndexBuildingScanner(ScanningContext scanningContext, IndexView annotationMetadata) {
		this.scanningContext = scanningContext;
		this.annotationMetadata = annotationMetadata;
	}

	@Override
	public ScanningResult scan(URL... boundaries) {
		var resultCollector = new ResultCollector();
		var indexer = new Indexer();
		for ( URL boundary : boundaries ) {
			var archive = scanningContext.getArchiveDescriptorFactory().buildArchiveDescriptor(boundary);
			archive.visitClassEntries( (entry) -> indexClassEntry( entry, indexer ) );
			collectMapping( archive, resultCollector );
		}
		var indexToUse = indexer.complete();
		scanIndex( indexToUse, resultCollector );
		return resultCollector.toResult();
	}

	@Override
	public ScanningResult jpaScan(ArchiveDescriptor rootArchive, JaxbPersistenceUnitImpl jaxbUnit) {
		var resultCollector = new ResultCollector();
		var indexer = new Indexer();

		collectMapping( rootArchive, resultCollector );
		if ( Boolean.FALSE.equals( jaxbUnit.isExcludeUnlistedClasses() ) ) {
			rootArchive.visitClassEntries(  (entry) -> indexClassEntry( entry, indexer ) );
		}
		if ( CollectionHelper.isNotEmpty( jaxbUnit.getJarFiles() ) ) {
			jaxbUnit.getJarFiles().forEach( jarFileRef -> {
				final var jarFileArchive = rootArchive.resolveJarFileReference( jarFileRef );
				jarFileArchive.visitClassEntries(  (entry) -> indexClassEntry( entry, indexer ) );
				collectMapping( jarFileArchive, resultCollector );
			} );
		}
		var indexToUse = indexer.complete();

		scanIndex( indexToUse, resultCollector );
		return resultCollector.toResult();
	}

	private void scanIndex(IndexView candidates, ResultCollector collector) {
		final var baseline = IndexerSupport.buildBaselineIndexer().complete();
		final var lookup = annotationMetadata == null
				? CompositeIndex.create( candidates, baseline )
				: CompositeIndex.create( candidates, baseline, annotationMetadata );
		IndexScanner.scanForResources( candidates, lookup, collector );
	}

	private static void collectMapping(ArchiveDescriptor archive, ResultCollector collector) {
		final var mapping = archive.findEntry( "META-INF/orm.xml" );
		if ( mapping != null ) {
			collector.addMapping( mapping.getUri() );
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
