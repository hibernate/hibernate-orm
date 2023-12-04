/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models;

import java.io.IOException;
import java.lang.annotation.Annotation;

import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.categorize.internal.OrmAnnotationHelper;
import org.hibernate.models.internal.AnnotationDescriptorRegistryStandard;
import org.hibernate.models.internal.BaseLineJavaTypes;
import org.hibernate.models.internal.SourceModelBuildingContextImpl;
import org.hibernate.models.internal.jandex.JandexBuilders;
import org.hibernate.models.internal.jandex.JandexIndexerHelper;
import org.hibernate.models.internal.jdk.JdkBuilders;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.ClassLoading;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;

import static org.hibernate.models.internal.SimpleClassLoading.SIMPLE_CLASS_LOADING;

/**
 * @author Steve Ebersole
 */
public class SourceModelTestHelper {

	public static SourceModelBuildingContext createBuildingContext(Class<?>... modelClasses) {
		return createBuildingContext( SIMPLE_CLASS_LOADING, modelClasses );
	}

	public static SourceModelBuildingContext createBuildingContext(ClassLoading classLoadingAccess, Class<?>... modelClasses) {
		final Index jandexIndex = buildJandexIndex( classLoadingAccess, modelClasses );
		return createBuildingContext( jandexIndex, modelClasses );
	}

	public static SourceModelBuildingContextImpl createBuildingContext(Index jandexIndex, Class<?>... modelClasses) {
		return createBuildingContext( jandexIndex, SIMPLE_CLASS_LOADING, modelClasses );
	}

	public static SourceModelBuildingContextImpl createBuildingContext(
			Index jandexIndex,
			ClassLoading classLoadingAccess,
			Class<?>... modelClasses) {
		final SourceModelBuildingContextImpl buildingContext = new SourceModelBuildingContextImpl(
				classLoadingAccess,
				jandexIndex,
				(contributions, buildingContext1) -> {
					OrmAnnotationHelper.forEachOrmAnnotation( contributions::registerAnnotation );
				}
		);
		final ClassDetailsRegistry classDetailsRegistry = buildingContext.getClassDetailsRegistry();
		final AnnotationDescriptorRegistryStandard annotationDescriptorRegistry = (AnnotationDescriptorRegistryStandard) buildingContext.getAnnotationDescriptorRegistry();

		for ( ClassInfo knownClass : jandexIndex.getKnownClasses() ) {
//			if ( knownClass.simpleName().endsWith( "package-info" ) ) {
//				new PackageDetailsImpl( knownClass, buildingContext );
//				continue;
//			}

			classDetailsRegistry.resolveClassDetails(
					knownClass.name().toString(),
					JandexBuilders.DEFAULT_BUILDER
			);

			if ( knownClass.isAnnotation() ) {
				final Class<? extends Annotation> annotationClass = buildingContext
						.getClassLoading()
						.classForName( knownClass.name().toString() );
				annotationDescriptorRegistry.resolveDescriptor( annotationClass, annotationType -> JdkBuilders.buildAnnotationDescriptor(
						annotationType,
						annotationDescriptorRegistry
				) );
			}
		}

		for ( int i = 0; i < modelClasses.length; i++ ) {
			classDetailsRegistry.resolveClassDetails( modelClasses[i].getName() );
		}

		return buildingContext;
	}

	public static Index buildJandexIndex(Class<?>... modelClasses) {
		return buildJandexIndex( SIMPLE_CLASS_LOADING, modelClasses );
	}

	public static Index buildJandexIndex(ClassLoading classLoadingAccess, Class<?>... modelClasses) {
		final Indexer indexer = new Indexer();
		BaseLineJavaTypes.forEachJavaType( (javaType) -> JandexIndexerHelper.apply( javaType, indexer, classLoadingAccess ) );
		JpaAnnotations.forEachAnnotation( (descriptor) -> JandexIndexerHelper.apply( descriptor.getAnnotationType(), indexer, classLoadingAccess ) );
		HibernateAnnotations.forEachAnnotation( (descriptor) -> JandexIndexerHelper.apply( descriptor.getAnnotationType(), indexer, classLoadingAccess ) );

		for ( Class<?> modelClass : modelClasses ) {
			try {
				indexer.indexClass( modelClass );
			}
			catch (IOException e) {
				throw new RuntimeException( e );
			}
		}

		return indexer.complete();
	}
}
