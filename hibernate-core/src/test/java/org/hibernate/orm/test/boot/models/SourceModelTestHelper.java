/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models;

import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.mapping.internal.context.RootMappingDefaults;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.models.internal.ClassLoaderServiceLoading;
import org.hibernate.boot.models.internal.DomainModelCategorizationCollector;
import org.hibernate.boot.models.internal.GlobalRegistrationsImpl;
import org.hibernate.boot.models.internal.ModelsHelper;
import org.hibernate.boot.mapping.internal.xml.PersistenceUnitMetadataImpl;
import org.hibernate.boot.mapping.internal.xml.XmlPreProcessingResult;
import org.hibernate.boot.mapping.internal.xml.XmlPreProcessor;
import org.hibernate.boot.mapping.internal.xml.XmlProcessingResult;
import org.hibernate.boot.mapping.internal.xml.XmlProcessor;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.pipeline.internal.MappingResolutionOptions;
import org.hibernate.models.UnknownClassException;
import org.hibernate.models.jandex.Settings;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.ClassLoading;
import org.hibernate.models.spi.ModelsConfiguration;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.forEachOrmAnnotation;
import static org.hibernate.internal.util.collections.CollectionHelper.mutableJoin;

/**
 * Test helper for creating source-model contexts and applying XML overlays.
 * This helper intentionally does not build legacy metadata.
 *
 * @author Steve Ebersole
 */
public class SourceModelTestHelper {
	public static final ClassLoading SIMPLE_CLASS_LOADING = new ClassLoading() {
		private final ClassLoader classLoader = SourceModelTestHelper.class.getClassLoader();

		@Override
		@SuppressWarnings("unchecked")
		public <T> Class<T> classForName(String name) {
			try {
				return (Class<T>) Class.forName( name, false, classLoader );
			}
			catch (ClassNotFoundException e) {
				throw new UnknownClassException( "Unable to load class " + name, e );
			}
		}

		@Override
		public <T> Class<T> findClassForName(String name) {
			try {
				return classForName( name );
			}
			catch (UnknownClassException e) {
				return null;
			}
		}

		@Override
		public URL locateResource(String resourceName) {
			return classLoader.getResource( resourceName );
		}

		@Override
		public <S> Collection<S> loadJavaServices(Class<S> serviceType) {
			return ServiceLoader.load( serviceType, classLoader ).stream()
					.map( ServiceLoader.Provider::get )
					.toList();
		}
	};

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
		final ModelsConfiguration configuration = new ModelsConfiguration()
				.setClassLoading( classLoadingAccess )
				.setRegistryPrimer( (contributions, buildingContext1) ->
						forEachOrmAnnotation( contributions::registerAnnotation ) );
		if ( jandexIndex != null ) {
			configuration.configValue( Settings.INDEX_PARAM, jandexIndex );
		}
		final ModelsContext ctx = configuration.bootstrap();

		if ( jandexIndex != null ) {

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
		forEachOrmAnnotation( descriptor -> indexClass( descriptor.getAnnotationType(), indexer ) );

		for ( Class<?> modelClass : modelClasses ) {
			indexClass( modelClass, indexer );
		}

		return indexer.complete();
	}

	public static ModelsContext createBuildingContext(
			ManagedResources managedResources,
			StandardServiceRegistry serviceRegistry) {
		final org.hibernate.boot.pipeline.internal.MappingResolutionOptionsImpl metadataBuildingOptions =
				new org.hibernate.boot.pipeline.internal.MappingResolutionOptionsImpl( serviceRegistry );
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
		final org.hibernate.boot.pipeline.internal.MappingResolutionOptionsImpl metadataBuildingOptions =
				new org.hibernate.boot.pipeline.internal.MappingResolutionOptionsImpl( serviceRegistry );
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
			MappingResolutionOptions metadataBuildingOptions,
			BootstrapContext bootstrapContext) {
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
		final var metadataBuildingContext =
				new MetadataBuildingContextTestingImpl( bootstrapContext.getServiceRegistry() );

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
				metadataBuildingContext,
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
		final ModelsConfiguration configuration = new ModelsConfiguration()
				.setClassLoading( classLoading )
				.setRegistryPrimer( ModelsHelper::preFillRegistries );
		if ( jandexIndex != null ) {
			configuration.configValue( Settings.INDEX_PARAM, jandexIndex );
		}
		return configuration.bootstrap();
	}

	private static IndexView buildJandexIndex(ClassLoaderServiceLoading classLoading, List<String> classNames) {
		final Indexer indexer = new Indexer();
		forEachOrmAnnotation( descriptor -> indexClass( descriptor.getAnnotationType(), indexer ) );

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

	private static void indexClass(Class<?> type, Indexer indexer) {
		try {
			indexer.indexClass( type );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}
}
