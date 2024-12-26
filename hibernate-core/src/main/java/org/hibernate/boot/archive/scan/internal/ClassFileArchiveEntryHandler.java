/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.scan.internal;

import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.boot.archive.scan.spi.ClassDescriptor;
import org.hibernate.boot.archive.spi.ArchiveContext;
import org.hibernate.boot.archive.spi.ArchiveEntry;
import org.hibernate.boot.archive.spi.ArchiveEntryHandler;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Steve Ebersole
 */
public class ClassFileArchiveEntryHandler implements ArchiveEntryHandler {
	private final ScanResultCollector resultCollector;
	private final ClassLoaderService classLoaderService;

	public ClassFileArchiveEntryHandler(ScanResultCollector resultCollector, ServiceRegistry serviceRegistry) {
		this.resultCollector = resultCollector;
		this.classLoaderService = serviceRegistry.requireService( ClassLoaderService.class );

	}

	@Override
	public void handleEntry(ArchiveEntry entry, ArchiveContext context) {
		final String nameWithinArchive = entry.getNameWithinArchive();
		// strip the ending ".class"
		final String classNameWithPaths = nameWithinArchive.substring( 0, nameWithinArchive.length() - 6 );
		final String classNameBase = classNameWithPaths.charAt( 0 ) == '/'
				? classNameWithPaths.substring( 1 )
				: classNameWithPaths;
		final String className = classNameBase.replace( '/', '.' );

		if ( !resultCollector.isListedOrDetectable( className, context.isRootUrl() ) ) {
			return;
		}

		ClassDescriptor.Categorization categorization = ClassDescriptor.Categorization.OTHER;

		try {
			final Class<?> classForName = classLoaderService.classForName( className );
			if ( classForName.isAnnotationPresent( Converter.class ) ) {
				categorization = ClassDescriptor.Categorization.CONVERTER;
			}
			else if ( isModel( classForName ) ) {
				categorization = ClassDescriptor.Categorization.MODEL;
			}
		}
		catch (ClassLoadingException ignore) {

		}

		if ( categorization == ClassDescriptor.Categorization.OTHER ) {
			return;
		}

		final ClassDescriptor classDescriptor = new ClassDescriptorImpl( className, categorization, entry.getStreamAccess() );
		resultCollector.handleClass( classDescriptor, context.isRootUrl() );
	}

	private boolean isModel(Class<?> classForName) {
		return classForName.isAnnotationPresent( Entity.class )
				|| classForName.isAnnotationPresent( MappedSuperclass.class )
				|| classForName.isAnnotationPresent( Embeddable.class );

//		final MutableBoolean hasMatch = new MutableBoolean();
//		OrmAnnotationHelper.forEachOrmAnnotation( (annotationDescriptor) -> {
//			if ( hasMatch.getValue() ) {
//				return;
//			}
//			if ( classForName.isAnnotationPresent( annotationDescriptor.getAnnotationType() ) ) {
//				hasMatch.setValue( true );
//			}
//		} );
//		return hasMatch.getValue();
	}
}
