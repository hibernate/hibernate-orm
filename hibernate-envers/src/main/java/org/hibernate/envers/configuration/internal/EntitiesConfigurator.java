/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers.configuration.internal;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.envers.configuration.internal.metadata.AuditEntityNameRegister;
import org.hibernate.envers.configuration.internal.metadata.AuditMetadataGenerator;
import org.hibernate.envers.configuration.internal.metadata.EntityXmlMappingData;
import org.hibernate.envers.configuration.internal.metadata.reader.AnnotationsMetadataReader;
import org.hibernate.envers.configuration.internal.metadata.reader.ClassAuditingData;
import org.hibernate.envers.internal.entities.EntitiesConfigurations;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.envers.internal.tools.graph.GraphTopologicalSort;
import org.hibernate.envers.strategy.AuditStrategy;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.service.ServiceRegistry;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class EntitiesConfigurator {
	public EntitiesConfigurations configure(
			MetadataImplementor metadata,
			ServiceRegistry serviceRegistry,
			ReflectionManager reflectionManager,
			MappingCollector mappingCollector,
			GlobalConfiguration globalConfiguration,
			AuditEntitiesConfiguration auditEntitiesConfiguration,
			AuditStrategy auditStrategy,
			Document revisionInfoXmlMapping,
			Element revisionInfoRelationMapping) {
		// Creating a name register to capture all audit entity names created.
		final AuditEntityNameRegister auditEntityNameRegister = new AuditEntityNameRegister();

		// Sorting the persistent class topologically - superclass always before subclass
		final Iterator<PersistentClass> classes = GraphTopologicalSort.sort( new PersistentClassGraphDefiner( metadata ) )
				.iterator();

		final ClassesAuditingData classesAuditingData = new ClassesAuditingData();
		final Map<PersistentClass, EntityXmlMappingData> xmlMappings = new HashMap<PersistentClass, EntityXmlMappingData>();

		// Reading metadata from annotations
		while ( classes.hasNext() ) {
			final PersistentClass pc = classes.next();

			// Ensure we're in POJO, not dynamic model, mapping.
			if (pc.getClassName() != null) {
				// Collecting information from annotations on the persistent class pc
				final AnnotationsMetadataReader annotationsMetadataReader =
						new AnnotationsMetadataReader(globalConfiguration, reflectionManager, pc);
				final ClassAuditingData auditData = annotationsMetadataReader.getAuditData();

				classesAuditingData.addClassAuditingData(pc, auditData);
			}
		}

		// Now that all information is read we can update the calculated fields.
		classesAuditingData.updateCalculatedFields();

		final AuditMetadataGenerator auditMetaGen = new AuditMetadataGenerator(
				metadata,
				serviceRegistry,
				globalConfiguration,
				auditEntitiesConfiguration,
				auditStrategy,
				revisionInfoRelationMapping,
				auditEntityNameRegister
		);

		// First pass
		for ( Map.Entry<PersistentClass, ClassAuditingData> pcDatasEntry : classesAuditingData.getAllClassAuditedData() ) {
			final PersistentClass pc = pcDatasEntry.getKey();
			final ClassAuditingData auditData = pcDatasEntry.getValue();

			final EntityXmlMappingData xmlMappingData = new EntityXmlMappingData();
			if ( auditData.isAudited() ) {
				if ( !StringTools.isEmpty( auditData.getAuditTable().value() ) ) {
					auditEntitiesConfiguration.addCustomAuditTableName( pc.getEntityName(), auditData.getAuditTable().value() );
				}

				auditMetaGen.generateFirstPass( pc, auditData, xmlMappingData, true );
			}
			else {
				auditMetaGen.generateFirstPass( pc, auditData, xmlMappingData, false );
			}

			xmlMappings.put( pc, xmlMappingData );
		}

		// Second pass
		for ( Map.Entry<PersistentClass, ClassAuditingData> pcDatasEntry : classesAuditingData.getAllClassAuditedData() ) {
			final EntityXmlMappingData xmlMappingData = xmlMappings.get( pcDatasEntry.getKey() );

			if ( pcDatasEntry.getValue().isAudited() ) {
				auditMetaGen.generateSecondPass( pcDatasEntry.getKey(), pcDatasEntry.getValue(), xmlMappingData );
				try {
					mappingCollector.addDocument( xmlMappingData.getMainXmlMapping() );

					for ( Document additionalMapping : xmlMappingData.getAdditionalXmlMappings() ) {
						mappingCollector.addDocument( additionalMapping );
					}
				}
				catch (DocumentException e) {
					throw new MappingException( e );
				}
			}
		}

		// Only if there are any versioned classes
		if ( auditMetaGen.getEntitiesConfigurations().size() > 0 ) {
			try {
				if ( revisionInfoXmlMapping != null ) {
					mappingCollector.addDocument( revisionInfoXmlMapping );
				}
			}
			catch (DocumentException e) {
				throw new MappingException( e );
			}
		}

		return new EntitiesConfigurations(
				auditMetaGen.getEntitiesConfigurations(),
				auditMetaGen.getNotAuditedEntitiesConfigurations()
		);
	}
}
