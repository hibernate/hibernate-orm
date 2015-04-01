/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.boot.internal;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.persistence.Converter;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.archive.scan.internal.StandardScanner;
import org.hibernate.boot.archive.scan.spi.ClassDescriptor;
import org.hibernate.boot.archive.scan.spi.JandexInitializer;
import org.hibernate.boot.archive.scan.spi.MappingFileDescriptor;
import org.hibernate.boot.archive.scan.spi.PackageDescriptor;
import org.hibernate.boot.archive.scan.spi.ScanParameters;
import org.hibernate.boot.archive.scan.spi.ScanResult;
import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.internal.DeploymentResourcesInterpreter.DeploymentResources;
import org.hibernate.boot.internal.MetadataBuilderImpl.MetadataBuildingOptionsImpl;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.model.source.internal.annotations.AnnotationMetadataSourceProcessorImpl;
import org.hibernate.boot.model.source.internal.hbm.EntityHierarchyBuilder;
import org.hibernate.boot.model.source.internal.hbm.EntityHierarchySourceImpl;
import org.hibernate.boot.model.source.internal.hbm.HbmMetadataSourceProcessorImpl;
import org.hibernate.boot.model.source.internal.hbm.MappingDocument;
import org.hibernate.boot.model.source.internal.hbm.ModelBinder;
import org.hibernate.boot.model.source.spi.MetadataSourceProcessor;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.AdditionalJaxbMappingProducer;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.MetadataContributor;
import org.hibernate.cfg.AttributeConverterDefinition;
import org.hibernate.cfg.MetadataSourceType;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.TypeFactory;
import org.hibernate.type.TypeResolver;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;

import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

/**
 * Represents the process of building a Metadata object.  The main entry point is the
 * static {@link #build}
 *
 * @author Steve Ebersole
 */
public class MetadataBuildingProcess {
	private static final Logger log = Logger.getLogger( MetadataBuildingProcess.class );

	public static MetadataImpl build(
			final MetadataSources sources,
			final MetadataBuildingOptionsImpl options) {
		final ClassLoaderService classLoaderService = options.getServiceRegistry().getService( ClassLoaderService.class );

		final ClassLoaderAccess classLoaderAccess = new ClassLoaderAccessImpl(
				options.getTempClassLoader(),
				classLoaderService
		);

//		final JandexInitManager jandexInitializer = buildJandexInitializer( options, classLoaderAccess );
		
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// scanning - Jandex initialization and source discovery
		if ( options.getScanEnvironment() != null ) {
			final Scanner scanner = buildScanner( options, classLoaderAccess );
			final ScanResult scanResult = scanner.scan(
					options.getScanEnvironment(),
					options.getScanOptions(),
					new ScanParameters() {
						@Override
						public JandexInitializer getJandexInitializer() {
//							return jandexInitializer;
							return null;
						}
					}
			);

			// Add to the MetadataSources any classes/packages/mappings discovered during scanning
			addScanResultsToSources( sources, options, scanResult );
		}

//		// todo : add options.getScanEnvironment().getExplicitlyListedClassNames() to jandex?
//		//		^^ - another option is to make sure that they are added to sources
//
//		if ( !jandexInitializer.wasIndexSupplied() ) {
//			// If the Jandex Index(View) was supplied, we consider that supplied
//			// one "complete".
//			// Here though we were NOT supplied an index; in this case we want to
//			// additionally ensure that any-and-all "known" classes are added to
//			// the index we are building
//			sources.indexKnownClasses( jandexInitializer );
//		}
		
		// It's necessary to delay the binding of XML resources until now.  ClassLoaderAccess is needed for
		// reflection, etc.
//		sources.buildBindResults( classLoaderAccess );

//		final IndexView jandexView = augmentJandexFromMappings( jandexInitializer.buildIndex(), sources, options );
		final IndexView jandexView = options.getJandexView();

		final BasicTypeRegistry basicTypeRegistry = handleTypes( options );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// prep to start handling binding in earnest

//		final JandexAccessImpl jandexAccess = new JandexAccessImpl(
//				jandexView,
//				classLoaderAccess
//
//		);
		final InFlightMetadataCollectorImpl metadataCollector = new InFlightMetadataCollectorImpl(
				options,
				sources,
				new TypeResolver( basicTypeRegistry, new TypeFactory() )
		);

		final MetadataBuildingContextRootImpl rootMetadataBuildingContext = new MetadataBuildingContextRootImpl(
				options,
				classLoaderAccess,
				metadataCollector
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Set up the processors and start binding
		//		NOTE : this becomes even more simplified after we move purely
		// 		to unified model

		final MetadataSourceProcessor processor = new MetadataSourceProcessor() {
			private final HbmMetadataSourceProcessorImpl hbmProcessor = new HbmMetadataSourceProcessorImpl(
					sources,
					rootMetadataBuildingContext
			);

			private final AnnotationMetadataSourceProcessorImpl annotationProcessor = new AnnotationMetadataSourceProcessorImpl(
					sources,
					rootMetadataBuildingContext,
					jandexView
			);

			@Override
			public void prepare() {
				hbmProcessor.prepare();
				annotationProcessor.prepare();
			}

			@Override
			public void processTypeDefinitions() {
				hbmProcessor.processTypeDefinitions();
				annotationProcessor.processTypeDefinitions();
			}

			@Override
			public void processQueryRenames() {
				hbmProcessor.processQueryRenames();
				annotationProcessor.processQueryRenames();
			}

			@Override
			public void processNamedQueries() {
				hbmProcessor.processNamedQueries();
				annotationProcessor.processNamedQueries();
			}

			@Override
			public void processAuxiliaryDatabaseObjectDefinitions() {
				hbmProcessor.processAuxiliaryDatabaseObjectDefinitions();
				annotationProcessor.processAuxiliaryDatabaseObjectDefinitions();
			}

			@Override
			public void processIdentifierGenerators() {
				hbmProcessor.processIdentifierGenerators();
				annotationProcessor.processIdentifierGenerators();
			}

			@Override
			public void processFilterDefinitions() {
				hbmProcessor.processFilterDefinitions();
				annotationProcessor.processFilterDefinitions();
			}

			@Override
			public void processFetchProfiles() {
				hbmProcessor.processFetchProfiles();
				annotationProcessor.processFetchProfiles();
			}

			@Override
			public void prepareForEntityHierarchyProcessing() {
				for ( MetadataSourceType metadataSourceType : options.getSourceProcessOrdering() ) {
					if ( metadataSourceType == MetadataSourceType.HBM ) {
						hbmProcessor.prepareForEntityHierarchyProcessing();
					}

					if ( metadataSourceType == MetadataSourceType.CLASS ) {
						annotationProcessor.prepareForEntityHierarchyProcessing();
					}
				}
			}

			@Override
			public void processEntityHierarchies(Set<String> processedEntityNames) {
				for ( MetadataSourceType metadataSourceType : options.getSourceProcessOrdering() ) {
					if ( metadataSourceType == MetadataSourceType.HBM ) {
						hbmProcessor.processEntityHierarchies( processedEntityNames );
					}

					if ( metadataSourceType == MetadataSourceType.CLASS ) {
						annotationProcessor.processEntityHierarchies( processedEntityNames );
					}
				}
			}

			@Override
			public void postProcessEntityHierarchies() {
				for ( MetadataSourceType metadataSourceType : options.getSourceProcessOrdering() ) {
					if ( metadataSourceType == MetadataSourceType.HBM ) {
						hbmProcessor.postProcessEntityHierarchies();
					}

					if ( metadataSourceType == MetadataSourceType.CLASS ) {
						annotationProcessor.postProcessEntityHierarchies();
					}
				}
			}

			@Override
			public void processResultSetMappings() {
				hbmProcessor.processResultSetMappings();
				annotationProcessor.processResultSetMappings();
			}

			@Override
			public void finishUp() {
				hbmProcessor.finishUp();
				annotationProcessor.finishUp();
			}
		};

		processor.prepare();

		processor.processTypeDefinitions();
		processor.processQueryRenames();
		processor.processAuxiliaryDatabaseObjectDefinitions();

		processor.processIdentifierGenerators();
		processor.processFilterDefinitions();
		processor.processFetchProfiles();

		final Set<String> processedEntityNames = new HashSet<String>();
		processor.prepareForEntityHierarchyProcessing();
		processor.processEntityHierarchies( processedEntityNames );
		processor.postProcessEntityHierarchies();

		processor.processResultSetMappings();
		processor.processNamedQueries();

		processor.finishUp();

		for ( MetadataContributor contributor : classLoaderService.loadJavaServices( MetadataContributor.class ) ) {
			log.tracef( "Calling MetadataContributor : %s", contributor );
			contributor.contribute( metadataCollector, jandexView );
		}

		metadataCollector.processSecondPasses( rootMetadataBuildingContext );

		Iterable<AdditionalJaxbMappingProducer> producers = classLoaderService.loadJavaServices( AdditionalJaxbMappingProducer.class );
		if ( producers != null ) {
			final EntityHierarchyBuilder hierarchyBuilder = new EntityHierarchyBuilder();
//			final MappingBinder mappingBinder = new MappingBinder( true );
			// We need to disable validation here.  It seems Envers is not producing valid (according to schema) XML
			final MappingBinder mappingBinder = new MappingBinder( false );
			for ( AdditionalJaxbMappingProducer producer : producers ) {
				log.tracef( "Calling AdditionalJaxbMappingProducer : %s", producer );
				Collection<MappingDocument> additionalMappings = producer.produceAdditionalMappings(
						metadataCollector,
						jandexView,
						mappingBinder,
						rootMetadataBuildingContext
				);
				for ( MappingDocument mappingDocument : additionalMappings ) {
					hierarchyBuilder.indexMappingDocument( mappingDocument );
				}
			}

			ModelBinder binder = ModelBinder.prepare( rootMetadataBuildingContext );
			for ( EntityHierarchySourceImpl entityHierarchySource : hierarchyBuilder.buildHierarchies() ) {
				binder.bindEntityHierarchy( entityHierarchySource );
			}
		}

		return metadataCollector.buildMetadataInstance( rootMetadataBuildingContext );
	}

//	private static JandexInitManager buildJandexInitializer(
//			MetadataBuildingOptions options,
//			ClassLoaderAccess classLoaderAccess) {
//		final boolean autoIndexMembers = ConfigurationHelper.getBoolean(
//				org.hibernate.cfg.AvailableSettings.ENABLE_AUTO_INDEX_MEMBER_TYPES,
//				options.getServiceRegistry().getService( ConfigurationService.class ).getSettings(),
//				false
//		);
//
//		return new JandexInitManager( options.getJandexView(), classLoaderAccess, autoIndexMembers );
//	}

	private static final Class[] SINGLE_ARG = new Class[] { ArchiveDescriptorFactory.class };

	private static Scanner buildScanner(MetadataBuildingOptions options, ClassLoaderAccess classLoaderAccess) {
		final Object scannerSetting = options.getScanner();
		final ArchiveDescriptorFactory archiveDescriptorFactory = options.getArchiveDescriptorFactory();

		if ( scannerSetting == null ) {
			// No custom Scanner specified, use the StandardScanner
			if ( archiveDescriptorFactory == null ) {
				return new StandardScanner();
			}
			else {
				return new StandardScanner( archiveDescriptorFactory );
			}
		}
		else {
			if ( Scanner.class.isInstance( scannerSetting ) ) {
				if ( archiveDescriptorFactory != null ) {
					throw new IllegalStateException(
							"A Scanner instance and an ArchiveDescriptorFactory were both specified; please " +
									"specify one or the other, or if you need to supply both, Scanner class to use " +
									"(assuming it has a constructor accepting a ArchiveDescriptorFactory).  " +
									"Alternatively, just pass the ArchiveDescriptorFactory during your own " +
									"Scanner constructor assuming it is statically known."
					);
				}
				return (Scanner) scannerSetting;
			}

			final Class<? extends  Scanner> scannerImplClass;
			if ( Class.class.isInstance( scannerSetting ) ) {
				scannerImplClass = (Class<? extends Scanner>) scannerSetting;
			}
			else {
				scannerImplClass = classLoaderAccess.classForName( scannerSetting.toString() );
			}


			if ( archiveDescriptorFactory != null ) {
				// find the single-arg constructor - its an error if none exists
				try {
					final Constructor<? extends Scanner> constructor = scannerImplClass.getConstructor( SINGLE_ARG );
					try {
						return constructor.newInstance( archiveDescriptorFactory );
					}
					catch (Exception e) {
						throw new IllegalStateException(
								"Error trying to instantiate custom specified Scanner [" +
										scannerImplClass.getName() + "]",
								e
						);
					}
				}
				catch (NoSuchMethodException e) {
					throw new IllegalArgumentException(
							"Configuration named a custom Scanner and a custom ArchiveDescriptorFactory, but " +
									"Scanner impl did not define a constructor accepting ArchiveDescriptorFactory"
					);
				}
			}
			else {
				// could be either ctor form...
				// find the single-arg constructor - its an error if none exists
				try {
					final Constructor<? extends Scanner> constructor = scannerImplClass.getConstructor( SINGLE_ARG );
					try {
						return constructor.newInstance( StandardArchiveDescriptorFactory.INSTANCE );
					}
					catch (Exception e) {
						throw new IllegalStateException(
								"Error trying to instantiate custom specified Scanner [" +
										scannerImplClass.getName() + "]",
								e
						);
					}
				}
				catch (NoSuchMethodException e) {
					try {
						final Constructor<? extends Scanner> constructor = scannerImplClass.getConstructor();
						try {
							return constructor.newInstance();
						}
						catch (Exception e2) {
							throw new IllegalStateException(
									"Error trying to instantiate custom specified Scanner [" +
											scannerImplClass.getName() + "]",
									e2
							);
						}
					}
					catch (NoSuchMethodException ignore) {
						throw new IllegalArgumentException(
								"Configuration named a custom Scanner, but we were unable to locate " +
										"an appropriate constructor"
						);
					}
				}
			}
		}
	}

	private static void addScanResultsToSources(
			MetadataSources sources,
			MetadataBuildingOptionsImpl options,
			ScanResult scanResult) {
		final ClassLoaderService cls = options.getServiceRegistry().getService( ClassLoaderService.class );

		DeploymentResources deploymentResources = DeploymentResourcesInterpreter.INSTANCE.buildDeploymentResources(
				options.getScanEnvironment(),
				scanResult,
				options.getServiceRegistry()
		);

		for ( ClassDescriptor classDescriptor : deploymentResources.getClassDescriptors() ) {
			final String className = classDescriptor.getName();

			// todo : leverage Jandex calls after we fully integrate Jandex...
			try {
				final Class classRef = cls.classForName( className );

				// logic here assumes an entity is not also a converter...
				final Converter converter = (Converter) classRef.getAnnotation( Converter.class );
				if ( converter != null ) {
					//noinspection unchecked
					options.addAttributeConverterDefinition(
							AttributeConverterDefinition.from( classRef, converter.autoApply() )
					);
				}
				else {
					sources.addAnnotatedClass( classRef );
				}
			}
			catch (ClassLoadingException e) {
				// Not really sure what this means...
				sources.addAnnotatedClassName( className );
			}
		}

		for ( PackageDescriptor packageDescriptor : deploymentResources.getPackageDescriptors() ) {
			sources.addPackage( packageDescriptor.getName() );
		}

		for ( MappingFileDescriptor mappingFileDescriptor : deploymentResources.getMappingFileDescriptors() ) {
			sources.addInputStream( mappingFileDescriptor.getStreamAccess() );
		}
	}


	private static BasicTypeRegistry handleTypes(MetadataBuildingOptions options) {
		final ClassLoaderService classLoaderService = options.getServiceRegistry().getService( ClassLoaderService.class );

		// ultimately this needs to change a little bit to account for HHH-7792
		final BasicTypeRegistry basicTypeRegistry = new BasicTypeRegistry();

		final TypeContributions typeContributions = new TypeContributions() {
			@Override
			public void contributeType(org.hibernate.type.BasicType type) {
				basicTypeRegistry.register( type );
			}

			@Override
			public void contributeType(UserType type, String[] keys) {
				basicTypeRegistry.register( type, keys );
			}

			@Override
			public void contributeType(CompositeUserType type, String[] keys) {
				basicTypeRegistry.register( type, keys );
			}
		};

		// add Dialect contributed types
		final Dialect dialect = options.getServiceRegistry().getService( JdbcServices.class ).getDialect();
		dialect.contributeTypes( typeContributions, options.getServiceRegistry() );

		// add TypeContributor contributed types.
		for ( TypeContributor contributor : classLoaderService.loadJavaServices( TypeContributor.class ) ) {
			contributor.contribute( typeContributions, options.getServiceRegistry() );
		}

		// add explicit application registered types
		for ( org.hibernate.type.BasicType basicType : options.getBasicTypeRegistrations() ) {
			basicTypeRegistry.register( basicType );
		}

		return basicTypeRegistry;
	}

}
