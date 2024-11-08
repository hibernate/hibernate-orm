/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.internal;

import java.util.function.Supplier;

import org.hibernate.annotations.TenantId;
import org.hibernate.models.internal.MutableClassDetailsRegistry;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.RegistryPrimer;
import org.hibernate.models.spi.SourceModelBuildingContext;


/**
 * @author Steve Ebersole
 */
public class ModelsHelper {
	public static void preFillRegistries(RegistryPrimer.Contributions contributions, SourceModelBuildingContext buildingContext) {
		OrmAnnotationHelper.forEachOrmAnnotation( contributions::registerAnnotation );

		buildingContext.getAnnotationDescriptorRegistry().getDescriptor( TenantId.class );

//		if ( buildingContext instanceof JandexModelBuildingContext ) {
//			final IndexView jandexIndex = buildingContext.as( JandexModelBuildingContext.class ).getJandexIndex();
//			if ( jandexIndex == null ) {
//				return;
//			}
//
//			final ClassDetailsRegistry classDetailsRegistry = buildingContext.getClassDetailsRegistry();
//			final AnnotationDescriptorRegistry annotationDescriptorRegistry = buildingContext.getAnnotationDescriptorRegistry();
//
//			for ( ClassInfo knownClass : jandexIndex.getKnownClasses() ) {
//				final String className = knownClass.name().toString();
//
//				if ( knownClass.isAnnotation() ) {
//					// it is always safe to load the annotation classes - we will never be enhancing them
//					//noinspection rawtypes
//					final Class annotationClass = buildingContext
//							.getClassLoading()
//							.classForName( className );
//					//noinspection unchecked
//					annotationDescriptorRegistry.resolveDescriptor(
//							annotationClass,
//							(t) -> JdkBuilders.buildAnnotationDescriptor( annotationClass, buildingContext )
//					);
//				}
//
//				resolveClassDetails(
//						className,
//						classDetailsRegistry,
//						() -> new JandexClassDetails( knownClass, buildingContext )
//				);
//			}
//		}
	}

	public static ClassDetails resolveClassDetails(
			String className,
			ClassDetailsRegistry classDetailsRegistry,
			Supplier<ClassDetails> classDetailsSupplier) {
		ClassDetails classDetails = classDetailsRegistry.findClassDetails( className );
		if ( classDetails != null ) {
			return classDetails;
		}
		classDetails = classDetailsSupplier.get();
		classDetailsRegistry.as( MutableClassDetailsRegistry.class )
				.addClassDetails( className, classDetails );
		return classDetails;
	}
}
