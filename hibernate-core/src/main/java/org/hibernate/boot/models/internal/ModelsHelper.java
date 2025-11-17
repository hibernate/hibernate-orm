/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.internal;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.util.function.Supplier;

import org.hibernate.annotations.TenantId;
import org.hibernate.models.internal.MutableClassDetailsRegistry;
import org.hibernate.models.internal.jdk.JdkClassDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.RegistryPrimer;
import org.hibernate.models.spi.ModelsContext;


/**
 * @author Steve Ebersole
 */
public class ModelsHelper {
	public static void preFillRegistries(RegistryPrimer.Contributions contributions, ModelsContext buildingContext) {
		OrmAnnotationHelper.forEachOrmAnnotation( contributions::registerAnnotation );

		registerPrimitive( boolean.class, buildingContext );
		registerPrimitive( byte.class, buildingContext );
		registerPrimitive( short.class, buildingContext );
		registerPrimitive( int.class, buildingContext );
		registerPrimitive( long.class, buildingContext );
		registerPrimitive( double.class, buildingContext );
		registerPrimitive( float.class, buildingContext );
		registerPrimitive( char.class, buildingContext );
		registerPrimitive( Blob.class, buildingContext );
		registerPrimitive( Clob.class, buildingContext );
		registerPrimitive( NClob.class, buildingContext );

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

	private static void registerPrimitive(Class<?> theClass, ModelsContext buildingContext) {
		final MutableClassDetailsRegistry classDetailsRegistry = buildingContext.getClassDetailsRegistry().as( MutableClassDetailsRegistry.class );
		classDetailsRegistry.addClassDetails( new JdkClassDetails( theClass, buildingContext ) );

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
