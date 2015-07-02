/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.process.spi;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.ClassLoaderAccessImpl;
import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.internal.MetadataBuildingContextRootImpl;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.model.process.internal.ManagedResourcesImpl;
import org.hibernate.boot.model.process.internal.ScanningCoordinator;
import org.hibernate.boot.model.source.internal.annotations.AnnotationMetadataSourceProcessorImpl;
import org.hibernate.boot.model.source.internal.hbm.EntityHierarchyBuilder;
import org.hibernate.boot.model.source.internal.hbm.EntityHierarchySourceImpl;
import org.hibernate.boot.model.source.internal.hbm.HbmMetadataSourceProcessorImpl;
import org.hibernate.boot.model.source.internal.hbm.MappingDocument;
import org.hibernate.boot.model.source.internal.hbm.ModelBinder;
import org.hibernate.boot.model.source.spi.MetadataSourceProcessor;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.AdditionalJaxbMappingProducer;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.MetadataContributor;
import org.hibernate.boot.spi.MetadataImplementor;
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
 * Represents the process of of transforming a {@link org.hibernate.boot.MetadataSources}
 * reference into a {@link org.hibernate.boot.Metadata} reference.  Allows for 2 different process paradigms:<ul>
 *     <li>
 *         Single step : as defined by the {@link #build} method; internally leverages the 2-step paradigm
 *     </li>
 *     <li>
 *         Two step : a first step coordinates resource scanning and some other preparation work; a second step
 *         builds the {@link org.hibernate.boot.Metadata}.  A hugely important distinction in the need for the
 *         steps is that the first phase should strive to not load user entity/component classes so that we can still
 *         perform enhancement on them later.  This approach caters to the 2-phase bootstrap we use in regards
 *         to WildFly Hibernate-JPA integration.  The first step is defined by {@link #prepare} which returns
 *         a {@link ManagedResources} instance.  The second step is defined by calling
 *         {@link #complete}
 *     </li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class MetadataBuildingProcess {
	private static final Logger log = Logger.getLogger( MetadataBuildingProcess.class );

	/**
	 * Unified single phase for MetadataSources->Metadata process
	 *
	 * @param sources The MetadataSources
	 * @param options The building options
	 *
	 * @return The built Metadata
	 */
	public static MetadataImplementor build(
			final MetadataSources sources,
			final MetadataBuildingOptions options) {
		return complete( prepare( sources, options ), options );
	}

	/**
	 * First step of 2-phase for MetadataSources->Metadata process
	 *
	 * @param sources The MetadataSources
	 * @param options The building options
	 *
	 * @return Token/memento representing all known users resources (classes, packages, mapping files, etc).
	 */
	public static ManagedResources prepare(
			final MetadataSources sources,
			final MetadataBuildingOptions options) {
		final ManagedResourcesImpl managedResources = ManagedResourcesImpl.baseline( sources, options );
		ScanningCoordinator.INSTANCE.coordinateScan( managedResources, options, sources.getXmlMappingBinderAccess() );
		return managedResources;
	}

	/**
	 * Second step of 2-phase for MetadataSources->Metadata process
	 *
	 * @param managedResources The token/memento from 1st phase
	 * @param options The building options
	 *
	 * @return Token/memento representing all known users resources (classes, packages, mapping files, etc).
	 */
	public static MetadataImplementor complete(final ManagedResources managedResources, final MetadataBuildingOptions options) {
		final BasicTypeRegistry basicTypeRegistry = handleTypes( options );

		final InFlightMetadataCollectorImpl metadataCollector = new InFlightMetadataCollectorImpl(
				options,
				new TypeResolver( basicTypeRegistry, new TypeFactory() )
		);
		for ( AttributeConverterDefinition attributeConverterDefinition : managedResources.getAttributeConverterDefinitions() ) {
			metadataCollector.addAttributeConverter( attributeConverterDefinition );
		}

		final ClassLoaderService classLoaderService = options.getServiceRegistry().getService( ClassLoaderService.class );

		final ClassLoaderAccess classLoaderAccess = new ClassLoaderAccessImpl(
				options.getTempClassLoader(),
				classLoaderService
		);

		final MetadataBuildingContextRootImpl rootMetadataBuildingContext = new MetadataBuildingContextRootImpl(
				options,
				classLoaderAccess,
				metadataCollector
		);

		final IndexView jandexView = options.getJandexView();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Set up the processors and start binding
		//		NOTE : this becomes even more simplified after we move purely
		// 		to unified model

		final MetadataSourceProcessor processor = new MetadataSourceProcessor() {
			private final HbmMetadataSourceProcessorImpl hbmProcessor = new HbmMetadataSourceProcessorImpl(
					managedResources,
					rootMetadataBuildingContext
			);

			private final AnnotationMetadataSourceProcessorImpl annotationProcessor = new AnnotationMetadataSourceProcessorImpl(
					managedResources,
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
			final MappingBinder mappingBinder = new MappingBinder( classLoaderService, false );
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
