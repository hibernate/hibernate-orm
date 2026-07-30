/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.annotations.TargetEmbeddable;
import org.hibernate.boot.mapping.internal.context.RootMappingDefaults;
import org.hibernate.boot.pipeline.internal.source.PreparedMappingSources;
import org.hibernate.boot.mapping.internal.xml.XmlProcessor;
import org.hibernate.boot.mapping.internal.xml.XmlPreProcessor;
import org.hibernate.boot.mapping.internal.xml.XmlPreProcessingResult;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Transient;

import static org.hibernate.boot.mapping.internal.categorize.EntityHierarchyBuilder.createEntityHierarchies;

/// Processes {@linkplain PreparedMappingSources resolved mapping sources} and produces a
/// {@linkplain CategorizedDomainModelImpl categorized domain model}.
///
/// XML mappings are pre-processed first so they can contribute managed class names
/// and metadata-complete annotations.  The resulting visible persistent types are
/// then organized into managed-type inheritance state before entity hierarchies are
/// created.
///
/// This is the public entry point for the categorization phase.  It owns the
/// transition from collected sources to categorized contracts; later phases should
/// consume the resulting {@link CategorizedDomainModelImpl} rather than repeat source
/// collection.
///
/// @since 9.0
/// @author Steve Ebersole
public class DomainModelCategorizer {
	private DomainModelCategorizer() {
	}

	public static CategorizedDomainModelImpl categorize(
			PreparedMappingSources resolvedMappingSources,
			MetadataBuildingContext metadataBuildingContext) {
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// 	- pre-process the XML
		// 	- collect all known classes
		// 	- use the MetadataBuildingContext's ModelsContext
		//
		// INPUTS:
		//		- resolvedMappingSources
		//		- metadataBuildingContext
		//
		// OUTPUTS:
		//		- availableXmlMappings
		//		- allKnownClassNames (technically could be included in xmlPreProcessingResult)
		//		- modelsContext

		final var persistenceUnitMetadata = metadataBuildingContext.getMetadataCollector().getPersistenceUnitMetadata();
		final XmlPreProcessingResult xmlPreProcessingResult = XmlPreProcessor.preProcessXmlResources(
				resolvedMappingSources,
				persistenceUnitMetadata
		);

		final List<String> allKnownClassNames = new ArrayList<>( xmlPreProcessingResult.getMappedClasses() );
		resolvedMappingSources.managedClassDetails().forEach( (classDetails) -> allKnownClassNames.add( classDetails.getName() ) );
		resolvedMappingSources.packageDetails().forEach( (packageDetails) -> allKnownClassNames.add( packageDetails.getName() ) );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// 	- process metadata-complete XML
		//	- collect overlay XML
		//	- process annotations (including those from metadata-complete XML)
		//	- apply overlay XML
		//
		// INPUTS:
		//		- options
		//		- xmlPreProcessingResult
		//		- sourceModelBuildingContext
		//
		// OUTPUTS
		//		- rootEntities
		//		- mappedSuperClasses
		//  	- embeddables

		final ModelsContext modelsContext = metadataBuildingContext.getModelsContext();
		final ClassDetailsRegistry mutableClassDetailsRegistry = modelsContext.getClassDetailsRegistry();
		final DomainModelCategorizationCollector modelCategorizationCollector = new DomainModelCategorizationCollector(
				modelsContext,
				metadataBuildingContext.getMetadataCollector().getDatabase().getDialect(),
				(strategy) -> org.hibernate.boot.model.internal.GeneratorStrategies.resolveGeneratorClass(
						strategy,
						metadataBuildingContext
				)
		);

		final RootMappingDefaults mappingDefaults = rootMappingDefaults( metadataBuildingContext );
		final org.hibernate.boot.mapping.internal.xml.XmlProcessingResult xmlProcessingResult = XmlProcessor.processXml(
				xmlPreProcessingResult,
				persistenceUnitMetadata,
				modelCategorizationCollector::apply,
				modelsContext,
				metadataBuildingContext,
				mappingDefaults
		);

		xmlProcessingResult.apply();

		allKnownClassNames.forEach( (className) -> {
			final ClassDetails classDetails = mutableClassDetailsRegistry.resolveClassDetails( className );
			modelCategorizationCollector.apply( classDetails );
		} );
		xmlPreProcessingResult.getMappedNames().forEach( (className) -> {
			final ClassDetails classDetails = mutableClassDetailsRegistry.resolveClassDetails( className );
			modelCategorizationCollector.apply( classDetails );
		} );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		//	- create entity-hierarchies
		//	- create the CategorizedDomainModelImpl
		//
		// INPUTS:
		//		- rootEntities
		//		- mappedSuperClasses
		//  	- embeddables
		//
		// OUTPUTS:
		//		- CategorizedDomainModelImpl

		final ManagedTypeInheritanceState inheritanceState = new ManagedTypeInheritanceState(
				modelCategorizationCollector.getSourcePersistentTypes(),
				resolvedMappingSources.includeUnlistedStructuralTypes()
						? ManagedTypeInheritanceState.MissingPersistentSuperclassHandling.INCLUDE
						: ManagedTypeInheritanceState.MissingPersistentSuperclassHandling.EXCEPTION
		);
		inheritanceState.getMappedSuperclasses().forEach(
				modelCategorizationCollector::applyDiscoveredMappedSuperclass
		);
		discoverReachableEmbeddables(
				modelCategorizationCollector,
				resolvedMappingSources.includeUnlistedStructuralTypes()
		);
		final Map<String, EmbeddableTypeMetadataImpl> embeddableMetadata =
				createEmbeddableMetadata( modelCategorizationCollector.getEmbeddables() );
		// Collect the entity hierarchies based on the scoped managed type inheritance state
		final CategorizationContextImpl mappingBuildingContext = new CategorizationContextImpl(
				persistenceUnitMetadata,
				mappingDefaults,
				mutableClassDetailsRegistry,
				modelsContext,
				embeddableMetadata,
				metadataBuildingContext.getBuildingPlan().getSharedCacheMode(),
				metadataBuildingContext.getBuildingPlan().getImplicitCacheAccessType(),
				modelCategorizationCollector.getGlobalRegistrations(),
				metadataBuildingContext.getMetadataCollector().getConverterRegistry(),
				metadataBuildingContext.getMetadataCollector().getDatabase()
		);
		metadataBuildingContext.getMetadataCollector()
				.getIdentifierGeneratorRegistrations()
				.values()
				.forEach( modelCategorizationCollector.getGlobalRegistrations()::collectIdentifierGenerator );
		final Set<EntityHierarchyImpl> entityHierarchies = createEntityHierarchies(
				inheritanceState,
				mappingBuildingContext
		);

		return modelCategorizationCollector.createResult( entityHierarchies, embeddableMetadata );
	}

	private static Map<String, EmbeddableTypeMetadataImpl> createEmbeddableMetadata(
			Map<String, ClassDetails> embeddableClasses) {
		final Map<String, EmbeddableTypeMetadataImpl> result = new LinkedHashMap<>( embeddableClasses.size() );
		embeddableClasses.forEach(
				(name, classDetails) -> result.put( name, new EmbeddableTypeMetadataImpl( classDetails ) )
		);
		return Map.copyOf( result );
	}

	private static void discoverReachableEmbeddables(
			DomainModelCategorizationCollector modelCategorizationCollector,
			boolean includeUnlistedStructuralTypes) {
		final Set<String> processedTypeNames = new LinkedHashSet<>();
		final List<ClassDetails> typesToProcess = new ArrayList<>();
		typesToProcess.addAll( modelCategorizationCollector.getSourcePersistentTypes() );
		typesToProcess.addAll( modelCategorizationCollector.getEmbeddables().values() );

		for ( int i = 0; i < typesToProcess.size(); i++ ) {
			final ClassDetails typeToProcess = typesToProcess.get( i );
			if ( !processedTypeNames.add( typeKey( typeToProcess ) ) ) {
				continue;
			}
			discoverReachableEmbeddables(
					typeToProcess,
					modelCategorizationCollector,
					includeUnlistedStructuralTypes,
					typesToProcess
			);
		}
	}

	private static void discoverReachableEmbeddables(
			ClassDetails declaringType,
			DomainModelCategorizationCollector modelCategorizationCollector,
			boolean includeUnlistedStructuralTypes,
			List<ClassDetails> typesToProcess) {
		for ( ClassDetails memberDeclaringType = declaringType;
				memberDeclaringType != null && memberDeclaringType != ClassDetails.OBJECT_CLASS_DETAILS;
				memberDeclaringType = memberDeclaringType.getSuperClass() ) {
			discoverReachableEmbeddables(
					declaringType,
					memberDeclaringType,
					modelCategorizationCollector,
					includeUnlistedStructuralTypes,
					typesToProcess
			);
		}
	}

	private static void discoverReachableEmbeddables(
			ClassDetails rootDeclaringType,
			ClassDetails memberDeclaringType,
			DomainModelCategorizationCollector modelCategorizationCollector,
			boolean includeUnlistedStructuralTypes,
			List<ClassDetails> typesToProcess) {
		for ( MemberDetails member : persistentMembers( memberDeclaringType ) ) {
			if ( member.isPlural() ) {
				collectReachableEmbeddable(
						rootDeclaringType,
						member,
						determineRelativeType( member.getElementType(), rootDeclaringType ),
						true,
						modelCategorizationCollector,
						includeUnlistedStructuralTypes,
						typesToProcess
				);
				collectReachableEmbeddable(
						rootDeclaringType,
						member,
						determineRelativeType( member.getMapKeyType(), rootDeclaringType ),
						false,
						modelCategorizationCollector,
						includeUnlistedStructuralTypes,
						typesToProcess
				);
			}
			else {
				collectReachableEmbeddable(
						rootDeclaringType,
						member,
						member.getType().determineRelativeType( rootDeclaringType ),
						true,
						modelCategorizationCollector,
						includeUnlistedStructuralTypes,
						typesToProcess
				);
			}
		}
	}

	private static Collection<MemberDetails> persistentMembers(ClassDetails declaringType) {
		final List<MemberDetails> persistentMembers = new ArrayList<>();
		declaringType.getFields().forEach( (field) -> {
			if ( field.isPersistable() && !field.hasDirectAnnotationUsage( Transient.class ) ) {
				persistentMembers.add( field );
			}
		} );
		declaringType.getMethods().forEach( (method) -> {
			if ( method.isPersistable() && !method.hasDirectAnnotationUsage( Transient.class ) ) {
				persistentMembers.add( method );
			}
		} );
		return persistentMembers;
	}

	private static TypeDetails determineRelativeType(TypeDetails typeDetails, ClassDetails declaringType) {
		return typeDetails == null ? null : typeDetails.determineRelativeType( declaringType );
	}

	private static void collectReachableEmbeddable(
			ClassDetails declaringType,
			MemberDetails member,
			TypeDetails memberType,
			boolean applyMemberEmbeddingAnnotations,
			DomainModelCategorizationCollector modelCategorizationCollector,
			boolean includeUnlistedStructuralTypes,
			List<ClassDetails> typesToProcess) {
		if ( memberType == null ) {
			return;
		}
		final TargetEmbeddable targetEmbeddable = applyMemberEmbeddingAnnotations
				? member.getDirectAnnotationUsage( TargetEmbeddable.class )
				: null;
		final ClassDetails embeddableType = targetEmbeddable == null
				? memberType.determineRawClass()
				: modelCategorizationCollector.resolveClassDetails( targetEmbeddable.value().getName() );
		final boolean explicitEmbedded = applyMemberEmbeddingAnnotations
				&& ( member.hasDirectAnnotationUsage( Embedded.class )
						|| member.hasDirectAnnotationUsage( EmbeddedId.class )
						|| targetEmbeddable != null );
		if ( embeddableType == null
				|| ( !explicitEmbedded && !embeddableType.hasDirectAnnotationUsage( Embeddable.class ) )
				|| modelCategorizationCollector.getEmbeddables().containsKey( embeddableType.getClassName() ) ) {
			return;
		}

		if ( !includeUnlistedStructuralTypes ) {
			throw new MappingException(
					"Embeddable `%s` referenced by `%s#%s` was not included in PreparedMappingSources".formatted(
							embeddableType.getName(),
							declaringType.getName(),
							member.resolveAttributeName()
					)
			);
		}

		modelCategorizationCollector.applyDiscoveredEmbeddable( embeddableType );
		typesToProcess.add( embeddableType );
	}

	private static String typeKey(ClassDetails classDetails) {
		final String className = classDetails.getClassName();
		return className == null ? classDetails.getName() : className;
	}

	private static RootMappingDefaults rootMappingDefaults(MetadataBuildingContext metadataBuildingContext) {
		if ( metadataBuildingContext.getEffectiveDefaults() instanceof RootMappingDefaults rootMappingDefaults ) {
			return rootMappingDefaults;
		}

		return new RootMappingDefaults(
				metadataBuildingContext.getBuildingPlan().getMappingDefaults(),
				metadataBuildingContext.getMetadataCollector().getPersistenceUnitMetadata()
		);
	}

}
