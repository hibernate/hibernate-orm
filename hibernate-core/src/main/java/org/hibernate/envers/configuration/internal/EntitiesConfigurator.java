/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.ClassLoadingException;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.envers.boot.spi.AuditMetadataBuilderImplementor;
import org.hibernate.envers.configuration.internal.metadata.AuditEntityNameRegister;
import org.hibernate.envers.configuration.internal.metadata.AuditMetadataGenerator;
import org.hibernate.envers.configuration.internal.metadata.EntityXmlMappingData;
import org.hibernate.envers.configuration.internal.metadata.reader.AnnotationsMetadataReader;
import org.hibernate.envers.configuration.internal.metadata.reader.ClassAuditingData;
import org.hibernate.envers.internal.entities.EntitiesConfigurations;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.service.ServiceRegistry;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;

import static org.hibernate.envers.internal.tools.graph.GraphTopologicalSort.sort;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class EntitiesConfigurator {

	private final ServiceRegistry serviceRegistry;
	private final AuditMetadataBuilderImplementor auditMetadataBuilder;
	private final AuditEntityNameRegister auditEntityNameRegister;
	private final Map<PersistentClass, EntityXmlMappingData> xmlMappings;

	public EntitiesConfigurator(AuditMetadataBuilderImplementor auditMetadataBuilder) {
		this.auditMetadataBuilder = auditMetadataBuilder;
		this.serviceRegistry = auditMetadataBuilder.getAuditMetadataBuildingOptions().getServiceRegistry();
		this.auditEntityNameRegister = new AuditEntityNameRegister();
		this.xmlMappings = new HashMap<>();
	}

	public EntitiesConfigurations build(
			InFlightMetadataCollector metadata,
			MappingCollector mappingCollector,
			Document revisionInfoXmlMapping,
			Element revisionInfoRelationMapping) {
		return complete(
				prepare( metadata ),
				metadata,
				mappingCollector,
				revisionInfoXmlMapping,
				revisionInfoRelationMapping
		);
	}

	private ClassesAuditingData prepare(InFlightMetadataCollector metadata) {
		final ReflectionManager reflectionManager = metadata.getBootstrapContext().getReflectionManager();
		final ClassesAuditingData classesAuditingData = new ClassesAuditingData();
		final Iterator<PersistentClass> classes = sort( new PersistentClassGraphDefiner( metadata ) ).iterator();
		while ( classes.hasNext() ) {
			final PersistentClass persistentClass = classes.next();
			if ( persistentClass.getClassName() != null ) {
				try {
					final XClass clazz =reflectionManager.classForName( persistentClass.getClassName() );
					final AnnotationsMetadataReader reader = new AnnotationsMetadataReader(
							auditMetadataBuilder.getAuditMetadataBuildingOptions(),
							reflectionManager,
							persistentClass,
							clazz
					);
					classesAuditingData.addClassAuditingData( persistentClass, reader.getAuditData() );
				}
				catch ( ClassLoadingException e ) {
					// todo: log a warning?
				}
			}
		}
		classesAuditingData.updateCalculatedFields();
		return classesAuditingData;
	}

	private EntitiesConfigurations complete(
			ClassesAuditingData classesAuditingData,
			InFlightMetadataCollector metadata,
			MappingCollector mappingCollector,
			Document revisionInfoXmlMapping,
			Element revisionInfoRelationMapping) {

		final AuditMetadataGenerator auditMetaGen = new AuditMetadataGenerator(
				metadata,
				serviceRegistry,
				auditMetadataBuilder.getAuditMetadataBuildingOptions(),
				revisionInfoRelationMapping,
				auditEntityNameRegister
		);

		firstPass( classesAuditingData, auditMetaGen );
		secondPass( classesAuditingData, auditMetaGen, mappingCollector, revisionInfoXmlMapping );

		return new EntitiesConfigurations(
				auditMetaGen.getEntitiesConfigurations(),
				auditMetaGen.getNotAuditedEntitiesConfigurations()
		);
	}

	// pc.getEntityName
	// pc.getTable
	// pc.getIdentifierProperty
	// pc.getIdentifierMapper
	// pc.getIdentifier
	// pc.getServiceRegistry
	// stores xml mapping per pc
	private void firstPass(final ClassesAuditingData classesAuditingData, final AuditMetadataGenerator generator) {
		for ( Map.Entry<PersistentClass, ClassAuditingData> pcDatasEntry : classesAuditingData.getAllClassAuditedData() ) {
			final PersistentClass pc = pcDatasEntry.getKey();
			final ClassAuditingData auditData = pcDatasEntry.getValue();

			final EntityXmlMappingData xmlMappingData = new EntityXmlMappingData();
			if ( auditData.isAudited() ) {
				if ( !StringTools.isEmpty( auditData.getAuditTable().value() ) ) {
					auditMetadataBuilder.getAuditMetadataBuildingOptions().addCustomAuditTableName(
							pc.getEntityName(),
							auditData.getAuditTable().value()
					);
				}
				generator.generateFirstPass( pc, auditData, xmlMappingData, true );
			}
			else {
				generator.generateFirstPass( pc, auditData, xmlMappingData, false );
			}

			xmlMappings.put( pc, xmlMappingData );
		}
	}

	// pc.getUnjoinedPropertyIterator
	// pc.getJoinIterator
	private void secondPass(
			final ClassesAuditingData classesAuditingData,
			final AuditMetadataGenerator generator,
			final MappingCollector mappingCollector,
			final Document revisionInfoXmlMapping) {
		for ( Map.Entry<PersistentClass, ClassAuditingData> pcDatasEntry : classesAuditingData.getAllClassAuditedData() ) {
			final EntityXmlMappingData xmlMappingData = xmlMappings.get( pcDatasEntry.getKey() );
			if ( pcDatasEntry.getValue().isAudited() ) {
				generator.generateSecondPass( pcDatasEntry.getKey(), pcDatasEntry.getValue(), xmlMappingData );
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
		if ( generator.getEntitiesConfigurations().size() > 0 ) {
			try {
				if ( revisionInfoXmlMapping != null ) {
					mappingCollector.addDocument( revisionInfoXmlMapping );
				}
			}
			catch (DocumentException e) {
				throw new MappingException( e );
			}
		}
	}
}
