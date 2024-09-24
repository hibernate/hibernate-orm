/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.archive.scan.spi;

import java.io.IOException;
import java.io.InputStream;
import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.archive.scan.internal.ClassDescriptorImpl;
import org.hibernate.archive.scan.internal.ScanResultCollector;
import org.hibernate.boot.archive.scan.spi.ClassDescriptor;
import org.hibernate.boot.archive.spi.ArchiveContext;
import org.hibernate.boot.archive.spi.ArchiveEntry;
import org.hibernate.boot.archive.spi.ArchiveEntryHandler;
import org.hibernate.boot.archive.spi.ArchiveException;

import org.jboss.jandex.ClassSummary;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;

/**
 * Defines handling and filtering for class file entries within an archive
 *
 * @author Steve Ebersole
 */
public class ClassFileArchiveEntryHandler implements ArchiveEntryHandler {

	private final static DotName CONVERTER = DotName.createSimple( Converter.class.getName() );

	private final static DotName[] MODELS = {
			DotName.createSimple( Entity.class.getName() ),
			DotName.createSimple( MappedSuperclass.class.getName() ),
			DotName.createSimple( Embeddable.class.getName() )
	};

	private final ScanResultCollector resultCollector;
	private final Indexer indexer;

	public ClassFileArchiveEntryHandler(ScanResultCollector resultCollector) {
		this.resultCollector = resultCollector;
		this.indexer = new Indexer();
	}

	@Override
	public void handleEntry(ArchiveEntry entry, ArchiveContext context) {

		final ClassDescriptor classDescriptor = toClassDescriptor( entry );

		if ( classDescriptor.getCategorization() == ClassDescriptor.Categorization.OTHER ) {
			return;
		}

		resultCollector.handleClass( classDescriptor, context.isRootUrl() );
	}

	private ClassDescriptor toClassDescriptor(ArchiveEntry entry) {
		try (InputStream inputStream = entry.getStreamAccess().accessInputStream()) {
			ClassSummary classSummary = indexer.indexWithSummary( inputStream );
			Index index = indexer.complete();
			return toClassDescriptor( classSummary, index, entry );
		}
		catch (IOException e) {
			throw new ArchiveException( "Could not build ClassInfo", e );
		}
	}

	private ClassDescriptor toClassDescriptor(ClassSummary classSummary, Index index, ArchiveEntry entry) {
		ClassDescriptor.Categorization categorization = ClassDescriptor.Categorization.OTHER;

		if ( isModel( index ) ) {
			categorization = ClassDescriptor.Categorization.MODEL;
		}
		else if ( isConverter( index ) ) {
			categorization = ClassDescriptor.Categorization.CONVERTER;
		}

		return new ClassDescriptorImpl( classSummary.name().toString(), categorization, entry.getStreamAccess() );
	}

	private boolean isConverter(Index index) {
		return !index.getAnnotations( CONVERTER ).isEmpty();
	}

	private boolean isModel(Index index) {
		for ( DotName model : MODELS ) {
			if ( !index.getAnnotations( model ).isEmpty() ) {
				return true;
			}
		}
		return false;
	}
}
