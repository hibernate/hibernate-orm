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
package org.hibernate.boot.model.source.internal.hbm;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.model.source.spi.MetadataSourceProcessor;
import org.hibernate.boot.spi.MetadataBuildingContext;

import org.jboss.logging.Logger;


/**
 * MetadataSourceProcessor implementation for processing {@code hbm.xml} mapping documents.
 *
 * @author Steve Ebersole
 */
public class HbmMetadataSourceProcessorImpl implements MetadataSourceProcessor {
	private static final Logger log = Logger.getLogger( HbmMetadataSourceProcessorImpl.class );

	private final MetadataBuildingContext rootBuildingContext;
	private List<MappingDocument> mappingDocuments;

	private final ModelBinder modelBinder;

	private List<EntityHierarchySourceImpl> entityHierarchies;

	public HbmMetadataSourceProcessorImpl(
			MetadataSources metadataSources,
			MetadataBuildingContext rootBuildingContext) {
		this(
				metadataSources.getXmlBindings(),
				rootBuildingContext
		);
	}

	@SuppressWarnings("unchecked")
	public HbmMetadataSourceProcessorImpl(
			List<Binding> xmlBindings,
			MetadataBuildingContext rootBuildingContext) {
		this.rootBuildingContext = rootBuildingContext;
		final EntityHierarchyBuilder hierarchyBuilder = new EntityHierarchyBuilder();

		this.mappingDocuments = new ArrayList<MappingDocument>();

		for ( Binding xmlBinding : xmlBindings ) {
			if ( !JaxbHbmHibernateMapping.class.isInstance( xmlBinding.getRoot() ) ) {
				continue;
			}

			final MappingDocument mappingDocument = new MappingDocument(
					(JaxbHbmHibernateMapping) xmlBinding.getRoot(),
					xmlBinding.getOrigin(),
					rootBuildingContext
			);
			mappingDocuments.add( mappingDocument );
			hierarchyBuilder.indexMappingDocument( mappingDocument );
		}

		entityHierarchies = hierarchyBuilder.buildHierarchies();
		modelBinder = ModelBinder.prepare( rootBuildingContext );
	}

	@Override
	public void prepare() {
		for ( MappingDocument mappingDocument : mappingDocuments ) {
			mappingDocument.prepare();
		}
	}

	@Override
	public void processTypeDefinitions() {
		for ( MappingDocument mappingDocument : mappingDocuments ) {
			mappingDocument.processTypeDefinitions();
		}
	}

	@Override
	public void processQueryRenames() {
		for ( MappingDocument mappingDocument : mappingDocuments ) {
			mappingDocument.processQueryRenames();
		}
	}

	@Override
	public void processNamedQueries() {
		for ( MappingDocument mappingDocument : mappingDocuments ) {
			mappingDocument.processNamedQueries();
		}
	}

	@Override
	public void processAuxiliaryDatabaseObjectDefinitions() {
		for ( MappingDocument mappingDocument : mappingDocuments ) {
			mappingDocument.processAuxiliaryDatabaseObjectDefinitions();
		}
	}

	@Override
	public void processFilterDefinitions() {
		for ( MappingDocument mappingDocument : mappingDocuments ) {
			mappingDocument.processFilterDefinitions();
		}
	}

	@Override
	public void processFetchProfiles() {
		for ( MappingDocument mappingDocument : mappingDocuments ) {
			mappingDocument.processFetchProfiles();
		}
	}

	@Override
	public void processIdentifierGenerators() {
		for ( MappingDocument mappingDocument : mappingDocuments ) {
			mappingDocument.processIdentifierGenerators();
		}
	}

	@Override
	public void prepareForEntityHierarchyProcessing() {
	}

	@Override
	public void processEntityHierarchies(Set<String> processedEntityNames) {
		hierarchy_loop : for ( EntityHierarchySourceImpl entityHierarchy : entityHierarchies ) {
			for ( String entityName : entityHierarchy.getContainedEntityNames() ) {
				if ( processedEntityNames.contains( entityName ) ) {
					log.debugf(
							"Skipping HBM processing of entity hierarchy [%s], as at least one entity [%s] has been processed",
							entityHierarchy.getRoot().getEntityNamingSource().getEntityName(),
							entityName
					);
					continue hierarchy_loop;
				}
			}

			modelBinder.bindEntityHierarchy( entityHierarchy );
			processedEntityNames.addAll( entityHierarchy.getContainedEntityNames() );
		}
	}

	@Override
	public void postProcessEntityHierarchies() {
		modelBinder.finishUp( rootBuildingContext );
	}

	@Override
	public void processResultSetMappings() {
		for ( MappingDocument mappingDocument : mappingDocuments ) {
			mappingDocument.processResultSetMappings();
		}
	}

	@Override
	public void finishUp() {
		for ( MappingDocument mappingDocument : mappingDocuments ) {
			mappingDocument.finishUp();
		}
	}
}
