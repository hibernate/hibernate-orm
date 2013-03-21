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

import javax.persistence.Converter;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;

import org.hibernate.jpa.boot.archive.spi.ArchiveContext;
import org.hibernate.jpa.boot.archive.spi.ArchiveEntry;
import org.hibernate.jpa.boot.archive.spi.ArchiveEntryHandler;
import org.hibernate.jpa.boot.archive.spi.ArchiveException;
import org.hibernate.jpa.boot.internal.ClassDescriptorImpl;
import org.hibernate.jpa.boot.scan.spi.ScanOptions;
import org.hibernate.jpa.boot.spi.ClassDescriptor;

/**
* @author Steve Ebersole
*/
public class ClassFileArchiveEntryHandler implements ArchiveEntryHandler {
	private final ScanOptions scanOptions;
	private final Callback callback;

	public static interface Callback {
		public void locatedClass(ClassDescriptor classDescriptor);
	}

	public ClassFileArchiveEntryHandler(ScanOptions scanOptions, Callback callback) {
		this.scanOptions = scanOptions;
		this.callback = callback;
	}

	@Override
	public void handleEntry(ArchiveEntry entry, ArchiveContext context) {
		final ClassFile classFile = toClassFile( entry );
		final ClassDescriptor classDescriptor = toClassDescriptor( classFile, entry );

		if ( ! context.getPersistenceUnitDescriptor().getManagedClassNames().contains( classDescriptor.getName() ) ) {
			if ( context.isRootUrl() ) {
				if ( ! scanOptions.canDetectUnlistedClassesInRoot() ) {
					return;
				}
			}
			else {
				if ( ! scanOptions.canDetectUnlistedClassesInNonRoot() ) {
					return;
				}
			}
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
