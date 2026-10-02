package org.hibernate.boot.model.process.spi;

import org.hibernate.MappingException;
import org.hibernate.boot.model.process.internal.ManagedClassDetails;
import org.hibernate.boot.model.process.internal.ManagedResourceValidation;
import org.hibernate.boot.model.process.internal.ManagedResourcesBuilder;
import org.hibernate.boot.models.spi.GlobalRegistrations;
import org.hibernate.boot.models.xml.spi.PersistenceUnitMetadata;
import org.hibernate.models.internal.jdk.JdkClassDetails;
import org.hibernate.models.spi.ModelsContext;

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
import java.util.Comparator;
import java.util.HashSet;
import org.hibernate.boot.models.xml.internal.XmlPreProcessingResultImpl;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.AttributeConverter;
import org.hibernate.AssertionFailure;
import org.hibernate.Internal;
import org.hibernate.Remove;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.internal.MetadataBuildingContextRootImpl;
import org.hibernate.boot.internal.RootMappingDefaults;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.model.convert.internal.ConverterDescriptors;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.model.process.internal.ManagedResourcesImpl;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.model.source.internal.annotations.AnnotationMetadataSourceProcessorImpl;
import org.hibernate.boot.model.source.internal.annotations.DomainModelSource;
import org.hibernate.boot.model.source.internal.hbm.EntityHierarchyBuilder;
import org.hibernate.boot.model.source.internal.hbm.HbmMetadataSourceProcessorImpl;
import org.hibernate.boot.model.source.internal.hbm.MappingDocument;
import org.hibernate.boot.model.source.internal.hbm.ModelBinder;
import org.hibernate.boot.model.source.spi.MetadataSourceProcessor;
import org.hibernate.boot.models.internal.DomainModelCategorizationCollector;
import org.hibernate.boot.models.xml.spi.XmlPreProcessor;
import org.hibernate.boot.models.xml.spi.XmlProcessor;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.AdditionalMappingContributions;
import org.hibernate.boot.spi.AdditionalMappingContributor;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.EffectiveMappingDefaults;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.JpaOrmXmlPersistenceUnitDefaultAware;
import org.hibernate.boot.spi.MappingDefaults;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.WrapperArrayHandling;
import org.hibernate.type.descriptor.java.ByteArrayJavaType;
import org.hibernate.type.descriptor.java.CharacterArrayJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeConstructor;
import org.hibernate.type.descriptor.jdbc.JavaTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.JsonArrayJdbcTypeConstructor;
import org.hibernate.type.descriptor.jdbc.JsonAsStringArrayJdbcTypeConstructor;
import org.hibernate.type.descriptor.jdbc.JsonAsStringJdbcType;
import org.hibernate.type.descriptor.jdbc.UuidAsBinaryJdbcType;
import org.hibernate.type.descriptor.jdbc.XmlArrayJdbcTypeConstructor;
import org.hibernate.type.descriptor.jdbc.XmlAsStringArrayJdbcTypeConstructor;
import org.hibernate.type.descriptor.jdbc.XmlAsStringJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.internal.NamedBasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.CompositeUserType;

import static org.hibernate.internal.util.config.ConfigurationHelper.getPreferredSqlTypeCodeForArray;
import static org.hibernate.internal.util.config.ConfigurationHelper.getPreferredSqlTypeCodeForDuration;
import static org.hibernate.internal.util.config.ConfigurationHelper.getPreferredSqlTypeCodeForInstant;
import static org.hibernate.internal.util.config.ConfigurationHelper.getPreferredSqlTypeCodeForUuid;

/**
 * Represents the process of transforming a {@link MetadataSources}
 * reference into a {@link org.hibernate.boot.Metadata} reference.  Allows for 2 different process paradigms:<ul>
 *     <li>
 *         Single step: as defined by the {@link #build} method; internally leverages the 2-step paradigm
 *     </li>
 *     <li>
 *         Two step: a first step coordinates resource scanning and some other preparation work; a second step
 *         builds the {@link org.hibernate.boot.Metadata}. A hugely important distinction in the need for the
 *         steps is that the first phase should strive to not load user entity/component classes so that we can still
 *         perform enhancement on them later. This approach caters to the 2-phase bootstrap we use in regard to
 *         WildFly Hibernate-JPA integration. The first step is defined by {@link #prepare} which returns
 *         a {@link ManagedResources} instance. The second step is defined by calling {@link #complete}
 *     </li>
 * </ul>
 *
 * @author Steve Ebersole
 */
@Remove
public class MetadataBuildingProcess {

	private static final Comparator<TypeContributor> TYPE_CONTRIBUTOR_COMPARATOR = Comparator.comparingInt(
					TypeContributor::ordinal )
			.thenComparing( a -> a.getClass().getCanonicalName() );

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
	 * First step of two-phase for {@link MetadataSources} to
	 * {@link org.hibernate.boot.Metadata} process
	 *
	 * @param sources The MetadataSources
	 * @param bootstrapContext The bootstrapContext
	 *
	 * @return Token/memento representing all known users resources
	 *         (classes, packages, mapping files, and so on).
	 */
	public static ManagedResources prepare(
			final MetadataSources sources,
			final BootstrapContext bootstrapContext) {
		return ManagedResourcesImpl.baseline( sources, bootstrapContext );
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

		final var metadataCollector = new InFlightMetadataCollectorImpl( bootstrapContext, options );

		handleTypes( bootstrapContext, options, metadataCollector );

		final var domainModelSource = processManagedResources(
				managedResources,
				metadataCollector,
				bootstrapContext,
				options.getMappingDefaults()
		);

		// use any persistence-unit-defaults defined in orm.xml
		( (JpaOrmXmlPersistenceUnitDefaultAware) options )
				.apply( domainModelSource.getPersistenceUnitMetadata() );

		final var rootMetadataBuildingContext = new MetadataBuildingContextRootImpl(
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

		processAdditionalMappingContributions( metadataCollector, options,
				bootstrapContext.getClassLoaderService(), rootMetadataBuildingContext );

		applyExtraQueryImports( managedResources, metadataCollector );

		return metadataCollector.buildMetadataInstance( rootMetadataBuildingContext );
	}

	@Internal
	public static void coordinateProcessors(
			ManagedResources managedResources,
			MetadataBuildingOptions options,
			MetadataBuildingContextRootImpl rootMetadataBuildingContext,
			DomainModelSource domainModelSource,
			InFlightMetadataCollectorImpl metadataCollector) {
		final MetadataSourceProcessor hbmProcessor = options.isXmlMappingEnabled()
				? new HbmMetadataSourceProcessorImpl( managedResources, rootMetadataBuildingContext )
				: new NoOpMetadataSourceProcessorImpl();

		final AnnotationMetadataSourceProcessorImpl annotationProcessor = new AnnotationMetadataSourceProcessorImpl(
				domainModelSource,
				rootMetadataBuildingContext
		);

		final var bootstrapContext = rootMetadataBuildingContext.getBootstrapContext();
		final var classLoaderService = bootstrapContext.getClassLoaderService();
		assert classLoaderService != null;

		final var converterRegistry =
				rootMetadataBuildingContext.getMetadataCollector().getConverterRegistry();
		domainModelSource.getConversionRegistrations().forEach( registration -> {
			final var explicitDomainType = registration.getExplicitDomainType();
			converterRegistry.addRegisteredConversion( new RegisteredConversion(
					explicitDomainType == void.class || explicitDomainType == Void.class
							? void.class
							: explicitDomainType,
					registration.getConverterType(),
					registration.isAutoApply()
			) );
		} );
		domainModelSource.getConverterRegistrations().forEach( registration ->
				converterRegistry.addAttributeConverter( ConverterDescriptors.of(
						classLoaderService.classForName( registration.converterClass().getClassName() ),
						registration.autoApply(), false
				) ) );

		final var processor = new MetadataSourceProcessor() {

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

		processor.prepareForEntityHierarchyProcessing();
		processor.processEntityHierarchies( new HashSet<>() );
		processor.postProcessEntityHierarchies();

		processor.processResultSetMappings();

		metadataCollector.processSecondPasses( rootMetadataBuildingContext );

		// Make sure collections are fully bound before processing
		// named queries as hbm result set mappings require it
		processor.processNamedQueries();

		processor.finishUp();
	}

	@Internal
	public static DomainModelSource processManagedResources(
			ManagedResources managedResources,
			InFlightMetadataCollector metadataCollector,
			BootstrapContext bootstrapContext,
			MappingDefaults optionDefaults) {
		return processManagedResources( managedResources, bootstrapContext, optionDefaults,
				bootstrapContext.getModelsContext(), metadataCollector.getPersistenceUnitMetadata(),
				metadataCollector.getGlobalRegistrations() );
	}

	@Internal
	public static DomainModelSource processManagedResources(
			ManagedResources managedResources,
			BootstrapContext bootstrapContext,
			MappingDefaults optionDefaults,
			ModelsContext modelsContext,
			PersistenceUnitMetadata aggregatedPersistenceUnitMetadata,
			GlobalRegistrations globalRegistrations) {
		final var registry = modelsContext.getClassDetailsRegistry();
		final var xml = bootstrapContext.getMetadataBuildingOptions().isXmlMappingEnabled()
				? XmlPreProcessor.preProcessXmlResources( managedResources, aggregatedPersistenceUnitMetadata )
				: new XmlPreProcessingResultImpl( aggregatedPersistenceUnitMetadata );
		final var javaTypes = new LinkedHashMap<String, ClassDetails>();
		final var dynamicTypes = new LinkedHashMap<String, ClassDetails>();
		final var packages = new LinkedHashMap<String, ClassDetails>();
		final var modules = new LinkedHashMap<String, DomainModelSource.ModuleDescriptor>();

		managedResources.getClassDetails().forEach( details ->
				ManagedClassDetails.register( details, registry ) );
		final var identities = new ManagedResourcesBuilder();
		for ( var type : managedResources.getAnnotatedClassReferences() ) {
			identities.addClass( type );
			var details = registry.findClassDetails( type.getName() );
			if ( details == null ) {
				details = new JdkClassDetails( type, modelsContext );
				ManagedClassDetails.register( details, registry );
			}
			addJavaType( details, javaTypes );
		}
		for ( var name : managedResources.getAnnotatedClassNames() ) {
			ManagedResourceValidation.validateClassName( name );
			addJavaType( registry.resolveClassDetails( name ), javaTypes );
		}
		managedResources.getClassDetails().forEach( details -> addManagedType( details, javaTypes, dynamicTypes ) );
		for ( var name : xml.getMappedClasses() ) {
			ManagedResourceValidation.validateClassName( name );
			addJavaType( registry.resolveClassDetails( name ), javaTypes );
		}
		for ( var packageName : new LinkedHashSet<>( managedResources.getAnnotatedPackageNames() ) ) {
			packages.put( packageName, registry.resolveExplicitPackageDetails( packageName ) );
		}
		for ( var moduleName : managedResources.getAnnotatedModuleNames() ) {
			modules.computeIfAbsent( moduleName, name -> new DomainModelSource.ModuleDescriptor(
					name, modelsContext.getModuleDetailsRegistry().resolveModuleDetails( name ) ) );
		}

		final var categorizer = new DomainModelCategorizationCollector( globalRegistrations, modelsContext );
		final var categorized = new HashSet<String>();
		javaTypes.values().forEach( details -> applyKnownClass( details, categorized, categorizer ) );
		dynamicTypes.values().forEach( details -> applyKnownClass( details, categorized, categorizer ) );
		packages.values().forEach( categorizer::apply );
		final var defaults = new RootMappingDefaults( optionDefaults, aggregatedPersistenceUnitMetadata );
		for ( var name : xml.getMappedNames() ) {
			final var existing = registry.findClassDetails( name );
			if ( existing != null && existing.getClassName() != null && !existing.getClassName().isEmpty() ) {
				throw new MappingException( "Dynamic model name conflicts with Java type '" + name + "'" );
			}
		}
		final var processedXml = XmlProcessor.processXml( xml, aggregatedPersistenceUnitMetadata,
				categorizer::apply, modelsContext, bootstrapContext, defaults );
		for ( var name : xml.getMappedNames() ) {
			final var details = registry.resolveClassDetails( name );
			if ( details.getClassName() != null && !details.getClassName().isEmpty() ) {
				throw new MappingException( "Dynamic model name conflicts with Java type '" + name + "'" );
			}
			dynamicTypes.putIfAbsent( name, details );
			applyKnownClass( details, categorized, categorizer );
		}
		processedXml.apply();
		return new DomainModelSource( registry, List.copyOf( javaTypes.values() ), List.copyOf( dynamicTypes.values() ),
				List.copyOf( packages.values() ), List.copyOf( modules.values() ),
				categorizer.getGlobalRegistrations(), defaults, aggregatedPersistenceUnitMetadata );
	}

	private static void addJavaType(ClassDetails details, Map<String, ClassDetails> javaTypes) {
		if ( details.getClassName() == null || details.getClassName().isEmpty() ) {
			throw new MappingException( "Java type declaration conflicts with dynamic model '" + details.getName() + "'" );
		}
		javaTypes.putIfAbsent( details.getName(), details );
	}

	private static void addManagedType(
			ClassDetails details,
			Map<String, ClassDetails> javaTypes,
			Map<String, ClassDetails> dynamicTypes) {
		final var target = details.getClassName() == null || details.getClassName().isEmpty() ? dynamicTypes : javaTypes;
		target.putIfAbsent( details.getName(), details );
	}

	private static void applyKnownClass(
			ClassDetails details, Set<String> categorized, DomainModelCategorizationCollector categorizer) {
		if ( categorized.add( details.getName() ) ) {
			categorizer.apply( details );
			final var superClass = details.getSuperClass();
			if ( superClass != null && superClass != ClassDetails.OBJECT_CLASS_DETAILS ) {
				applyKnownClass( superClass, categorized, categorizer );
			}
		}
	}

	private static void processAdditionalMappingContributions(
			InFlightMetadataCollectorImpl metadataCollector,
			MetadataBuildingOptions options,
			ClassLoaderService classLoaderService,
			MetadataBuildingContextRootImpl rootMetadataBuildingContext) {

		final var contributions =
				new AdditionalMappingContributionsImpl(
						metadataCollector,
						options,
						options.isXmlMappingEnabled()
								? new MappingBinder( classLoaderService, () -> false )
								: null,
						rootMetadataBuildingContext
				);

		final var additionalMappingContributors =
				classLoaderService.loadJavaServices( AdditionalMappingContributor.class );
		additionalMappingContributors.forEach( contributor -> {
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

		private Map<String, List<Class<?>>> additionalEntityClassesByContributor;
		private Map<String, List<ClassDetails>> additionalClassDetailsByContributor;
		private Map<String, List<JaxbEntityMappingsImpl>> additionalJaxbMappingsByContributor;
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
			if ( additionalEntityClassesByContributor == null ) {
				additionalEntityClassesByContributor = new LinkedHashMap<>();
			}
			additionalEntityClassesByContributor
					.computeIfAbsent( currentContributor, k -> new ArrayList<>() )
					.add( entityType );
		}

		@Override
		public void contributeManagedClass(ClassDetails classDetails) {
			if ( additionalClassDetailsByContributor == null ) {
				additionalClassDetailsByContributor = new LinkedHashMap<>();
			}
			additionalClassDetailsByContributor
					.computeIfAbsent( currentContributor, k -> new ArrayList<>() )
					.add( classDetails );

			ManagedClassDetails.register(
					classDetails, rootMetadataBuildingContext.getBootstrapContext().getModelsContext().getClassDetailsRegistry() );
		}

		@Override
		public void contributeBinding(InputStream xmlStream) {
			final var origin = new Origin( SourceType.INPUT_STREAM, null );
			final var bindingRoot = mappingBinder.bind( xmlStream, origin ).getRoot();
			if ( bindingRoot instanceof JaxbHbmHibernateMapping hibernateMapping ) {
				contributeBinding( hibernateMapping );
			}
			else if ( bindingRoot instanceof JaxbEntityMappingsImpl entityMappings ) {
				contributeBinding( entityMappings );
			}
			else {
				throw new AssertionFailure( "Unexpected binding type" );
			}
		}

		@Override
		public void contributeBinding(JaxbEntityMappingsImpl mappingJaxbBinding) {
			if ( options.isXmlMappingEnabled() ) {
				if ( additionalJaxbMappingsByContributor == null ) {
					additionalJaxbMappingsByContributor = new LinkedHashMap<>();
				}
				additionalJaxbMappingsByContributor
						.computeIfAbsent( currentContributor, k -> new ArrayList<>() )
						.add( mappingJaxbBinding );
			}
		}

		@Override
		public void contributeBinding(JaxbHbmHibernateMapping hbmJaxbBinding) {
			if ( options.isXmlMappingEnabled() ) {
				extraHbmXml = true;
				hierarchyBuilder.indexMappingDocument( new MappingDocument(
						currentContributor,
						hbmJaxbBinding,
						new Origin( SourceType.OTHER, null ),
						rootMetadataBuildingContext
				) );
			}
		}

		@Override
		public void contributeTable(Table table) {
			metadataCollector.getDatabase()
					.locateNamespace( table.getCatalogIdentifier(), table.getSchemaIdentifier() )
					.registerTable( table.getNameIdentifier(), table );
			metadataCollector.addTableNameBinding( table.getNameIdentifier(), table );
		}

		@Override
		public void contributeSequence(Sequence sequence) {
			final var sequenceName = sequence.getName();
			metadataCollector.getDatabase()
					.locateNamespace( sequenceName.getCatalogName(), sequenceName.getSchemaName() )
					.registerSequence( sequenceName.getSequenceName(), sequence );
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
			if ( additionalEntityClassesByContributor != null || additionalClassDetailsByContributor != null || additionalJaxbMappingsByContributor != null ) {
				final var allContributors = new LinkedHashSet<String>();
				if ( additionalEntityClassesByContributor != null ) {
					allContributors.addAll( additionalEntityClassesByContributor.keySet() );
				}
				if ( additionalClassDetailsByContributor != null ) {
					allContributors.addAll( additionalClassDetailsByContributor.keySet() );
				}
				if ( additionalJaxbMappingsByContributor != null ) {
					allContributors.addAll( additionalJaxbMappingsByContributor.keySet() );
				}
				for ( var contributor : allContributors ) {
					final var classes = additionalEntityClassesByContributor == null
							? null
							: additionalEntityClassesByContributor.get( contributor );
					final var classDetails = additionalClassDetailsByContributor == null
							? null
							: additionalClassDetailsByContributor.get( contributor );
					final var jaxbMappings = additionalJaxbMappingsByContributor == null
							? null
							: additionalJaxbMappingsByContributor.get( contributor );

					if ( classes != null ||  classDetails != null || jaxbMappings != null ) {
						final var context = "orm".equals( contributor )
								? rootMetadataBuildingContext
								: new MetadataBuildingContextRootImpl(
										contributor,
										rootMetadataBuildingContext.getBootstrapContext(),
										options,
										metadataCollector,
										rootMetadataBuildingContext.getEffectiveDefaults()
								);
						AnnotationMetadataSourceProcessorImpl.processAdditionalMappings(
								classes,
								classDetails,
								jaxbMappings,
								context,
								options
						);
					}
				}
			}

			// hbm.xml
			if ( extraHbmXml ) {
				final var binder = ModelBinder.prepare( rootMetadataBuildingContext );
				for ( var entityHierarchySource : hierarchyBuilder.buildHierarchies() ) {
					binder.bindEntityHierarchy( entityHierarchySource );
				}
			}
		}
	}


	private static void applyExtraQueryImports(
			ManagedResources managedResources,
			InFlightMetadataCollectorImpl metadataCollector) {
		final var extraQueryImports = managedResources.getExtraQueryImports();
		if ( extraQueryImports != null && !extraQueryImports.isEmpty() ) {
			for ( var entry : extraQueryImports.entrySet() ) {
				metadataCollector.addImport( entry.getKey(), entry.getValue().getName() );
			}
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
		final var classLoaderService = bootstrapContext.getClassLoaderService();
		final var typeConfiguration = bootstrapContext.getTypeConfiguration();
		final var serviceRegistry = bootstrapContext.getServiceRegistry();
		final var jdbcTypeRegistry = typeConfiguration.getJdbcTypeRegistry();
		final var typeContributions = new TypeContributions() {
			@Override
			public TypeConfiguration getTypeConfiguration() {
				return typeConfiguration;
			}

			@Override
			public void contributeAttributeConverter(Class<? extends AttributeConverter<?,?>> converterClass) {
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
			final var basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();

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
		final var dialect =
				options.getServiceRegistry()
						.requireService( JdbcServices.class )
						.getDialect();
		dialect.contributeTypes( typeContributions, options.getServiceRegistry() );

		// add TypeContributor contributed types.
		for ( var typeContributor : sortedTypeContributors( classLoaderService ) ) {
			typeContributor.contribute( typeContributions, options.getServiceRegistry() );
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
			jdbcTypeRegistry.addDescriptorIfAbsent( UuidAsBinaryJdbcType.INSTANCE );
		}

		jdbcTypeRegistry.addDescriptorIfAbsent( JsonAsStringJdbcType.VARCHAR_INSTANCE );
		jdbcTypeRegistry.addDescriptorIfAbsent( XmlAsStringJdbcType.VARCHAR_INSTANCE );
		if ( jdbcTypeRegistry.getConstructor( SqlTypes.JSON_ARRAY ) == null ) {
			if ( jdbcTypeRegistry.getDescriptor( SqlTypes.JSON ).getDdlTypeCode() == SqlTypes.JSON ) {
				jdbcTypeRegistry.addTypeConstructor( JsonArrayJdbcTypeConstructor.INSTANCE );
			}
			else {
				jdbcTypeRegistry.addTypeConstructor( JsonAsStringArrayJdbcTypeConstructor.INSTANCE );
			}
		}
		if ( jdbcTypeRegistry.getConstructor( SqlTypes.XML_ARRAY ) == null ) {
			if ( jdbcTypeRegistry.getDescriptor( SqlTypes.SQLXML ).getDdlTypeCode() == SqlTypes.SQLXML ) {
				jdbcTypeRegistry.addTypeConstructor( XmlArrayJdbcTypeConstructor.INSTANCE );
			}
			else {
				jdbcTypeRegistry.addTypeConstructor( XmlAsStringArrayJdbcTypeConstructor.INSTANCE );
			}
		}
		if ( jdbcTypeRegistry.getConstructor( SqlTypes.ARRAY ) == null ) {
			// Default the array constructor to e.g. JSON_ARRAY/XML_ARRAY if needed
			final JdbcTypeConstructor constructor =
					jdbcTypeRegistry.getConstructor( getPreferredSqlTypeCodeForArray( serviceRegistry ) );
			if ( constructor != null ) {
				jdbcTypeRegistry.addTypeConstructor( SqlTypes.ARRAY, constructor );
			}
		}

		final int preferredSqlTypeCodeForDuration = getPreferredSqlTypeCodeForDuration( serviceRegistry );
		if ( preferredSqlTypeCodeForDuration != SqlTypes.DURATION ) {
			adaptToPreferredSqlTypeCode(
					typeConfiguration,
					jdbcTypeRegistry,
					preferredSqlTypeCodeForDuration,
					Duration.class,
					StandardBasicTypes.DURATION.getName(),
					"org.hibernate.type.DurationType"
			);
		}

		addFallbackIfNecessary( jdbcTypeRegistry, SqlTypes.INET, SqlTypes.VARBINARY );
		addFallbackIfNecessary( jdbcTypeRegistry, SqlTypes.GEOMETRY, SqlTypes.VARBINARY );
		addFallbackIfNecessary( jdbcTypeRegistry, SqlTypes.POINT, SqlTypes.VARBINARY );
		addFallbackIfNecessary( jdbcTypeRegistry, SqlTypes.GEOGRAPHY, SqlTypes.GEOMETRY );

		addFallbackIfNecessary( jdbcTypeRegistry, SqlTypes.MATERIALIZED_BLOB, SqlTypes.BLOB );
		addFallbackIfNecessary( jdbcTypeRegistry, SqlTypes.MATERIALIZED_CLOB, SqlTypes.CLOB );
		addFallbackIfNecessary( jdbcTypeRegistry, SqlTypes.MATERIALIZED_NCLOB, SqlTypes.NCLOB );

		final var ddlTypeRegistry = typeConfiguration.getDdlTypeRegistry();
		// Fallback to the geometry DdlType when geography is requested
		final var geometryType = ddlTypeRegistry.getDescriptor( SqlTypes.GEOMETRY );
		if ( geometryType != null ) {
			ddlTypeRegistry.addDescriptorIfAbsent(
					new DdlTypeImpl(
							SqlTypes.GEOGRAPHY,
							geometryType.getTypeName( Size.nil(), null, ddlTypeRegistry ),
							dialect
					)
			);
		}

		// add explicit application registered types
		typeConfiguration.addBasicTypeRegistrationContributions( options.getBasicTypeRegistrations() );
		for ( var compositeUserType : options.getCompositeUserTypes() ) {
			metadataCollector.registerCompositeUserType( compositeUserType.returnedClass(),
					ReflectHelper.getClass( compositeUserType.getClass() ) );
		}

		final var timestampWithTimeZoneOverride = getTimestampWithTimeZoneOverride( options, jdbcTypeRegistry );
		if ( timestampWithTimeZoneOverride != null ) {
			adaptTimestampTypesToDefaultTimeZoneStorage( typeConfiguration, timestampWithTimeZoneOverride );
		}
		final var timeWithTimeZoneOverride = getTimeWithTimeZoneOverride( options, jdbcTypeRegistry );
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

//	private static void adaptToPreferredSqlTypeCode(
//			JdbcTypeRegistry jdbcTypeRegistry,
//			JdbcType dialectUuidDescriptor,
//			int defaultSqlTypeCode,
//			int preferredSqlTypeCode) {
//		if ( jdbcTypeRegistry.findDescriptor( defaultSqlTypeCode ) == dialectUuidDescriptor ) {
//			jdbcTypeRegistry.addDescriptor(
//					defaultSqlTypeCode,
//					jdbcTypeRegistry.getDescriptor( preferredSqlTypeCode )
//			);
//		}
//		// else warning?
//	}

	private static List<TypeContributor> sortedTypeContributors(
			ClassLoaderService classLoaderService) {
		Collection<TypeContributor> typeContributors = classLoaderService.loadJavaServices( TypeContributor.class );
		List<TypeContributor> contributors = new ArrayList<>( typeContributors );
		contributors.sort( TYPE_CONTRIBUTOR_COMPARATOR );
		return contributors;
	}

	private static void adaptToPreferredSqlTypeCode(
			TypeConfiguration typeConfiguration,
			JdbcTypeRegistry jdbcTypeRegistry,
			int preferredSqlTypeCode,
			Class<?> javaType,
			String name,
			String... additionalKeys) {
		final var javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();
		final var basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
		final var basicType = new NamedBasicTypeImpl<>(
				javaTypeRegistry.resolveDescriptor( javaType ),
				jdbcTypeRegistry.getDescriptor( preferredSqlTypeCode ),
				name
		);
		final var keys = Arrays.copyOf( additionalKeys, additionalKeys.length + 2 );
		keys[additionalKeys.length] = javaType.getSimpleName();
		keys[additionalKeys.length + 1] = javaType.getName();
		basicTypeRegistry.register( basicType, keys );
	}

	private static void adaptTimeTypesToDefaultTimeZoneStorage(
			TypeConfiguration typeConfiguration,
			JdbcType timestampWithTimeZoneOverride) {
		final var javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();
		final var basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
		if ( !( basicTypeRegistry.getRegisteredType( OffsetTime.class ).getJdbcType() instanceof JavaTimeJdbcType ) ) {
			basicTypeRegistry.register(
					new NamedBasicTypeImpl<>(
							javaTypeRegistry.resolveDescriptor( OffsetTime.class ),
							timestampWithTimeZoneOverride,
							"OffsetTime"
					),
					"org.hibernate.type.OffsetTimeType",
					OffsetTime.class.getSimpleName(),
					OffsetTime.class.getName()
			);
		}
	}

	private static void adaptTimestampTypesToDefaultTimeZoneStorage(
			TypeConfiguration typeConfiguration,
			JdbcType timestampWithTimeZoneOverride) {
		final var javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();
		final var basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
		if ( !( basicTypeRegistry.getRegisteredType( OffsetDateTime.class ).getJdbcType()
				instanceof JavaTimeJdbcType ) ) {
			basicTypeRegistry.register(
					new NamedBasicTypeImpl<>(
							javaTypeRegistry.resolveDescriptor( OffsetDateTime.class ),
							timestampWithTimeZoneOverride,
							"OffsetDateTime"
					),
					"org.hibernate.type.OffsetDateTimeType",
					OffsetDateTime.class.getSimpleName(),
					OffsetDateTime.class.getName()
			);
		}
		if ( !( basicTypeRegistry.getRegisteredType( ZonedDateTime.class ).getJdbcType()
				instanceof JavaTimeJdbcType ) ) {
			basicTypeRegistry.register(
					new NamedBasicTypeImpl<>(
							javaTypeRegistry.resolveDescriptor( ZonedDateTime.class ),
							timestampWithTimeZoneOverride,
							"ZonedDateTime"
					),
					"org.hibernate.type.ZonedDateTimeType",
					ZonedDateTime.class.getSimpleName(),
					ZonedDateTime.class.getName()
			);
		}
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
			jdbcTypeRegistry.addDescriptor( typeCode,
					jdbcTypeRegistry.getDescriptor( fallbackTypeCode ) );
		}
	}
}
