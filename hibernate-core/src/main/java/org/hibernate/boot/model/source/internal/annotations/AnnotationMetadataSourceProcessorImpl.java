/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.source.internal.annotations;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.boot.internal.MetadataBuildingContextRootImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterRegistry;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.model.internal.AnnotationBinder;
import org.hibernate.boot.model.internal.InheritanceState;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.process.spi.MetadataBuildingProcess;
import org.hibernate.boot.model.source.spi.MetadataSourceProcessor;
import org.hibernate.boot.models.spi.FilterDefRegistration;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.JpaOrmXmlPersistenceUnitDefaultAware;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.jboss.logging.Logger;

import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Steve Ebersole
 */
public class AnnotationMetadataSourceProcessorImpl implements MetadataSourceProcessor {
	private static final Logger log = Logger.getLogger( AnnotationMetadataSourceProcessorImpl.class );

	// NOTE : we de-categorize the classes into a single collection (xClasses) here to work with the
	// 		existing "binder" infrastructure.
	// todo : once we move to the phased binding approach, come back and handle that

	private final DomainModelSource domainModelSource;

	private final MetadataBuildingContextRootImpl rootMetadataBuildingContext;
	private final ClassLoaderService classLoaderService;

	private final LinkedHashSet<String> annotatedPackages = new LinkedHashSet<>();
	private final LinkedHashSet<ClassDetails> knownClasses = new LinkedHashSet<>();

	/**
	 * Normal constructor used while processing {@linkplain org.hibernate.boot.MetadataSources mapping sources}
	 */
	public AnnotationMetadataSourceProcessorImpl(
			ManagedResources managedResources,
			DomainModelSource domainModelSource,
			MetadataBuildingContextRootImpl rootMetadataBuildingContext) {
		this.domainModelSource = domainModelSource;
		this.rootMetadataBuildingContext = rootMetadataBuildingContext;

		final MetadataBuildingOptions metadataBuildingOptions = rootMetadataBuildingContext.getBuildingOptions();
		this.classLoaderService = metadataBuildingOptions.getServiceRegistry().getService( ClassLoaderService.class );
		assert classLoaderService != null;

		final ConverterRegistry converterRegistry = rootMetadataBuildingContext.getMetadataCollector().getConverterRegistry();
		domainModelSource.getConversionRegistrations().forEach( (registration) -> {
			final Class<?> domainType;
			if ( registration.getExplicitDomainType() == void.class
					|| registration.getExplicitDomainType() == Void.class ) {
				domainType = void.class;
			}
			else {
				domainType = registration.getExplicitDomainType();
			}
			converterRegistry.addRegisteredConversion( new RegisteredConversion(
					domainType,
					registration.getConverterType(),
					registration.isAutoApply(),
					rootMetadataBuildingContext
			) );
		} );
		domainModelSource.getConverterRegistrations().forEach( (registration) -> {
			converterRegistry.addAttributeConverter( new ClassBasedConverterDescriptor(
					classLoaderService.classForName( registration.converterClass().getClassName() ),
					registration.autoApply(),
					rootMetadataBuildingContext.getBootstrapContext().getClassmateContext()
			) );
		} );

		applyManagedClasses( domainModelSource, knownClasses, rootMetadataBuildingContext );

		for ( String className : managedResources.getAnnotatedClassNames() ) {
			knownClasses.add( domainModelSource.getClassDetailsRegistry().resolveClassDetails( className ) );
		}

		for ( Class<?> annotatedClass : managedResources.getAnnotatedClassReferences() ) {
			knownClasses.add( domainModelSource.getClassDetailsRegistry().resolveClassDetails( annotatedClass.getName() ) );
		}

		annotatedPackages.addAll( managedResources.getAnnotatedPackageNames() );
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
		final AdditionalManagedResourcesImpl.Builder mrBuilder = new AdditionalManagedResourcesImpl.Builder();
		mrBuilder.addLoadedClasses( additionalClasses );
		mrBuilder.addClassDetails( additionalClassDetails );
		mrBuilder.addJaxbEntityMappings( additionalJaxbMappings );

		final ManagedResources mr = mrBuilder.build();
		final DomainModelSource additionalDomainModelSource = MetadataBuildingProcess.processManagedResources(
				mr,
				rootMetadataBuildingContext.getMetadataCollector(),
				rootMetadataBuildingContext.getBootstrapContext(),
				options.getMappingDefaults()
		);
		final AnnotationMetadataSourceProcessorImpl processor = new AnnotationMetadataSourceProcessorImpl( mr, additionalDomainModelSource, rootMetadataBuildingContext );
		processor.processEntityHierarchies( new LinkedHashSet<>() );
	}

	@Override
	public void prepare() {
		// use any persistence-unit-defaults defined in orm.xml
		( (JpaOrmXmlPersistenceUnitDefaultAware) rootMetadataBuildingContext.getBuildingOptions() ).apply( domainModelSource.getPersistenceUnitMetadata() );

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
		final Map<String, FilterDefRegistration> filterDefRegistrations = domainModelSource.getGlobalRegistrations().getFilterDefRegistrations();
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

		LinkedHashSet<XClass> orderedClasses = CollectionHelper.linkedSetOfSize( classes.size() * 2 );
		List<XClass> clazzHierarchy = new ArrayList<>();

		for ( ClassDetails clazz : classes ) {
			if ( clazz.isInterface() ) {
				if ( clazz.hasDirectAnnotationUsage( Entity.class ) ) {
					throw new MappingException( "Only classes (not interfaces) may be mapped as @Entity : " + clazz.getName() );
				}
			}
			if ( clazz.hasDirectAnnotationUsage( MappedSuperclass.class ) ) {
				if ( debug ) {
					log.debugf(
							"Skipping explicit MappedSuperclass %s, the class will be discovered analyzing the implementing class",
							clazz
					);
				}
			}
			else {
				if ( orderedClasses.contains( clazz ) ) {
					continue;
				}

				clazzHierarchy.clear();
				clazzHierarchy.add( clazz );

				ClassDetails superClass = clazz.getSuperClass();
				while ( superClass != null
						&& !Object.class.getName().equals( superClass.getName() ) ) {
					if ( superClass.hasDirectAnnotationUsage( Entity.class )
							|| superClass.hasDirectAnnotationUsage( MappedSuperclass.class ) ) {
						if ( orderedClasses.contains( superClass ) ) {
							break;
						}
						clazzHierarchy.add( superClass );
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
			}
		}
		return new ArrayList<>( orderedClasses );
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

	private static void applyManagedClasses(
			DomainModelSource domainModelSource,
			LinkedHashSet<ClassDetails> knownClasses,
			MetadataBuildingContextRootImpl rootMetadataBuildingContext) {
		final ClassDetailsRegistry classDetailsRegistry = domainModelSource.getClassDetailsRegistry();
		domainModelSource.getManagedClassNames().forEach( (className) -> {
			knownClasses.add( classDetailsRegistry.resolveClassDetails( className ) );
		} );
	}
}
