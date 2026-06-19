/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models;

import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.internal.RootMappingDefaults;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.process.spi.MetadataBuildingProcess;
import org.hibernate.boot.models.internal.ClassLoaderServiceLoading;
import org.hibernate.boot.models.internal.DomainModelCategorizationCollector;
import org.hibernate.boot.models.internal.GlobalRegistrationsImpl;
import org.hibernate.boot.models.internal.ModelsHelper;
import org.hibernate.boot.models.mapping.internal.xml.PersistenceUnitMetadataImpl;
import org.hibernate.boot.models.mapping.internal.xml.XmlPreProcessingResult;
import org.hibernate.boot.models.mapping.internal.xml.XmlPreProcessor;
import org.hibernate.boot.models.mapping.internal.xml.XmlProcessingResult;
import org.hibernate.boot.models.mapping.internal.xml.XmlProcessor;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.models.internal.BasicModelsContextImpl;
import org.hibernate.models.jandex.internal.JandexIndexerHelper;
import org.hibernate.models.jandex.internal.JandexModelsContextImpl;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.ClassLoading;
import org.hibernate.models.spi.ModelsContext;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.List;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.forEachOrmAnnotation;
import static org.hibernate.internal.util.collections.CollectionHelper.mutableJoin;
import static org.hibernate.models.internal.BaseLineJavaTypes.forEachJavaType;
import static org.hibernate.models.internal.SimpleClassLoading.SIMPLE_CLASS_LOADING;

/**
 * @author Steve Ebersole
 */
public class SourceModelTestHelper {

	public static ModelsContext createBuildingContext(Class<?>... modelClasses) {
		return createBuildingContext( SIMPLE_CLASS_LOADING, modelClasses );
	}

	public static ModelsContext createBuildingContext(ClassLoading classLoadingAccess, Class<?>... modelClasses) {
		final Index jandexIndex = buildJandexIndex( classLoadingAccess, modelClasses );
		return createBuildingContext( jandexIndex, modelClasses );
	}

	public static ModelsContext createBuildingContext(Index jandexIndex, Class<?>... modelClasses) {
		return createBuildingContext( jandexIndex, SIMPLE_CLASS_LOADING, modelClasses );
	}

	public static ModelsContext createBuildingContext(
			Index jandexIndex,
			ClassLoading classLoadingAccess,
			Class<?>... modelClasses) {
		final ModelsContext ctx;

		if ( jandexIndex == null ) {
			ctx = new BasicModelsContextImpl(
					classLoadingAccess,
					false,
					(contributions, buildingContext1) -> forEachOrmAnnotation( contributions::registerAnnotation )
			);
		}
		else {
			ctx = new JandexModelsContextImpl(
					jandexIndex,
					false,
					classLoadingAccess,
					(contributions, buildingContext1) -> forEachOrmAnnotation( contributions::registerAnnotation )
			);

			for ( ClassInfo knownClass : jandexIndex.getKnownClasses() ) {
				ctx.getClassDetailsRegistry().resolveClassDetails( knownClass.name().toString() );

				if ( knownClass.isAnnotation() ) {
					final Class<? extends Annotation> annotationClass = classLoadingAccess.classForName( knownClass.name().toString() );
					ctx.getAnnotationDescriptorRegistry().getDescriptor( annotationClass );
				}
			}
		}

		for ( Class<?> modelClass : modelClasses ) {
			ctx.getClassDetailsRegistry().resolveClassDetails( modelClass.getName() );
		}

		return ctx;
	}

	public static Index buildJandexIndex(Class<?>... modelClasses) {
		return buildJandexIndex( SIMPLE_CLASS_LOADING, modelClasses );
	}

	public static Index buildJandexIndex(ClassLoading classLoadingAccess, Class<?>... modelClasses) {
		final Indexer indexer = new Indexer();
		forEachJavaType( (javaType) -> JandexIndexerHelper.apply( javaType, indexer, classLoadingAccess ) );
		forEachOrmAnnotation( (descriptor) -> JandexIndexerHelper.apply( descriptor.getAnnotationType(), indexer, classLoadingAccess ) );

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

	public static ModelsContext createBuildingContext(
			ManagedResources managedResources,
			StandardServiceRegistry serviceRegistry) {
		final MetadataBuilderImpl.MetadataBuildingOptionsImpl metadataBuildingOptions =
				new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
		final BootstrapContextImpl bootstrapContext =
				new BootstrapContextImpl( serviceRegistry, metadataBuildingOptions );
		metadataBuildingOptions.setBootstrapContext( bootstrapContext );
		return createBuildingContext(
				managedResources,
				false,
				metadataBuildingOptions,
				bootstrapContext
		);
	}

	public static ModelsContext createBuildingContext(
			ManagedResources managedResources,
			Index jandexIndex,
			StandardServiceRegistry serviceRegistry) {
		final MetadataBuilderImpl.MetadataBuildingOptionsImpl metadataBuildingOptions =
				new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
		final BootstrapContextTesting bootstrapContext =
				new BootstrapContextTesting( jandexIndex, serviceRegistry, metadataBuildingOptions );
		metadataBuildingOptions.setBootstrapContext( bootstrapContext );
		return createBuildingContext(
				managedResources,
				false,
				metadataBuildingOptions,
				bootstrapContext
		);
	}

	public static ModelsContext createBuildingContext(
			ManagedResources managedResources,
			boolean buildJandexIndex,
			MetadataBuildingOptions metadataBuildingOptions,
			BootstrapContext bootstrapContext) {
		MetadataBuildingProcess.complete(
				managedResources,
				bootstrapContext,
				metadataBuildingOptions
		);

		final ClassLoaderService classLoaderService =
				bootstrapContext.getServiceRegistry().getService( ClassLoaderService.class );
		final ClassLoaderServiceLoading classLoading = new ClassLoaderServiceLoading( classLoaderService );

		final PersistenceUnitMetadataImpl persistenceUnitMetadata = new PersistenceUnitMetadataImpl();

		final XmlPreProcessingResult xmlPreProcessingResult =
				XmlPreProcessor.preProcessXmlResources( managedResources, persistenceUnitMetadata );

		final List<String> allKnownClassNames = mutableJoin(
				managedResources.getAnnotatedClassReferences().stream().map( Class::getName ).toList(),
				managedResources.getAnnotatedClassNames(),
				xmlPreProcessingResult.getMappedClasses()
		);

		managedResources.getAnnotatedPackageNames().forEach( (packageName) -> {
			try {
				final Class<?> packageInfoClass = classLoading.classForName( packageName + ".package-info" );
				allKnownClassNames.add( packageInfoClass.getName() );
			}
			catch (ClassLoadingException classLoadingException) {
				// no package-info, so there can be no annotations... just skip it
			}
		} );
		managedResources.getAnnotatedClassReferences().forEach( (clazz) -> allKnownClassNames.add( clazz.getName() ) );

		final IndexView jandexIndex = buildJandexIndex
				? buildJandexIndex( classLoading, allKnownClassNames )
				: null;

		final ModelsContext ModelsContext =
				createModelsContext( jandexIndex, classLoading );

		final RootMappingDefaults rootMappingDefaults =
				new RootMappingDefaults( metadataBuildingOptions.getMappingDefaults(), persistenceUnitMetadata );

		final GlobalRegistrationsImpl globalRegistrations =
				new GlobalRegistrationsImpl( ModelsContext, bootstrapContext );
		final DomainModelCategorizationCollector modelCategorizationCollector = new DomainModelCategorizationCollector(
				globalRegistrations,
				ModelsContext
		);

		final XmlProcessingResult xmlProcessingResult = XmlProcessor.processXml(
				xmlPreProcessingResult,
				persistenceUnitMetadata,
				modelCategorizationCollector::apply,
				ModelsContext,
				bootstrapContext,
				rootMappingDefaults
		);

		final ClassDetailsRegistry classDetailsRegistry = ModelsContext.getClassDetailsRegistry();
		allKnownClassNames.forEach( (className) ->
				modelCategorizationCollector.apply( classDetailsRegistry.resolveClassDetails( className ) ) );
		xmlPreProcessingResult.getMappedNames().forEach( (className) ->
				modelCategorizationCollector.apply( classDetailsRegistry.resolveClassDetails( className ) ) );

		xmlProcessingResult.apply();

		return ModelsContext;
	}

	private static ModelsContext createModelsContext(
			IndexView jandexIndex, ClassLoaderServiceLoading classLoading) {
		return jandexIndex == null
				? new BasicModelsContextImpl( classLoading, false, ModelsHelper::preFillRegistries )
				: new JandexModelsContextImpl( jandexIndex, false, classLoading, ModelsHelper::preFillRegistries );
	}

	private static IndexView buildJandexIndex(ClassLoaderServiceLoading classLoading, List<String> classNames) {
		final Indexer indexer = new Indexer();
		forEachJavaType( (javaType) -> JandexIndexerHelper.apply( javaType, indexer, classLoading ) );
		forEachOrmAnnotation( (descriptor) -> JandexIndexerHelper.apply( descriptor.getAnnotationType(), indexer, classLoading ) );

		classNames.forEach( (className) -> {
			final URL classUrl = classLoading.locateResource( className.replace( '.', '/' ) + ".class" );
			if ( classUrl == null ) {
				throw new RuntimeException( "Could not locate class file : " + className );
			}
			try (final InputStream classFileStream = classUrl.openStream() ) {
				indexer.index( classFileStream );
			}
			catch (IOException e) {
				throw new RuntimeException( e );
			}
		} );

		return indexer.complete();
	}
}
