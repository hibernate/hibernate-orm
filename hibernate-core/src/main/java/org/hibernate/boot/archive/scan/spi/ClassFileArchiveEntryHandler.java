/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.scan.spi;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.persistence.Converter;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;

import org.hibernate.boot.archive.scan.internal.ClassDescriptorImpl;
import org.hibernate.boot.archive.scan.internal.ScanResultCollector;
import org.hibernate.boot.archive.spi.ArchiveContext;
import org.hibernate.boot.archive.spi.ArchiveEntry;
import org.hibernate.boot.archive.spi.ArchiveEntryHandler;
import org.hibernate.boot.archive.spi.ArchiveException;

/**
 * Defines handling and filtering for class file entries within an archive
 *
 * @author Steve Ebersole
 */
public class ClassFileArchiveEntryHandler implements ArchiveEntryHandler {
	private final ScanResultCollector resultCollector;

	public ClassFileArchiveEntryHandler(ScanResultCollector resultCollector) {
		this.resultCollector = resultCollector;
	}

	@Override
	public void handleEntry(ArchiveEntry entry, ArchiveContext context) {
		// Ultimately we'd like to leverage Jandex here as long term we want to move to
		// using Jandex for annotation processing.  But even then, Jandex atm does not have
		// any facility for passing a stream and conditionally indexing it into an Index or
		// returning existing ClassInfo objects.
		//
		// So not sure we can ever not do this unconditional input stream read :(
		final ClassFile classFile = toClassFile( entry );
		final ClassDescriptor classDescriptor = toClassDescriptor( classFile, entry );

		if ( classDescriptor.getCategorization() == ClassDescriptor.Categorization.OTHER ) {
			return;
		}

		resultCollector.handleClass( classDescriptor, context.isRootUrl() );
	}

	private ClassFile toClassFile(ArchiveEntry entry) {
		final InputStream inputStream = entry.getStreamAccess().accessInputStream();
		final DataInputStream dataInputStream = new DataInputStream( inputStream );
		try {
			return new ClassFile( dataInputStream );
		}
		catch (IOException e) {
			throw new ArchiveException( "Could not build ClassFile" );
		}
		finally {
			try {
				dataInputStream.close();
			}
			catch (Exception ignore) {
			}

			try {
				inputStream.close();
			}
			catch (IOException ignore) {
			}
		}
	}

	private ClassDescriptor toClassDescriptor(ClassFile classFile, ArchiveEntry entry) {
		ClassDescriptor.Categorization categorization = ClassDescriptor.Categorization.OTHER;;

		final AnnotationsAttribute visibleAnnotations = (AnnotationsAttribute) classFile.getAttribute( AnnotationsAttribute.visibleTag );
		if ( visibleAnnotations != null ) {
			if ( visibleAnnotations.getAnnotation( Entity.class.getName() ) != null
					|| visibleAnnotations.getAnnotation( MappedSuperclass.class.getName() ) != null
					|| visibleAnnotations.getAnnotation( Embeddable.class.getName() ) != null ) {
				categorization = ClassDescriptor.Categorization.MODEL;
			}
			else if ( visibleAnnotations.getAnnotation( Converter.class.getName() ) != null ) {
				categorization = ClassDescriptor.Categorization.CONVERTER;
			}
		}

		return new ClassDescriptorImpl( classFile.getName(), categorization, entry.getStreamAccess() );
	}
}
