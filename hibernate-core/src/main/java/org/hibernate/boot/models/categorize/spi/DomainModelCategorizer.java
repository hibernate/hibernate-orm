/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.internal.RootMappingDefaults;
import org.hibernate.boot.models.AvailableResources;
import org.hibernate.boot.models.categorize.internal.DomainModelCategorizationCollector;
import org.hibernate.boot.models.categorize.internal.ManagedTypeInheritanceState;
import org.hibernate.boot.models.categorize.internal.CategorizationContextImpl;
import org.hibernate.boot.models.xml.spi.XmlProcessor;
import org.hibernate.boot.models.xml.spi.XmlPreProcessor;
import org.hibernate.boot.models.xml.spi.XmlPreProcessingResult;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.categorize.internal.EntityHierarchyBuilder.createEntityHierarchies;

/// Processes {@linkplain AvailableResources available resources} and produces a
/// {@linkplain CategorizedDomainModel categorized domain model}.
///
/// XML mappings are pre-processed first so they can contribute managed class names
/// and metadata-complete annotations.  The resulting visible persistent types are
/// then organized into managed-type inheritance state before entity hierarchies are
/// created.
///
/// This is the public entry point for the categorization phase.  It owns the
/// transition from collected sources to categorized contracts; later phases should
/// consume the resulting {@link CategorizedDomainModel} rather than repeat source
/// collection.
///
/// @since 9.0
/// @author Steve Ebersole
public class DomainModelCategorizer {
	private DomainModelCategorizer() {
	}

	public static CategorizedDomainModel categorize(
			AvailableResources availableResources,
			MetadataBuildingContext metadataBuildingContext) {
		final var bootstrapContext = metadataBuildingContext.getBootstrapContext();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// 	- pre-process the XML
		// 	- collect all known classes
		// 	- use the BootstrapContext's ModelsContext
		//
		// INPUTS:
		//		- availableResources
		//		- bootstrapContext
		//
		// OUTPUTS:
		//		- availableXmlMappings
		//		- allKnownClassNames (technically could be included in xmlPreProcessingResult)
		//		- modelsContext

		final var persistenceUnitMetadata = metadataBuildingContext.getMetadataCollector().getPersistenceUnitMetadata();
		final XmlPreProcessingResult xmlPreProcessingResult = XmlPreProcessor.preProcessXmlResources(
				availableResources,
				persistenceUnitMetadata
		);

		final List<String> allKnownClassNames = new ArrayList<>( xmlPreProcessingResult.getMappedClasses() );
		availableResources.managedClassDetails().forEach( (classDetails) -> allKnownClassNames.add( classDetails.getName() ) );
		availableResources.packageDetails().forEach( (packageDetails) -> allKnownClassNames.add( packageDetails.getName() ) );


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
		final ModelsContext modelsContext = bootstrapContext.getModelsContext();
		final ClassDetailsRegistry mutableClassDetailsRegistry = modelsContext.getClassDetailsRegistry();
		final DomainModelCategorizationCollector modelCategorizationCollector = new DomainModelCategorizationCollector(
				areIdGeneratorsGlobal,
				modelsContext
		);

		final RootMappingDefaults mappingDefaults = rootMappingDefaults( metadataBuildingContext );
		final org.hibernate.boot.models.xml.spi.XmlProcessingResult xmlProcessingResult = XmlProcessor.processXml(
				xmlPreProcessingResult,
				persistenceUnitMetadata,
				modelCategorizationCollector::apply,
				modelsContext,
				bootstrapContext,
				mappingDefaults
		);

		allKnownClassNames.forEach( (className) -> {
			final ClassDetails classDetails = mutableClassDetailsRegistry.resolveClassDetails( className );
			modelCategorizationCollector.apply( classDetails );
		} );
		xmlPreProcessingResult.getMappedNames().forEach( (className) -> {
			final ClassDetails classDetails = mutableClassDetailsRegistry.resolveClassDetails( className );
			modelCategorizationCollector.apply( classDetails );
		} );

		xmlProcessingResult.apply();


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		//	- create entity-hierarchies
		//	- create the CategorizedDomainModel
		//
		// INPUTS:
		//		- rootEntities
		//		- mappedSuperClasses
		//  	- embeddables
		//
		// OUTPUTS:
		//		- CategorizedDomainModel

		// Collect the entity hierarchies based on the scoped managed type inheritance state
		final CategorizationContextImpl mappingBuildingContext = new CategorizationContextImpl(
				persistenceUnitMetadata,
				mappingDefaults,
				mutableClassDetailsRegistry,
				bootstrapContext.getMetadataBuildingOptions().getSharedCacheMode(),
				modelCategorizationCollector.getGlobalRegistrations(),
				metadataBuildingContext.getMetadataCollector().getConverterRegistry(),
				metadataBuildingContext.getMetadataCollector().getDatabase()
		);

		final ManagedTypeInheritanceState inheritanceState = new ManagedTypeInheritanceState(
				modelCategorizationCollector.getSourcePersistentTypes()
		);
		final Set<EntityHierarchy> entityHierarchies = createEntityHierarchies(
				inheritanceState,
				mappingBuildingContext
		);

		return modelCategorizationCollector.createResult( entityHierarchies );
	}

	private static RootMappingDefaults rootMappingDefaults(MetadataBuildingContext metadataBuildingContext) {
		if ( metadataBuildingContext.getEffectiveDefaults() instanceof RootMappingDefaults rootMappingDefaults ) {
			return rootMappingDefaults;
		}

		return new RootMappingDefaults(
				metadataBuildingContext.getBuildingOptions().getMappingDefaults(),
				metadataBuildingContext.getMetadataCollector().getPersistenceUnitMetadata()
		);
	}

}
