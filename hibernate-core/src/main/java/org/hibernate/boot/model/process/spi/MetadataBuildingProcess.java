/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.process.spi;

import java.io.InputStream;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.Internal;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.internal.MetadataBuildingContextRootImpl;
import org.hibernate.boot.internal.RootMappingDefaults;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.model.process.internal.ManagedResourcesImpl;
import org.hibernate.boot.model.process.internal.ScanningCoordinator;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.model.source.internal.annotations.AnnotationMetadataSourceProcessorImpl;
import org.hibernate.boot.model.source.internal.annotations.DomainModelSource;
import org.hibernate.boot.model.source.internal.hbm.EntityHierarchyBuilder;
import org.hibernate.boot.model.source.internal.hbm.EntityHierarchySourceImpl;
import org.hibernate.boot.model.source.internal.hbm.HbmMetadataSourceProcessorImpl;
import org.hibernate.boot.model.source.internal.hbm.MappingDocument;
import org.hibernate.boot.model.source.internal.hbm.ModelBinder;
import org.hibernate.boot.model.source.spi.MetadataSourceProcessor;
import org.hibernate.boot.models.internal.DomainModelCategorizationCollector;
import org.hibernate.boot.models.xml.spi.XmlPreProcessingResult;
import org.hibernate.boot.models.xml.spi.XmlPreProcessor;
import org.hibernate.boot.models.xml.spi.XmlProcessingResult;
import org.hibernate.boot.models.xml.spi.XmlProcessor;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.AdditionalMappingContributions;
import org.hibernate.boot.spi.AdditionalMappingContributor;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.EffectiveMappingDefaults;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MappingDefaults;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.mapping.Table;
import org.hibernate.models.internal.MutableClassDetailsRegistry;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.WrapperArrayHandling;
import org.hibernate.type.descriptor.java.ByteArrayJavaType;
import org.hibernate.type.descriptor.java.CharacterArrayJavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JsonArrayAsStringJdbcType;
import org.hibernate.type.descriptor.jdbc.JsonAsStringJdbcType;
import org.hibernate.type.descriptor.jdbc.XmlAsStringJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.internal.NamedBasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.CompositeUserType;


import jakarta.persistence.AttributeConverter;

import static org.hibernate.internal.util.collections.CollectionHelper.mutableJoin;
import static org.hibernate.internal.util.config.ConfigurationHelper.getPreferredSqlTypeCodeForArray;
import static org.hibernate.internal.util.config.ConfigurationHelper.getPreferredSqlTypeCodeForDuration;
import static org.hibernate.internal.util.config.ConfigurationHelper.getPreferredSqlTypeCodeForInstant;
import static org.hibernate.internal.util.config.ConfigurationHelper.getPreferredSqlTypeCodeForUuid;

/**
 * Represents the process of transforming a {@link MetadataSources}
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

	/**
	 * Unified single phase for MetadataSources to Metadata process
	 *
	 * @param sources The MetadataSources
	 * @param options The building options
	 *
	 * @return The built Metadata
	 */
	public static MetadataImplementor build(
			final MetadataSources sources,
			final BootstrapContext bootstrapContext,
			final MetadataBuildingOptions options) {
		return complete( prepare( sources, bootstrapContext ), bootstrapContext, options );
	}

	/**
	 * First step of two-phase for MetadataSources to Metadata process
	 *
	 * @param sources The MetadataSources
	 * @param bootstrapContext The bootstrapContext
	 *
	 * @return Token/memento representing all known users resources (classes, packages, mapping files, etc).
	 */
	public static ManagedResources prepare(
			final MetadataSources sources,
			final BootstrapContext bootstrapContext) {
		final ManagedResourcesImpl managedResources = ManagedResourcesImpl.baseline( sources, bootstrapContext );
		final ConfigurationService configService =
				bootstrapContext.getServiceRegistry().requireService( ConfigurationService.class );
		final boolean xmlMappingEnabled = configService.getSetting(
				AvailableSettings.XML_MAPPING_ENABLED,
				StandardConverters.BOOLEAN,
				true
		);
		ScanningCoordinator.INSTANCE.coordinateScan(
				managedResources,
				bootstrapContext,
				xmlMappingEnabled ? sources.getXmlMappingBinderAccess() : null
		);
		return managedResources;
	}

	/**
	 * Second step of two-phase for MetadataSources to Metadata process
	 *
	 * @param managedResources The token/memento from 1st phase
	 * @param options The building options
	 *
	 * @return Token/memento representing all known users resources (classes, packages, mapping files, etc).
	 */
	public static MetadataImplementor complete(
			final ManagedResources managedResources,
			final BootstrapContext bootstrapContext,
			final MetadataBuildingOptions options) {

		final ClassLoaderService classLoaderService = bootstrapContext.getServiceRegistry().getService( ClassLoaderService.class );
		assert classLoaderService != null;
		final InFlightMetadataCollectorImpl metadataCollector = new InFlightMetadataCollectorImpl( bootstrapContext, options );

		handleTypes( bootstrapContext, options, metadataCollector );

		final DomainModelSource domainModelSource = processManagedResources(
				managedResources,
				metadataCollector,
				bootstrapContext,
				options.getMappingDefaults()
		);

		final MetadataBuildingContextRootImpl rootMetadataBuildingContext = new MetadataBuildingContextRootImpl(
				"orm",
				bootstrapContext,
				options,
				metadataCollector,
				domainModelSource.getEffectiveMappingDefaults()
		);

		managedResources.getAttributeConverterDescriptors().forEach( metadataCollector::addAttributeConverter );

		bootstrapContext.getTypeConfiguration().scope( rootMetadataBuildingContext );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Set up the processors and start binding
		//		NOTE : this becomes even more simplified after we move purely
		// 		to unified model
//		final IndexView jandexView = domainModelSource.getJandexIndex();

		coordinateProcessors(
				managedResources,
				options,
				rootMetadataBuildingContext,
				domainModelSource,
				metadataCollector
		);

		processAdditionalMappingContributions( metadataCollector, options, classLoaderService, rootMetadataBuildingContext );

		applyExtraQueryImports( managedResources, metadataCollector );

		return metadataCollector.buildMetadataInstance( rootMetadataBuildingContext );
	}

	private static void coordinateProcessors(
			ManagedResources managedResources,
			MetadataBuildingOptions options,
			MetadataBuildingContextRootImpl rootMetadataBuildingContext,
			DomainModelSource domainModelSource,
			InFlightMetadataCollectorImpl metadataCollector) {
		final MetadataSourceProcessor processor = new MetadataSourceProcessor() {
			private final MetadataSourceProcessor hbmProcessor = options.isXmlMappingEnabled()
					? new HbmMetadataSourceProcessorImpl( managedResources, rootMetadataBuildingContext )
					: new NoOpMetadataSourceProcessorImpl();

			private final AnnotationMetadataSourceProcessorImpl annotationProcessor =
					new AnnotationMetadataSourceProcessorImpl(
							managedResources,
							domainModelSource,
							rootMetadataBuildingContext
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
				hbmProcessor.prepareForEntityHierarchyProcessing();
				annotationProcessor.prepareForEntityHierarchyProcessing();
			}

			@Override
			public void processEntityHierarchies(Set<String> processedEntityNames) {
				hbmProcessor.processEntityHierarchies( processedEntityNames );
				annotationProcessor.processEntityHierarchies( processedEntityNames );
			}

			@Override
			public void postProcessEntityHierarchies() {
				hbmProcessor.postProcessEntityHierarchies();
				annotationProcessor.postProcessEntityHierarchies();
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

		final Set<String> processedEntityNames = new HashSet<>();
		processor.prepareForEntityHierarchyProcessing();
		processor.processEntityHierarchies( processedEntityNames );
		processor.postProcessEntityHierarchies();

		processor.processResultSetMappings();

		metadataCollector.processSecondPasses( rootMetadataBuildingContext );

		// Make sure collections are fully bound before processing named queries as hbm result set mappings require it
		processor.processNamedQueries();

		processor.finishUp();
	}

	@Internal
	public static DomainModelSource processManagedResources(
			ManagedResources managedResources,
			InFlightMetadataCollector metadataCollector,
			BootstrapContext bootstrapContext,
			MappingDefaults optionDefaults) {
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// 	- pre-process the XML
		// 	- collect all known classes
		// 	- resolve (possibly building) Jandex index
		// 	- build the SourceModelBuildingContext
		//
		// INPUTS:
		//		- serviceRegistry
		//		- managedResources
		//		- bootstrapContext (supplied Jandex index, if one)
		//
		// OUTPUTS:
		//		- xmlPreProcessingResult
		//		- allKnownClassNames (technically could be included in xmlPreProcessingResult)
		//		- sourceModelBuildingContext

		final SourceModelBuildingContext sourceModelBuildingContext = metadataCollector.getSourceModelBuildingContext();
		final XmlPreProcessingResult xmlPreProcessingResult = XmlPreProcessor.preProcessXmlResources(
				managedResources,
				metadataCollector.getPersistenceUnitMetadata()
		);

		assert metadataCollector.getPersistenceUnitMetadata() == xmlPreProcessingResult.getPersistenceUnitMetadata();

		//noinspection unchecked
		final List<String> allKnownClassNames = mutableJoin(
				managedResources.getAnnotatedClassReferences().stream().map( Class::getName ).collect( Collectors.toList() ),
				managedResources.getAnnotatedClassNames(),
				xmlPreProcessingResult.getMappedClasses()
		);
		managedResources.getAnnotatedPackageNames().forEach( (packageName) -> {
			try {
				final Class<?> packageInfoClass = sourceModelBuildingContext.getClassLoading().classForName( packageName + ".package-info" );
				allKnownClassNames.add( packageInfoClass.getName() );
			}
			catch (ClassLoadingException classLoadingException) {
				// no package-info, so there can be no annotations... just skip it
			}
		} );
		managedResources.getAnnotatedClassReferences().forEach( (clazz) -> allKnownClassNames.add( clazz.getName() ) );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// 	- process metadata-complete XML
		//	- collect overlay XML
		//	- process annotations (including those from metadata-complete XML)
		//	- apply overlay XML
		//
		// INPUTS:
		//		- "options" (areIdGeneratorsGlobal, etc)
		//		- xmlPreProcessingResult
		//		- sourceModelBuildingContext
		//
		// OUTPUTS
		//		- rootEntities
		//		- mappedSuperClasses
		//  	- embeddables

		// JPA id generator global-ity thing
		final boolean areIdGeneratorsGlobal = true;
		final ClassDetailsRegistry classDetailsRegistry = sourceModelBuildingContext.getClassDetailsRegistry();
		final DomainModelCategorizationCollector modelCategorizationCollector = new DomainModelCategorizationCollector(
				areIdGeneratorsGlobal,
				metadataCollector.getGlobalRegistrations(),
				sourceModelBuildingContext
		);

		final RootMappingDefaults rootMappingDefaults = new RootMappingDefaults(
				optionDefaults,
				xmlPreProcessingResult.getPersistenceUnitMetadata()
		);
		final XmlProcessingResult xmlProcessingResult = XmlProcessor.processXml(
				xmlPreProcessingResult,
				modelCategorizationCollector,
				sourceModelBuildingContext,
				bootstrapContext,
				rootMappingDefaults
		);

		final HashSet<String> categorizedClassNames = new HashSet<>();
		allKnownClassNames.forEach( (className) -> applyKnownClass(
				className,
				categorizedClassNames,
				classDetailsRegistry,
				modelCategorizationCollector
		) );
		xmlPreProcessingResult.getMappedNames().forEach( (className) -> applyKnownClass(
				className,
				categorizedClassNames,
				classDetailsRegistry,
				modelCategorizationCollector
		) );

		xmlProcessingResult.apply( xmlPreProcessingResult.getPersistenceUnitMetadata() );

		return new DomainModelSource(
				classDetailsRegistry,
				allKnownClassNames,
				modelCategorizationCollector.getGlobalRegistrations(),
				rootMappingDefaults,
				xmlPreProcessingResult.getPersistenceUnitMetadata()
		);
	}

	private static void applyKnownClass(
			String className,
			HashSet<String> categorizedClassNames,
			ClassDetailsRegistry classDetailsRegistry,
			DomainModelCategorizationCollector modelCategorizationCollector) {
		if ( categorizedClassNames.add( className ) ) {
			final ClassDetails classDetails = classDetailsRegistry.resolveClassDetails( className );
			applyKnownClass( classDetails, categorizedClassNames,classDetailsRegistry, modelCategorizationCollector );
		}
	}

	private static void applyKnownClass(
			ClassDetails classDetails,
			HashSet<String> categorizedClassNames,
			ClassDetailsRegistry classDetailsRegistry,
			DomainModelCategorizationCollector modelCategorizationCollector) {
		modelCategorizationCollector.apply( classDetails );
		if ( classDetails.getSuperClass() != null
				&& classDetails.getSuperClass() != ClassDetails.OBJECT_CLASS_DETAILS ) {
			if ( categorizedClassNames.add( classDetails.getSuperClass().getClassName() ) ) {
				applyKnownClass( classDetails.getSuperClass(), categorizedClassNames, classDetailsRegistry, modelCategorizationCollector );
			}
		}
	}

	private static void processAdditionalMappingContributions(
			InFlightMetadataCollectorImpl metadataCollector,
			MetadataBuildingOptions options,
			ClassLoaderService classLoaderService,
			MetadataBuildingContextRootImpl rootMetadataBuildingContext) {

		final AdditionalMappingContributionsImpl contributions = new AdditionalMappingContributionsImpl(
				metadataCollector,
				options,
				options.isXmlMappingEnabled() ? new MappingBinder( classLoaderService, () -> false ) : null,
				rootMetadataBuildingContext
		);

		final Collection<AdditionalMappingContributor> additionalMappingContributors = classLoaderService.loadJavaServices( AdditionalMappingContributor.class );
		additionalMappingContributors.forEach( (contributor) -> {
			contributions.setCurrentContributor( contributor.getContributorName() );
			try {
				contributor.contribute(
						contributions,
						metadataCollector,
						classLoaderService,
						rootMetadataBuildingContext
				);
			}
			finally {
				contributions.setCurrentContributor( null );
			}
		} );

		contributions.complete();
	}

	private static class AdditionalMappingContributionsImpl implements AdditionalMappingContributions {
		private final InFlightMetadataCollectorImpl metadataCollector;
		private final MetadataBuildingOptions options;
		private final MappingBinder mappingBinder;
		private final MetadataBuildingContextRootImpl rootMetadataBuildingContext;
		private final EntityHierarchyBuilder hierarchyBuilder = new EntityHierarchyBuilder();

		private List<Class<?>> additionalEntityClasses;
		private List<ClassDetails> additionalClassDetails;
		private List<JaxbEntityMappingsImpl> additionalJaxbMappings;
		private boolean extraHbmXml = false;

		private String currentContributor;

		public AdditionalMappingContributionsImpl(
				InFlightMetadataCollectorImpl metadataCollector,
				MetadataBuildingOptions options,
				MappingBinder mappingBinder,
				MetadataBuildingContextRootImpl rootMetadataBuildingContext) {
			this.metadataCollector = metadataCollector;
			this.options = options;
			this.mappingBinder = mappingBinder;
			this.rootMetadataBuildingContext = rootMetadataBuildingContext;
		}

		public void setCurrentContributor(String contributor) {
			this.currentContributor = contributor == null ? "orm" : contributor;
		}

		@Override
		public void contributeEntity(Class<?> entityType) {
			if ( additionalEntityClasses == null ) {
				additionalEntityClasses = new ArrayList<>();
			}
			additionalEntityClasses.add( entityType );
		}

		@Override
		public void contributeManagedClass(ClassDetails classDetails) {
			if ( additionalClassDetails == null ) {
				additionalClassDetails = new ArrayList<>();
			}
			additionalClassDetails.add( classDetails );
			metadataCollector.getSourceModelBuildingContext()
					.getClassDetailsRegistry()
					.as( MutableClassDetailsRegistry.class )
					.addClassDetails( classDetails.getName(), classDetails );
		}

		@Override
		public void contributeBinding(InputStream xmlStream) {
			final Origin origin = new Origin( SourceType.INPUT_STREAM, null );
			final Binding<JaxbBindableMappingDescriptor> binding = mappingBinder.bind( xmlStream, origin );

			final JaxbBindableMappingDescriptor bindingRoot = binding.getRoot();
			if ( bindingRoot instanceof JaxbHbmHibernateMapping ) {
				contributeBinding( (JaxbHbmHibernateMapping) bindingRoot );
			}
			else {
				contributeBinding( (JaxbEntityMappingsImpl) bindingRoot );
			}
		}

		@Override
		public void contributeBinding(JaxbEntityMappingsImpl mappingJaxbBinding) {
			if ( ! options.isXmlMappingEnabled() ) {
				return;
			}

			if ( additionalJaxbMappings == null ) {
				additionalJaxbMappings = new ArrayList<>();
			}
			additionalJaxbMappings.add( mappingJaxbBinding );
		}

		@Override
		public void contributeBinding(JaxbHbmHibernateMapping hbmJaxbBinding) {
			if ( ! options.isXmlMappingEnabled() ) {
				return;
			}

			extraHbmXml = true;

			hierarchyBuilder.indexMappingDocument( new MappingDocument(
					currentContributor,
					hbmJaxbBinding,
					new Origin( SourceType.OTHER, null ),
					rootMetadataBuildingContext
			) );
		}

		@Override
		public void contributeTable(Table table) {
			final Namespace namespace = metadataCollector.getDatabase().locateNamespace(
					table.getCatalogIdentifier(),
					table.getSchemaIdentifier()
			);
			namespace.registerTable( table.getNameIdentifier(), table );
			metadataCollector.addTableNameBinding( table.getNameIdentifier(), table );
		}

		@Override
		public void contributeSequence(Sequence sequence) {
			final Namespace namespace = metadataCollector.getDatabase().locateNamespace(
					sequence.getName().getCatalogName(),
					sequence.getName().getSchemaName()
			);
			namespace.registerSequence( sequence.getName().getSequenceName(), sequence );
		}

		@Override
		public void contributeAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject) {
			metadataCollector.addAuxiliaryDatabaseObject( auxiliaryDatabaseObject );
		}

		@Override
		public EffectiveMappingDefaults getEffectiveMappingDefaults() {
			return rootMetadataBuildingContext.getEffectiveDefaults();
		}

		public void complete() {
			// annotations / orm.xml
			if ( additionalEntityClasses != null || additionalClassDetails != null || additionalJaxbMappings != null ) {
				AnnotationMetadataSourceProcessorImpl.processAdditionalMappings(
						additionalEntityClasses,
						additionalClassDetails,
						additionalJaxbMappings,
						rootMetadataBuildingContext,
						options
				);
			}

			// hbm.xml
			if ( extraHbmXml ) {
				final ModelBinder binder = ModelBinder.prepare( rootMetadataBuildingContext );
				for ( EntityHierarchySourceImpl entityHierarchySource : hierarchyBuilder.buildHierarchies() ) {
					binder.bindEntityHierarchy( entityHierarchySource );
				}
			}
		}
	}


	private static void applyExtraQueryImports(
			ManagedResources managedResources,
			InFlightMetadataCollectorImpl metadataCollector) {
		final Map<String, Class<?>> extraQueryImports = managedResources.getExtraQueryImports();
		if ( extraQueryImports == null || extraQueryImports.isEmpty() ) {
			return;
		}

		for ( Map.Entry<String, Class<?>> entry : extraQueryImports.entrySet() ) {
			metadataCollector.addImport( entry.getKey(), entry.getValue().getName() );
		}
	}

//	todo (7.0) : buildJandexInitializer
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

	private static void handleTypes(
			BootstrapContext bootstrapContext,
			MetadataBuildingOptions options,
			InFlightMetadataCollector metadataCollector) {
		final ClassLoaderService classLoaderService =
				options.getServiceRegistry().requireService(ClassLoaderService.class);

		final TypeConfiguration typeConfiguration = bootstrapContext.getTypeConfiguration();
		final StandardServiceRegistry serviceRegistry = bootstrapContext.getServiceRegistry();
		final JdbcTypeRegistry jdbcTypeRegistry = typeConfiguration.getJdbcTypeRegistry();
		final TypeContributions typeContributions = new TypeContributions() {
			@Override
			public TypeConfiguration getTypeConfiguration() {
				return typeConfiguration;
			}

			@Override
			public void contributeAttributeConverter(Class<? extends AttributeConverter<?, ?>> converterClass) {
				metadataCollector.getConverterRegistry().addAttributeConverter( converterClass );
			}

			@Override
			public void contributeType(CompositeUserType<?> type) {
				options.getCompositeUserTypes().add( type );
			}
		};

		if ( options.getWrapperArrayHandling() == WrapperArrayHandling.LEGACY ) {
			typeConfiguration.getJavaTypeRegistry().addDescriptor( ByteArrayJavaType.INSTANCE );
			typeConfiguration.getJavaTypeRegistry().addDescriptor( CharacterArrayJavaType.INSTANCE );
			final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();

			basicTypeRegistry.addTypeReferenceRegistrationKey(
					StandardBasicTypes.CHARACTER_ARRAY.getName(),
					Character[].class.getName(), "Character[]"
			);
			basicTypeRegistry.addTypeReferenceRegistrationKey(
					StandardBasicTypes.BINARY_WRAPPER.getName(),
					Byte[].class.getName(), "Byte[]"
					);
		}

		// add Dialect contributed types
		final Dialect dialect = options.getServiceRegistry().requireService( JdbcServices.class ).getDialect();
		dialect.contribute( typeContributions, options.getServiceRegistry() );
		// Capture the dialect configured JdbcTypes so that we can detect if a TypeContributor overwrote them,
		// which has precedence over the fallback and preferred type registrations
		final JdbcType dialectArrayDescriptor = jdbcTypeRegistry.findDescriptor( SqlTypes.ARRAY );

		// add TypeContributor contributed types.
		for ( TypeContributor contributor : classLoaderService.loadJavaServices( TypeContributor.class ) ) {
			contributor.contribute( typeContributions, options.getServiceRegistry() );
		}

		// add fallback type descriptors
		final int preferredSqlTypeCodeForUuid = getPreferredSqlTypeCodeForUuid( serviceRegistry );
		if ( preferredSqlTypeCodeForUuid != SqlTypes.UUID ) {
			adaptToPreferredSqlTypeCode(
					typeConfiguration,
					jdbcTypeRegistry,
					preferredSqlTypeCodeForUuid,
					UUID.class,
					StandardBasicTypes.UUID.getName(),
					"org.hibernate.type.PostgresUUIDType",
					"uuid",
					"pg-uuid"
			);
		}
		else {
			addFallbackIfNecessary( jdbcTypeRegistry, SqlTypes.UUID, SqlTypes.BINARY );
		}

		final int preferredSqlTypeCodeForArray = getPreferredSqlTypeCodeForArray( serviceRegistry );
		if ( preferredSqlTypeCodeForArray != SqlTypes.ARRAY ) {
			adaptToPreferredSqlTypeCode(
					jdbcTypeRegistry,
					dialectArrayDescriptor,
					SqlTypes.ARRAY,
					preferredSqlTypeCodeForArray
			);
		}
		else {
			addFallbackIfNecessary( jdbcTypeRegistry, SqlTypes.ARRAY, SqlTypes.VARBINARY );
		}

		final int preferredSqlTypeCodeForDuration = getPreferredSqlTypeCodeForDuration( serviceRegistry );
		if ( preferredSqlTypeCodeForDuration != SqlTypes.INTERVAL_SECOND ) {
			adaptToPreferredSqlTypeCode(
					typeConfiguration,
					jdbcTypeRegistry,
					preferredSqlTypeCodeForDuration,
					Duration.class,
					StandardBasicTypes.DURATION.getName(),
					"org.hibernate.type.DurationType"
			);
		}
		else {
			addFallbackIfNecessary( jdbcTypeRegistry, SqlTypes.INTERVAL_SECOND, SqlTypes.DURATION );
		}

		addFallbackIfNecessary( jdbcTypeRegistry, SqlTypes.INET, SqlTypes.VARBINARY );
		addFallbackIfNecessary( jdbcTypeRegistry, SqlTypes.GEOMETRY, SqlTypes.VARBINARY );
		addFallbackIfNecessary( jdbcTypeRegistry, SqlTypes.POINT, SqlTypes.VARBINARY );
		addFallbackIfNecessary( jdbcTypeRegistry, SqlTypes.GEOGRAPHY, SqlTypes.GEOMETRY );

		jdbcTypeRegistry.addDescriptorIfAbsent( JsonAsStringJdbcType.VARCHAR_INSTANCE );
		jdbcTypeRegistry.addDescriptorIfAbsent( JsonArrayAsStringJdbcType.VARCHAR_INSTANCE );
		jdbcTypeRegistry.addDescriptorIfAbsent( XmlAsStringJdbcType.VARCHAR_INSTANCE );

		addFallbackIfNecessary( jdbcTypeRegistry, SqlTypes.MATERIALIZED_BLOB, SqlTypes.BLOB );
		addFallbackIfNecessary( jdbcTypeRegistry, SqlTypes.MATERIALIZED_CLOB, SqlTypes.CLOB );
		addFallbackIfNecessary( jdbcTypeRegistry, SqlTypes.MATERIALIZED_NCLOB, SqlTypes.NCLOB );

		final DdlTypeRegistry ddlTypeRegistry = typeConfiguration.getDdlTypeRegistry();
		// Fallback to the geometry DdlType when geography is requested
		final DdlType geometryType = ddlTypeRegistry.getDescriptor( SqlTypes.GEOMETRY );
		if ( geometryType != null ) {
			ddlTypeRegistry.addDescriptorIfAbsent(
					new DdlTypeImpl(
							SqlTypes.GEOGRAPHY,
							geometryType.getTypeName( (Long) null, (Integer) null, (Integer) null ),
							dialect
					)
			);
		}

		// add explicit application registered types
		typeConfiguration.addBasicTypeRegistrationContributions( options.getBasicTypeRegistrations() );
		for ( CompositeUserType<?> compositeUserType : options.getCompositeUserTypes() ) {
			//noinspection unchecked
			metadataCollector.registerCompositeUserType(
					compositeUserType.returnedClass(),
					(Class<? extends CompositeUserType<?>>) compositeUserType.getClass()
			);
		}

		final JdbcType timestampWithTimeZoneOverride = getTimestampWithTimeZoneOverride( options, jdbcTypeRegistry );
		if ( timestampWithTimeZoneOverride != null ) {
			adaptTimestampTypesToDefaultTimeZoneStorage( typeConfiguration, timestampWithTimeZoneOverride );
		}
		final JdbcType timeWithTimeZoneOverride = getTimeWithTimeZoneOverride( options, jdbcTypeRegistry );
		if ( timeWithTimeZoneOverride != null ) {
			adaptTimeTypesToDefaultTimeZoneStorage( typeConfiguration, timeWithTimeZoneOverride );
		}
		final int preferredSqlTypeCodeForInstant = getPreferredSqlTypeCodeForInstant( serviceRegistry );
		if ( preferredSqlTypeCodeForInstant != SqlTypes.TIMESTAMP_UTC ) {
			adaptToPreferredSqlTypeCode(
					typeConfiguration,
					jdbcTypeRegistry,
					preferredSqlTypeCodeForInstant,
					Instant.class,
					StandardBasicTypes.INSTANT.getName(),
					"org.hibernate.type.InstantType",
					"instant"
			);
		}
	}

	private static void adaptToPreferredSqlTypeCode(
			JdbcTypeRegistry jdbcTypeRegistry,
			JdbcType dialectUuidDescriptor,
			int defaultSqlTypeCode,
			int preferredSqlTypeCode) {
		if ( jdbcTypeRegistry.findDescriptor( defaultSqlTypeCode ) == dialectUuidDescriptor ) {
			jdbcTypeRegistry.addDescriptor(
					defaultSqlTypeCode,
					jdbcTypeRegistry.getDescriptor( preferredSqlTypeCode )
			);
		}
		// else warning?
	}

	private static void adaptToPreferredSqlTypeCode(
			TypeConfiguration typeConfiguration,
			JdbcTypeRegistry jdbcTypeRegistry,
			int preferredSqlTypeCode,
			Class<?> javaType,
			String name,
			String... additionalKeys) {
		final JavaTypeRegistry javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();
		final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
		final BasicType<?> basicType = new NamedBasicTypeImpl<>(
				javaTypeRegistry.getDescriptor( javaType ),
				jdbcTypeRegistry.getDescriptor( preferredSqlTypeCode ),
				name
		);
		final String[] keys = Arrays.copyOf( additionalKeys, additionalKeys.length + 2 );
		keys[additionalKeys.length] = javaType.getSimpleName();
		keys[additionalKeys.length + 1] = javaType.getName();
		basicTypeRegistry.register( basicType, keys );
	}

	private static void adaptTimeTypesToDefaultTimeZoneStorage(
			TypeConfiguration typeConfiguration,
			JdbcType timestampWithTimeZoneOverride) {
		final JavaTypeRegistry javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();
		final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
		final BasicType<?> offsetDateTimeType = new NamedBasicTypeImpl<>(
				javaTypeRegistry.getDescriptor( OffsetTime.class ),
				timestampWithTimeZoneOverride,
				"OffsetTime"
		);
		basicTypeRegistry.register(
				offsetDateTimeType,
				"org.hibernate.type.OffsetTimeType",
				OffsetTime.class.getSimpleName(),
				OffsetTime.class.getName()
		);
	}

	private static void adaptTimestampTypesToDefaultTimeZoneStorage(
			TypeConfiguration typeConfiguration,
			JdbcType timestampWithTimeZoneOverride) {
		final JavaTypeRegistry javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();
		final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
		final BasicType<?> offsetDateTimeType = new NamedBasicTypeImpl<>(
				javaTypeRegistry.getDescriptor( OffsetDateTime.class ),
				timestampWithTimeZoneOverride,
				"OffsetDateTime"
		);
		final BasicType<?> zonedDateTimeType = new NamedBasicTypeImpl<>(
				javaTypeRegistry.getDescriptor( ZonedDateTime.class ),
				timestampWithTimeZoneOverride,
				"ZonedDateTime"
		);
		basicTypeRegistry.register(
				offsetDateTimeType,
				"org.hibernate.type.OffsetDateTimeType",
				OffsetDateTime.class.getSimpleName(),
				OffsetDateTime.class.getName()
		);
		basicTypeRegistry.register(
				zonedDateTimeType,
				"org.hibernate.type.ZonedDateTimeType",
				ZonedDateTime.class.getSimpleName(),
				ZonedDateTime.class.getName()
		);
	}

	private static JdbcType getTimeWithTimeZoneOverride(MetadataBuildingOptions options, JdbcTypeRegistry jdbcTypeRegistry) {
		return switch ( options.getDefaultTimeZoneStorage() ) {
			case NORMALIZE ->
				// For NORMALIZE, we replace the standard types that use TIME_WITH_TIMEZONE to use TIME
					jdbcTypeRegistry.getDescriptor( Types.TIME );
			case NORMALIZE_UTC ->
				// For NORMALIZE_UTC, we replace the standard types that use TIME_WITH_TIMEZONE to use TIME_UTC
					jdbcTypeRegistry.getDescriptor( SqlTypes.TIME_UTC );
			default -> null;
		};
	}

	private static JdbcType getTimestampWithTimeZoneOverride(MetadataBuildingOptions options, JdbcTypeRegistry jdbcTypeRegistry) {
		return switch (options.getDefaultTimeZoneStorage()) {
			case NORMALIZE ->
				// For NORMALIZE, we replace the standard types that use TIMESTAMP_WITH_TIMEZONE to use TIMESTAMP
					jdbcTypeRegistry.getDescriptor( Types.TIMESTAMP );
			case NORMALIZE_UTC ->
				// For NORMALIZE_UTC, we replace the standard types that use TIMESTAMP_WITH_TIMEZONE to use TIMESTAMP_UTC
					jdbcTypeRegistry.getDescriptor( SqlTypes.TIMESTAMP_UTC );
			default -> null;
		};
	}

	private static void addFallbackIfNecessary(
			JdbcTypeRegistry jdbcTypeRegistry,
			int typeCode,
			int fallbackTypeCode) {
		if ( !jdbcTypeRegistry.hasRegisteredDescriptor( typeCode ) ) {
			jdbcTypeRegistry.addDescriptor( typeCode, jdbcTypeRegistry.getDescriptor( fallbackTypeCode ) );
		}
	}
}
