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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.persistence.Converter;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;

import org.hibernate.jpa.boot.archive.spi.ArchiveContext;
import org.hibernate.jpa.boot.archive.spi.ArchiveEntry;
import org.hibernate.jpa.boot.archive.spi.ArchiveException;
import org.hibernate.jpa.boot.internal.ClassDescriptorImpl;
import org.hibernate.jpa.boot.spi.ClassDescriptor;

/**
 * Defines handling and filtering for class file entries within an archive
 *
 * @author Steve Ebersole
 */
public class ClassFileArchiveEntryHandler extends AbstractJavaArtifactArchiveEntryHandler {
	private final Callback callback;

	/**
	 * Contract for the thing interested in being notified about accepted class descriptors.
	 */
	public static interface Callback {
		public void locatedClass(ClassDescriptor classDescriptor);
	}

	public ClassFileArchiveEntryHandler(ScanOptions scanOptions, Callback callback) {
		super( scanOptions );
		this.callback = callback;
	}

	@Override
	public void handleEntry(ArchiveEntry entry, ArchiveContext context) {
		final ClassFile classFile = toClassFile( entry );
		final ClassDescriptor classDescriptor = toClassDescriptor( classFile, entry );

		if ( ! isListedOrDetectable( context, classDescriptor.getName() ) ) {
			return;
		}

		// we are only interested in classes with certain annotations, so see if the ClassDescriptor
		// represents a class which contains any of those annotations
		if ( ! containsClassAnnotationsOfInterest( classFile ) ) {
			return;
		}

		notifyMatchedClass( classDescriptor );
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

	@SuppressWarnings("SimplifiableIfStatement")
	private boolean containsClassAnnotationsOfInterest(ClassFile cf) {
		final AnnotationsAttribute visibleAnnotations = (AnnotationsAttribute) cf.getAttribute( AnnotationsAttribute.visibleTag );
		if ( visibleAnnotations == null ) {
			return false;
		}

		return visibleAnnotations.getAnnotation( Entity.class.getName() ) != null
				|| visibleAnnotations.getAnnotation( MappedSuperclass.class.getName() ) != null
				|| visibleAnnotations.getAnnotation( Embeddable.class.getName() ) != null
				|| visibleAnnotations.getAnnotation( Converter.class.getName() ) != null;
	}

	protected ClassDescriptor toClassDescriptor(ClassFile classFile, ArchiveEntry entry) {
		return new ClassDescriptorImpl( classFile.getName(), entry.getStreamAccess() );
	}

	protected final void notifyMatchedClass(ClassDescriptor classDescriptor) {
		callback.locatedClass( classDescriptor );
	}
}
