/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.scan.jandex;

import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedStatement;
import jakarta.persistence.NamedStatements;
import jakarta.persistence.NamedStoredProcedureQueries;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.jpa.boot.discovery.internal.StandardDiscovery;
import org.hibernate.jpa.boot.discovery.archive.spi.ArchiveDescriptor;
import org.hibernate.jpa.boot.discovery.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.jpa.boot.discovery.archive.spi.ArchiveEntry;
import org.hibernate.jpa.boot.discovery.spi.Boundaries;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.function.Consumer;

/**
 * @author Steve Ebersole
 */
public class DiscoveryImpl extends StandardDiscovery {
	private final IndexView existingJandexIndex;

	public DiscoveryImpl(
			ArchiveDescriptorFactory archiveDescriptorFactory,
			ClassLoaderService classLoaderService,
			IndexView existingJandexIndex) {
		super( archiveDescriptorFactory, classLoaderService );
		this.existingJandexIndex = existingJandexIndex;
	}

	@Override
	public void discoverClassNames(Boundaries boundaries, Consumer<String> classNameConsumer) {
		final IndexView jandexIndexToUse = existingJandexIndex != null
				? existingJandexIndex
				: buildJandexIndex( boundaries, classNameConsumer );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// todo (jpa4) : After https://github.com/jakartaee/persistence/pull/940 is available, rework this to use `@Discoverable`.
		//		for now...
		jandexIndexToUse.getAnnotations( Entity.class ).forEach( annotationUsage -> {
			classNameConsumer.accept( annotationUsage.target().asClass().name().toString() );
		} );
		jandexIndexToUse.getAnnotations( MappedSuperclass.class ).forEach( annotationUsage -> {
			classNameConsumer.accept( annotationUsage.target().asClass().name().toString() );
		} );
		jandexIndexToUse.getAnnotations( Embeddable.class ).forEach( annotationUsage -> {
			classNameConsumer.accept( annotationUsage.target().asClass().name().toString() );
		} );

		jandexIndexToUse.getAnnotations( Converter.class ).forEach( annotationUsage -> {
			classNameConsumer.accept( annotationUsage.target().asClass().name().toString() );
		} );

		jandexIndexToUse.getAnnotations( NamedQueries.class ).forEach( annotationUsage -> {
			classNameConsumer.accept( annotationUsage.target().asClass().name().toString() );
		} );
		jandexIndexToUse.getAnnotations( NamedQuery.class ).forEach( annotationUsage -> {
			classNameConsumer.accept( annotationUsage.target().asClass().name().toString() );
		} );
		jandexIndexToUse.getAnnotations( NamedStatements.class ).forEach( annotationUsage -> {
			classNameConsumer.accept( annotationUsage.target().asClass().name().toString() );
		} );
		jandexIndexToUse.getAnnotations( NamedStatement.class ).forEach( annotationUsage -> {
			classNameConsumer.accept( annotationUsage.target().asClass().name().toString() );
		} );
		jandexIndexToUse.getAnnotations( NamedStoredProcedureQueries.class ).forEach( annotationUsage -> {
			classNameConsumer.accept( annotationUsage.target().asClass().name().toString() );
		} );
		jandexIndexToUse.getAnnotations( NamedStoredProcedureQuery.class ).forEach( annotationUsage -> {
			classNameConsumer.accept( annotationUsage.target().asClass().name().toString() );
		} );

		jandexIndexToUse.getAnnotations( SqlResultSetMappings.class ).forEach( annotationUsage -> {
			classNameConsumer.accept( annotationUsage.target().asClass().name().toString() );
		} );
		jandexIndexToUse.getAnnotations( SqlResultSetMapping.class ).forEach( annotationUsage -> {
			classNameConsumer.accept( annotationUsage.target().asClass().name().toString() );
		} );
	}

	private IndexView buildJandexIndex(Boundaries boundaries, Consumer<String> classNameConsumer) {
		final var indexer = new Indexer();

		processMappingFiles( boundaries.getMappingFiles(), (className) -> {
			classNameConsumer.accept( className );
			try (final InputStream classFileStream = locateClassFile(className).openStream()) {
				indexer.index( classFileStream );
			}
			catch (IOException e) {
				throw new HibernateException( "Unable to open class file stream - " + className, e );
			}
		} );

		if ( CollectionHelper.isNotEmpty( boundaries.getUrls() ) ) {
			boundaries.getUrls().forEach( (url) -> {
				final ArchiveDescriptor nonRootArchive = archiveDescriptorFactory.buildArchiveDescriptor( url );
				nonRootArchive.visitClassEntries( (entry) -> processClassEntry( entry, indexer ) );
			} );
		}

		return indexer.complete();
	}

	private void processClassEntry(ArchiveEntry entry, Indexer indexer) {
		try (final InputStream stream = entry.getStreamAccess().accessInputStream()) {
			indexer.index( stream );
		}
		catch (IOException e) {
			throw new HibernateException( "Error accessing archive entry stream for indexing", e );
		}
	}

	private URL locateClassFile(String className) {
		var fileName = className.replace( '.', '/' ) + ".class";
		var url = classLoaderService.locateResource( fileName );
		if ( url == null ) {
			throw new HibernateException( "Unable to locate class file - " + className );
		}
		return url;
	}
}
