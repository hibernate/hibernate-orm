/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.annotations;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.boot.internal.MetadataBuildingContextRootImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.model.convert.internal.ConverterDescriptors;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.model.internal.AnnotationBinder;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.spi.MetadataSourceProcessor;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.JpaOrmXmlPersistenceUnitDefaultAware;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.models.spi.ClassDetails;

import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;

import static org.hibernate.boot.model.internal.AnnotationBinder.bindClass;
import static org.hibernate.boot.model.internal.AnnotationBinder.bindDefaults;
import static org.hibernate.boot.model.internal.AnnotationBinder.bindFetchProfilesForClass;
import static org.hibernate.boot.model.internal.AnnotationBinder.bindFetchProfilesForPackage;
import static org.hibernate.boot.model.internal.AnnotationBinder.buildInheritanceStates;
import static org.hibernate.boot.model.process.spi.MetadataBuildingProcess.processManagedResources;

/**
 * @author Steve Ebersole
 */
public class AnnotationMetadataSourceProcessorImpl implements MetadataSourceProcessor {

	// NOTE: we decategorize the classes into a single collection (xClasses)
	// 		  here to work with the existing "binder" infrastructure.
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

		final var bootstrapContext = rootMetadataBuildingContext.getBootstrapContext();

		classLoaderService = bootstrapContext.getClassLoaderService();
		assert classLoaderService != null;

		final var converterRegistry =
				rootMetadataBuildingContext.getMetadataCollector().getConverterRegistry();
		domainModelSource.getConversionRegistrations().forEach( (registration) -> {
			final var explicitDomainType = registration.getExplicitDomainType();
			converterRegistry.addRegisteredConversion( new RegisteredConversion(
					explicitDomainType == void.class || explicitDomainType == Void.class
							? void.class
							: explicitDomainType,
					registration.getConverterType(),
					registration.isAutoApply(),
					rootMetadataBuildingContext
			) );
		} );
		domainModelSource.getConverterRegistrations().forEach( (registration) -> {
			converterRegistry.addAttributeConverter( ConverterDescriptors.of(
					classLoaderService.classForName( registration.converterClass().getClassName() ),
					registration.autoApply(), false,
					bootstrapContext.getClassmateContext()
			) );
		} );

		applyManagedClasses( domainModelSource, knownClasses );

		final var classDetailsRegistry = domainModelSource.getClassDetailsRegistry();
		for ( String className : managedResources.getAnnotatedClassNames() ) {
			knownClasses.add( classDetailsRegistry.resolveClassDetails( className ) );
		}
		for ( var annotatedClass : managedResources.getAnnotatedClassReferences() ) {
			knownClasses.add( classDetailsRegistry.resolveClassDetails( annotatedClass.getName() ) );
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
		final var builder =
				new AdditionalManagedResourcesImpl.Builder(
						rootMetadataBuildingContext.getBootstrapContext().getServiceRegistry() );
		builder.addLoadedClasses( additionalClasses );
		builder.addClassDetails( additionalClassDetails );
		builder.addJaxbEntityMappings( additionalJaxbMappings );

		final var managedResources = builder.build();
		final var additionalDomainModelSource =
				processManagedResources( managedResources,
						rootMetadataBuildingContext.getMetadataCollector(),
						rootMetadataBuildingContext.getBootstrapContext(),
						options.getMappingDefaults() );
		new AnnotationMetadataSourceProcessorImpl( managedResources, additionalDomainModelSource, rootMetadataBuildingContext )
				.processEntityHierarchies( new LinkedHashSet<>() );
	}

	@Override
	public void prepare() {
		// use any persistence-unit-defaults defined in orm.xml
		( (JpaOrmXmlPersistenceUnitDefaultAware) rootMetadataBuildingContext.getBuildingOptions() )
				.apply( domainModelSource.getPersistenceUnitMetadata() );

		final var defaults = rootMetadataBuildingContext.getBuildingOptions().getMappingDefaults();
		rootMetadataBuildingContext.getMetadataCollector().getDatabase()
				.adjustDefaultNamespace( defaults.getImplicitCatalogName(), defaults.getImplicitSchemaName() );

		bindDefaults( rootMetadataBuildingContext );
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
		final var collector = rootMetadataBuildingContext.getMetadataCollector();
		for ( var registration : domainModelSource.getGlobalRegistrations().getFilterDefRegistrations().values() ) {
			collector.addFilterDefinition( registration.toFilterDefinition( rootMetadataBuildingContext ) );
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
		final var orderedClasses = orderAndFillHierarchy( knownClasses );
		final var inheritanceStatePerClass =
				buildInheritanceStates( orderedClasses, rootMetadataBuildingContext );

		for ( var clazz : orderedClasses ) {
			if ( !processedEntityNames.contains( clazz.getName() )
					&& !clazz.getName().endsWith( ".package-info" ) ) {
				bindClass( clazz, inheritanceStatePerClass, rootMetadataBuildingContext );
				bindFetchProfilesForClass( clazz, rootMetadataBuildingContext );
				processedEntityNames.add( clazz.getName() );
			}
		}
	}

	private List<ClassDetails> orderAndFillHierarchy(LinkedHashSet<ClassDetails> original) {
		final LinkedHashSet<ClassDetails> copy = new LinkedHashSet<>( original.size() );
		insertMappedSuperclasses( original, copy );
		// order the hierarchy
		final List<ClassDetails> workingCopy = new ArrayList<>( copy );
		final List<ClassDetails> newList = new ArrayList<>( copy.size() );
		while ( !workingCopy.isEmpty() ) {
			final var clazz = workingCopy.get( 0 );
			orderHierarchy( workingCopy, newList, copy, clazz );
		}
		return newList;
	}

	private void insertMappedSuperclasses(LinkedHashSet<ClassDetails> original, LinkedHashSet<ClassDetails> copy) {
		for ( var clazz : original ) {
			if ( clazz.isInterface() && clazz.hasDirectAnnotationUsage( Entity.class ) ) {
				throw new MappingException( "Interface '" + clazz.getName() + "' may not be annotated '@Entity'" );
			}
			else if ( clazz.hasDirectAnnotationUsage( MappedSuperclass.class ) ) {
				// Skip class with explicit @MappedSuperclass annotation
				// the class will be discovered analyzing the implementing class
			}
			else {
				copy.add( clazz );
				ClassDetails superClass = clazz.getSuperClass();
				while ( superClass != null
						&& !Object.class.getName().equals( superClass.getName() )
						&& !copy.contains( superClass ) ) {
					if ( superClass.hasDirectAnnotationUsage( Entity.class )
							|| superClass.hasDirectAnnotationUsage( MappedSuperclass.class ) ) {
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
			bindFetchProfilesForPackage( annotatedPackage, rootMetadataBuildingContext );
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
			LinkedHashSet<ClassDetails> knownClasses) {
		final var classDetailsRegistry = domainModelSource.getClassDetailsRegistry();
		domainModelSource.getManagedClassNames()
				.forEach( className -> knownClasses.add( classDetailsRegistry.resolveClassDetails( className ) ) );
	}
}
