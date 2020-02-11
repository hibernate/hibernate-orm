/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.scan.spi;

import java.io.IOException;
import java.io.InputStream;
import javax.persistence.Converter;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.hibernate.boot.archive.scan.internal.ClassDescriptorImpl;
import org.hibernate.boot.archive.scan.internal.ScanResultCollector;
import org.hibernate.boot.archive.spi.ArchiveContext;
import org.hibernate.boot.archive.spi.ArchiveEntry;
import org.hibernate.boot.archive.spi.ArchiveEntryHandler;
import org.hibernate.boot.archive.spi.ArchiveException;

import org.jboss.jandex.ClassInfo;
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

	public ClassFileArchiveEntryHandler(ScanResultCollector resultCollector) {
		this.resultCollector = resultCollector;
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
			Indexer indexer = new Indexer();
			ClassInfo classInfo = indexer.index( inputStream );
			Index index = indexer.complete();
			return toClassDescriptor( classInfo, index, entry );
		}
		catch (IOException e) {
			throw new ArchiveException( "Could not build ClassInfo", e );
		}
	}

	private ClassDescriptor toClassDescriptor(ClassInfo classInfo, Index index, ArchiveEntry entry) {
		ClassDescriptor.Categorization categorization = ClassDescriptor.Categorization.OTHER;

		if ( isModel( index ) ) {
			categorization = ClassDescriptor.Categorization.MODEL;
		}
		else if ( isConverter( index ) ) {
			categorization = ClassDescriptor.Categorization.CONVERTER;
		}

		return new ClassDescriptorImpl( classInfo.name().toString(), categorization, entry.getStreamAccess() );
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
