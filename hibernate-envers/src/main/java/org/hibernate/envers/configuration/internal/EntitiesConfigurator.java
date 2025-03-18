/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.envers.boot.model.PersistentEntity;
import org.hibernate.envers.boot.spi.EnversMetadataBuildingContext;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.configuration.internal.metadata.AuditMetadataGenerator;
import org.hibernate.envers.configuration.internal.metadata.EntityMappingData;
import org.hibernate.envers.configuration.internal.metadata.reader.AnnotationsMetadataReader;
import org.hibernate.envers.configuration.internal.metadata.reader.ClassAuditingData;
import org.hibernate.envers.internal.entities.EntitiesConfigurations;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.envers.internal.tools.graph.GraphTopologicalSort;
import org.hibernate.mapping.PersistentClass;


/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class EntitiesConfigurator {

	public EntitiesConfigurations configure(EnversMetadataBuildingContext metadataBuildingContext) {
		final MetadataImplementor metadata = metadataBuildingContext.getMetadataCollector();
		final Configuration configuration = metadataBuildingContext.getConfiguration();

		// Sorting the persistent class topologically - superclass always before subclass

		final Iterator<PersistentClass> classes = GraphTopologicalSort.sort( new PersistentClassGraphDefiner( metadata ) )
				.iterator();

		final ClassesAuditingData classesAuditingData = new ClassesAuditingData();

		// Reading metadata from annotations
		final AnnotationsMetadataReader reader = new AnnotationsMetadataReader( metadataBuildingContext );
		while ( classes.hasNext() ) {
			final PersistentClass pc = classes.next();

			// Ensure we're in POJO, not dynamic model, mapping.
			if ( pc.getClassName() != null ) {
				// Collecting information from annotations on the persistent class pc
				classesAuditingData.addClassAuditingData( reader.getAuditData( pc ) );
			}
		}

		// Now that all information is read we can update the calculated fields.
		classesAuditingData.updateCalculatedFields();

		final AuditMetadataGenerator auditMetaGen = new AuditMetadataGenerator( metadataBuildingContext );

		// First pass
		final Map<PersistentClass, EntityMappingData> mappings = new HashMap<>();
		for ( ClassAuditingData auditData : classesAuditingData.getAllClassAuditedData() ) {
			final EntityMappingData mappingData = new EntityMappingData();
			final PersistentClass persistentClass = auditData.getPersistentClass();
			if ( auditData.isAudited() ) {
				if ( !StringTools.isEmpty( auditData.getAuditTable().value() ) ) {
					final String entityName = persistentClass.getEntityName();
					final String auditTableName = auditData.getAuditTable().value();
					configuration.addCustomAuditTableName( entityName, auditTableName );
				}
				auditMetaGen.generateFirstPass( auditData, mappingData, true );
			}
			else {
				auditMetaGen.generateFirstPass( auditData, mappingData, false );
			}
			mappings.put( persistentClass, mappingData );
		}

		// Second pass
		for ( ClassAuditingData auditingData : classesAuditingData.getAllClassAuditedData() ) {
			final EntityMappingData mappingData = mappings.get( auditingData.getPersistentClass() );
			if ( auditingData.isAudited() ) {
				auditMetaGen.generateSecondPass( auditingData, mappingData );
				mappingData.build();

				metadataBuildingContext.getMappingCollector().addDocument( mappingData.getMapping() );

				for ( JaxbHbmHibernateMapping additionalMapping : mappingData.getAdditionalMappings() ) {
					metadataBuildingContext.getMappingCollector().addDocument( additionalMapping );
				}
			}
		}

		// Only if there are any versioned classes
		if ( !auditMetaGen.getAuditedEntityConfigurations().isEmpty() ) {
			final PersistentEntity revisionInfoMapping = configuration.getRevisionInfo().getRevisionInfoMapping();
			if ( revisionInfoMapping != null ) {
				final EntityMappingData mappingData = new EntityMappingData();
				mappingData.addMapping( revisionInfoMapping );
				mappingData.build();

				metadataBuildingContext.getMappingCollector().addDocument( mappingData.getMapping() );
			}
		}

		return new EntitiesConfigurations(
				auditMetaGen.getAuditedEntityConfigurations(),
				auditMetaGen.getNotAuditedEntityConfigurations()
		);
	}
}
