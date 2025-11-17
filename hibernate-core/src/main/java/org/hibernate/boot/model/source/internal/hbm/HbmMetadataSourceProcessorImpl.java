/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.spi.MetadataSourceProcessor;
import org.hibernate.boot.spi.MetadataBuildingContext;


import static org.hibernate.boot.BootLogging.BOOT_LOGGER;

/**
 * MetadataSourceProcessor implementation for processing {@code hbm.xml} mapping documents.
 *
 * @author Steve Ebersole
 */
public class HbmMetadataSourceProcessorImpl implements MetadataSourceProcessor {

	private final Collection<MappingDocument> mappingDocuments;

	private final ModelBinder modelBinder;

	private final List<EntityHierarchySourceImpl> entityHierarchies;

	public HbmMetadataSourceProcessorImpl(
			ManagedResources managedResources,
			MetadataBuildingContext rootBuildingContext) {
		this( managedResources.getXmlMappingBindings(), rootBuildingContext );
	}

	public HbmMetadataSourceProcessorImpl(
			Collection<Binding<? extends JaxbBindableMappingDescriptor>> xmlBindings,
			MetadataBuildingContext rootBuildingContext) {
		final var hierarchyBuilder = new EntityHierarchyBuilder();

		mappingDocuments = new ArrayList<>();

		for ( var xmlBinding : xmlBindings ) {
			if ( xmlBinding.getRoot() instanceof JaxbHbmHibernateMapping hibernateMapping ) {
				final var mappingDocument = new MappingDocument(
						"orm",
						hibernateMapping,
						xmlBinding.getOrigin(),
						rootBuildingContext
				);
				mappingDocuments.add( mappingDocument );
				hierarchyBuilder.indexMappingDocument( mappingDocument );
			}
		}

		entityHierarchies = hierarchyBuilder.buildHierarchies();
		modelBinder = ModelBinder.prepare( rootBuildingContext );
	}

	@Override
	public void prepare() {
		for ( var mappingDocument : mappingDocuments ) {
			mappingDocument.prepare();
		}
	}

	@Override
	public void processTypeDefinitions() {
		for ( var mappingDocument : mappingDocuments ) {
			mappingDocument.processTypeDefinitions();
		}
	}

	@Override
	public void processQueryRenames() {
		for ( var mappingDocument : mappingDocuments ) {
			mappingDocument.processQueryRenames();
		}
	}

	@Override
	public void processNamedQueries() {
		for ( var mappingDocument : mappingDocuments ) {
			mappingDocument.processNamedQueries();
		}
	}

	@Override
	public void processAuxiliaryDatabaseObjectDefinitions() {
		for ( var mappingDocument : mappingDocuments ) {
			mappingDocument.processAuxiliaryDatabaseObjectDefinitions();
		}
	}

	@Override
	public void processFilterDefinitions() {
		for ( var mappingDocument : mappingDocuments ) {
			mappingDocument.processFilterDefinitions();
		}
	}

	@Override
	public void processFetchProfiles() {
		for ( var mappingDocument : mappingDocuments ) {
			mappingDocument.processFetchProfiles();
		}
	}

	@Override
	public void processIdentifierGenerators() {
		for ( var mappingDocument : mappingDocuments ) {
			mappingDocument.processIdentifierGenerators();
		}
	}

	@Override
	public void prepareForEntityHierarchyProcessing() {
	}

	@Override
	public void processEntityHierarchies(Set<String> processedEntityNames) {
		hierarchy_loop : for ( var entityHierarchy : entityHierarchies ) {
			for ( String entityName : entityHierarchy.getContainedEntityNames() ) {
				if ( processedEntityNames.contains( entityName ) ) {
					if ( BOOT_LOGGER.isDebugEnabled() ) {
						BOOT_LOGGER.skippingHbmProcessingOfEntityHierarchy(
								entityHierarchy.getRoot().getEntityNamingSource().getEntityName(),
								entityName
						);
					}
					continue hierarchy_loop;
				}
			}
			modelBinder.bindEntityHierarchy( entityHierarchy );
			processedEntityNames.addAll( entityHierarchy.getContainedEntityNames() );
		}
	}

	@Override
	public void postProcessEntityHierarchies() {}

	@Override
	public void processResultSetMappings() {
		for ( var mappingDocument : mappingDocuments ) {
			mappingDocument.processResultSetMappings();
		}
	}

	@Override
	public void finishUp() {
		for ( var mappingDocument : mappingDocuments ) {
			mappingDocument.finishUp();
		}
	}
}
