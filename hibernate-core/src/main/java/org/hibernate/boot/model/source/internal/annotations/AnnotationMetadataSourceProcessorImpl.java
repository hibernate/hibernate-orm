/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.source.internal.annotations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.boot.internal.MetadataBuildingContextRootImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterRegistry;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.model.internal.AnnotationBinder;
import org.hibernate.boot.model.internal.InheritanceState;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.spi.MetadataSourceProcessor;
import org.hibernate.boot.models.categorize.internal.DomainModelCategorizationCollector;
import org.hibernate.boot.models.categorize.internal.EntityHierarchyBuilder;
import org.hibernate.boot.models.categorize.internal.ModelCategorizationContextImpl;
import org.hibernate.boot.models.categorize.spi.DomainModelCategorizations;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.FilterDefRegistration;
import org.hibernate.boot.models.categorize.spi.GlobalRegistrations;
import org.hibernate.boot.models.xml.spi.PersistenceUnitMetadata;
import org.hibernate.boot.models.xml.spi.XmlPreProcessingResult;
import org.hibernate.boot.models.xml.spi.XmlPreProcessor;
import org.hibernate.boot.models.xml.spi.XmlProcessingResult;
import org.hibernate.boot.models.xml.spi.XmlProcessor;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.JpaOrmXmlPersistenceUnitDefaultAware;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.logging.Logger;

import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;

import static org.hibernate.models.spi.ClassDetails.VOID_CLASS_DETAILS;
import static org.hibernate.models.spi.ClassDetails.VOID_OBJECT_CLASS_DETAILS;

/**
 * @author Steve Ebersole
 */
public class AnnotationMetadataSourceProcessorImpl implements MetadataSourceProcessor {
	private static final Logger log = Logger.getLogger( AnnotationMetadataSourceProcessorImpl.class );

	// NOTE : we de-categorize the classes into a single collection (xClasses) here to work with the
	// 		existing "binder" infrastructure.
	// todo : once we move to the phased binding approach, come back and handle that

	private final MetadataBuildingContextRootImpl rootMetadataBuildingContext;

	private final PersistenceUnitMetadata persistenceUnitMetadata;
	private final ClassLoaderService classLoaderService;

	private final DomainModelCategorizations domainModelCategorizations;

	private final LinkedHashSet<String> annotatedPackages = new LinkedHashSet<>();
	private final LinkedHashSet<ClassDetails> knownClasses = new LinkedHashSet<>();

	/**
	 * Normal constructor used while processing {@linkplain org.hibernate.boot.MetadataSources mapping sources}
	 */
	public AnnotationMetadataSourceProcessorImpl(
			ManagedResources managedResources,
			XmlPreProcessingResult xmlPreProcessingResult,
			MetadataBuildingContextRootImpl rootMetadataBuildingContext) {
		this.rootMetadataBuildingContext = rootMetadataBuildingContext;
		this.persistenceUnitMetadata = xmlPreProcessingResult.getPersistenceUnitMetadata();
		assert persistenceUnitMetadata != null;

		final MetadataBuildingOptions metadataBuildingOptions = rootMetadataBuildingContext.getBuildingOptions();
		final StandardServiceRegistry serviceRegistry = metadataBuildingOptions.getServiceRegistry();
		this.classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		assert classLoaderService != null;

		final InFlightMetadataCollector metadataCollector = rootMetadataBuildingContext.getMetadataCollector();
		final ConverterRegistry converterRegistry = metadataCollector.getConverterRegistry();
		final BootstrapContext bootstrapContext = rootMetadataBuildingContext.getBootstrapContext();
		final SourceModelBuildingContext sourceModelBuildingContext = metadataCollector.getSourceModelBuildingContext();
		final ClassDetailsRegistry classDetailsRegistry = sourceModelBuildingContext.getClassDetailsRegistry();


		final GlobalRegistrations globalRegistrations = rootMetadataBuildingContext.getMetadataCollector().getGlobalRegistrations();

		// JPA id generator global-ity thing
		final boolean areIdGeneratorsGlobal = true;
		final DomainModelCategorizationCollector modelCategorizationCollector = new DomainModelCategorizationCollector(
				areIdGeneratorsGlobal,
				metadataCollector.getGlobalRegistrations(),
				sourceModelBuildingContext
		);
		domainModelCategorizations = modelCategorizationCollector;

		final XmlProcessingResult xmlProcessingResult = XmlProcessor.processXml(
				xmlPreProcessingResult,
				modelCategorizationCollector,
				sourceModelBuildingContext,
				bootstrapContext,
				rootMetadataBuildingContext.getEffectiveDefaults()
		);
		xmlProcessingResult.apply( xmlPreProcessingResult.getPersistenceUnitMetadata() );

		final HashSet<String> categorizedClassNames = new HashSet<>();
		forEachKnownClassName(
				managedResources,
				xmlPreProcessingResult,
				sourceModelBuildingContext,
				(className) -> applyKnownClass(
						className,
						categorizedClassNames,
						knownClasses,
						classDetailsRegistry,
						modelCategorizationCollector
				)
		);
		xmlPreProcessingResult.getMappedNames().forEach( (className) -> applyKnownClass(
				className,
				categorizedClassNames,
				knownClasses,
				classDetailsRegistry,
				modelCategorizationCollector
		) );

		globalRegistrations.getConverterRegistrations().forEach( (registration) -> {
			final Class<?> domainType;
			if ( registration.getExplicitDomainType() == VOID_CLASS_DETAILS || registration.getExplicitDomainType() == VOID_OBJECT_CLASS_DETAILS ) {
				domainType = void.class;
			}
			else {
				domainType = classLoaderService.classForName( registration.getExplicitDomainType().getClassName() );
			}
			converterRegistry.addRegisteredConversion( new RegisteredConversion(
					domainType,
					classLoaderService.classForName( registration.getConverterType().getClassName() ),
					registration.isAutoApply(),
					rootMetadataBuildingContext
			) );
		} );
		globalRegistrations.getJpaConverters().forEach( (registration) -> {
			converterRegistry.addAttributeConverter( new ClassBasedConverterDescriptor(
					classLoaderService.classForName( registration.converterClass().getClassName() ),
					registration.autoApply(),
					bootstrapContext.getClassmateContext()
			) );
		} );

		annotatedPackages.addAll( managedResources.getAnnotatedPackageNames() );

	}

	public static void forEachKnownClassName(
			ManagedResources managedResources,
			XmlPreProcessingResult xmlPreProcessingResult,
			SourceModelBuildingContext context,
			Consumer<String> action) {
		managedResources.getAnnotatedPackageNames().forEach( (packageName) -> {
			try {
				context.getClassLoading().classForName( packageName + ".package-info" );
				action.accept( packageName + ".package-info" );
			}
			catch (ClassLoadingException ignore) {
			}
		} );
		managedResources.getAnnotatedClassReferences().forEach( (classReference) -> action.accept( classReference.getName() ) );
		managedResources.getAnnotatedClassNames().forEach( action );
		xmlPreProcessingResult.getMappedClasses().forEach( action );
	}

	private static void applyKnownClass(
			String className,
			HashSet<String> processedClassNames,
			LinkedHashSet<ClassDetails> knownClasses,
			ClassDetailsRegistry classDetailsRegistry,
			DomainModelCategorizationCollector modelCategorizationCollector) {
		if ( processedClassNames.add( className ) ) {
			final ClassDetails classDetails = classDetailsRegistry.resolveClassDetails( className );
			applyKnownClass( classDetails, processedClassNames, knownClasses, modelCategorizationCollector );
		}
	}

	private static void applyKnownClass(
			ClassDetails classDetails,
			HashSet<String> categorizedClassNames,
			LinkedHashSet<ClassDetails> knownClasses,
			DomainModelCategorizationCollector modelCategorizationCollector) {
		modelCategorizationCollector.apply( classDetails );
		knownClasses.add( classDetails );
		if ( classDetails.getSuperClass() != null
				&& classDetails.getSuperClass() != ClassDetails.OBJECT_CLASS_DETAILS ) {
			if ( categorizedClassNames.add( classDetails.getSuperClass().getClassName() ) ) {
				applyKnownClass( classDetails.getSuperClass(), categorizedClassNames, knownClasses, modelCategorizationCollector );
			}
		}
	}

	/**
	 * Used as part of processing
	 * {@linkplain org.hibernate.boot.spi.AdditionalMappingContributions#contributeEntity(Class) "additional" mappings}
	 */
	public static void processAdditionalMappings(
			List<Class<?>> additionalClasses,
			List<ClassDetails> additionalClassDetails,
			List<JaxbEntityMappingsImpl> additionalJaxbMappings,
			MetadataBuildingContextRootImpl rootMetadataBuildingContext,
			MetadataBuildingOptions options) {
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addLoadedClasses( additionalClasses )
				.addClassDetails( additionalClassDetails )
				.addJaxbEntityMappings( additionalJaxbMappings )
				.build();

		final XmlPreProcessingResult xmlPreProcessingResult = XmlPreProcessor.preProcessXmlResources(
				managedResources,
				rootMetadataBuildingContext.getMetadataCollector().getPersistenceUnitMetadata()
		);

		final AnnotationMetadataSourceProcessorImpl processor = new AnnotationMetadataSourceProcessorImpl(
				managedResources,
				xmlPreProcessingResult,
				rootMetadataBuildingContext
		);
		processor.processEntityHierarchies( new LinkedHashSet<>() );
	}

	@Override
	public void prepare() {
		// use any persistence-unit-defaults defined in orm.xml
		( (JpaOrmXmlPersistenceUnitDefaultAware) rootMetadataBuildingContext.getBuildingOptions() ).apply( persistenceUnitMetadata );

		rootMetadataBuildingContext.getMetadataCollector().getDatabase().adjustDefaultNamespace(
				rootMetadataBuildingContext.getBuildingOptions().getMappingDefaults().getImplicitCatalogName(),
				rootMetadataBuildingContext.getBuildingOptions().getMappingDefaults().getImplicitSchemaName()
		);

		AnnotationBinder.bindDefaults( rootMetadataBuildingContext );
		for ( String annotatedPackage : annotatedPackages ) {
			AnnotationBinder.bindPackage( classLoaderService, annotatedPackage, rootMetadataBuildingContext );
		}
	}

	@Override
	public void processTypeDefinitions() {
	}

	@Override
	public void processQueryRenames() {
	}

	@Override
	public void processNamedQueries() {
	}

	@Override
	public void processAuxiliaryDatabaseObjectDefinitions() {
	}

	@Override
	public void processIdentifierGenerators() {
	}

	@Override
	public void processFilterDefinitions() {
		final Map<String, FilterDefRegistration> filterDefRegistrations = rootMetadataBuildingContext.getMetadataCollector().getGlobalRegistrations().getFilterDefRegistrations();
		for ( Map.Entry<String, FilterDefRegistration> filterDefRegistrationEntry : filterDefRegistrations.entrySet() ) {
			final FilterDefRegistration filterDefRegistration = filterDefRegistrationEntry.getValue();
			final FilterDefinition filterDefinition = filterDefRegistration.toFilterDefinition( rootMetadataBuildingContext );
			rootMetadataBuildingContext.getMetadataCollector().addFilterDefinition( filterDefinition );
		}
	}

	@Override
	public void processFetchProfiles() {
	}

	@Override
	public void prepareForEntityHierarchyProcessing() {
	}

	@Override
	public void processEntityHierarchies(Set<String> processedEntityNames) {
		final InFlightMetadataCollector metadataCollector = rootMetadataBuildingContext.getMetadataCollector();
		final SourceModelBuildingContext sourceModelBuildingContext = metadataCollector.getSourceModelBuildingContext();

		final ModelCategorizationContextImpl modelCategorizationContext = new ModelCategorizationContextImpl(
				sourceModelBuildingContext.getClassDetailsRegistry().makeImmutableCopy(),
				sourceModelBuildingContext.getAnnotationDescriptorRegistry().makeImmutableCopy(),
				domainModelCategorizations.getGlobalRegistrations()
		);

		final Set<EntityHierarchy> entityHierarchies = EntityHierarchyBuilder.createEntityHierarchies(
				domainModelCategorizations.getRootEntities(),
				null,
				modelCategorizationContext
		);







		final List<ClassDetails> orderedClasses = orderAndFillHierarchy( knownClasses );
		Map<ClassDetails, InheritanceState> inheritanceStatePerClass = AnnotationBinder.buildInheritanceStates(
				orderedClasses,
				rootMetadataBuildingContext
		);

		for ( ClassDetails clazz : orderedClasses ) {
			if ( processedEntityNames.contains( clazz.getName() ) ) {
				log.debugf( "Skipping annotated class processing of entity [%s], as it has already been processed", clazz );
			}
			else {
				if ( clazz.getName().endsWith( ".package-info" ) ) {
					continue;
				}
				AnnotationBinder.bindClass( clazz, inheritanceStatePerClass, rootMetadataBuildingContext );
				AnnotationBinder.bindFetchProfilesForClass( clazz, rootMetadataBuildingContext );
				processedEntityNames.add( clazz.getName() );
			}
		}
	}

	private List<ClassDetails> orderAndFillHierarchy(LinkedHashSet<ClassDetails> original) {
		LinkedHashSet<ClassDetails> copy = new LinkedHashSet<>( original.size() );
		insertMappedSuperclasses( original, copy );

		// order the hierarchy
		List<ClassDetails> workingCopy = new ArrayList<>( copy );
		List<ClassDetails> newList = new ArrayList<>( copy.size() );
		while ( !workingCopy.isEmpty() ) {
			ClassDetails clazz = workingCopy.get( 0 );
			orderHierarchy( workingCopy, newList, copy, clazz );
		}
		return newList;
	}

	private void insertMappedSuperclasses(LinkedHashSet<ClassDetails> original, LinkedHashSet<ClassDetails> copy) {
		final boolean debug = log.isDebugEnabled();
		for ( ClassDetails clazz : original ) {
			if ( clazz.hasAnnotationUsage( MappedSuperclass.class ) ) {
				if ( debug ) {
					log.debugf(
							"Skipping explicit MappedSuperclass %s, the class will be discovered analyzing the implementing class",
							clazz
					);
				}
			}
			else {
				copy.add( clazz );
				ClassDetails superClass = clazz.getSuperClass();
				while ( superClass != null
						&& !Object.class.getName().equals( superClass.getName() )
						&& !copy.contains( superClass ) ) {
					if ( superClass.hasAnnotationUsage( Entity.class )
							|| superClass.hasAnnotationUsage( MappedSuperclass.class ) ) {
						copy.add( superClass );
					}
					superClass = superClass.getSuperClass();
				}
			}
		}
	}

	private void orderHierarchy(List<ClassDetails> copy, List<ClassDetails> newList, LinkedHashSet<ClassDetails> original, ClassDetails clazz) {
		if ( clazz != null && !Object.class.getName().equals( clazz.getName() ) ) {
			//process superclass first
			orderHierarchy( copy, newList, original, clazz.getSuperClass() );
			if ( original.contains( clazz ) ) {
				if ( !newList.contains( clazz ) ) {
					newList.add( clazz );
				}
				copy.remove( clazz );
			}
		}
	}

	@Override
	public void postProcessEntityHierarchies() {
		for ( String annotatedPackage : annotatedPackages ) {
			AnnotationBinder.bindFetchProfilesForPackage( classLoaderService, annotatedPackage, rootMetadataBuildingContext );
		}
	}

	@Override
	public void processResultSetMappings() {
	}

	@Override
	public void finishUp() {
	}
}
