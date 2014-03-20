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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.envers.configuration.internal.metadata.AuditEntityNameRegister;
import org.hibernate.envers.configuration.internal.metadata.AuditMetadataGenerator;
import org.hibernate.envers.configuration.internal.metadata.EntityXmlMappingData;
import org.hibernate.envers.configuration.internal.metadata.reader.AnnotationsMetadataReader;
import org.hibernate.envers.configuration.internal.metadata.reader.ClassAuditingData;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.internal.EnversMessageLogger;
import org.hibernate.envers.internal.entities.EntitiesConfigurations;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.envers.internal.tools.graph.GraphTopologicalSort;
import org.hibernate.envers.strategy.AuditStrategy;
import org.hibernate.metamodel.spi.binding.EntityBinding;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.DOMWriter;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.jboss.logging.Logger;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class EntitiesConfigurator {
	private static final EnversMessageLogger LOG = Logger.getMessageLogger(
			EnversMessageLogger.class,
			EntitiesConfigurator.class.getName()
	);


	public EntitiesConfigurations configure(
			AuditConfiguration.AuditConfigurationContext context,
			AuditStrategy auditStrategy,
			Document revisionInfoXmlMapping,
			Element revisionInfoRelationMapping) {
		// Creating a name register to capture all audit entity names created.
		final AuditEntityNameRegister auditEntityNameRegister = new AuditEntityNameRegister();
		final DOMWriter writer = new DOMWriter();

		// Sorting the entity bindings topologically - superclass always before subclass
		final List<EntityBinding> entityBindings =
				GraphTopologicalSort.sort( new EntityBindingGraphDefiner( context.getMetadata() ) );

		final ClassesAuditingData classesAuditingData = new ClassesAuditingData();
		final Map<EntityBinding, EntityXmlMappingData> xmlMappings = new HashMap<EntityBinding, EntityXmlMappingData>();

		// Reading metadata from annotations
		final AnnotationsMetadataReader annotationsMetadataReader = new AnnotationsMetadataReader( context );
		for ( EntityBinding entityBinding : entityBindings  ) {

			// Collecting information from annotations on the persistent class pc
			final ClassAuditingData auditData = annotationsMetadataReader.getAuditData( entityBinding );

			classesAuditingData.addClassAuditingData( entityBinding, auditData );
		}

		// Now that all information is read we can update the calculated fields.
		classesAuditingData.updateCalculatedFields();

		final AuditMetadataGenerator auditMetaGen = new AuditMetadataGenerator(
				context, auditStrategy, revisionInfoRelationMapping, auditEntityNameRegister
		);

		// First pass
		for ( Map.Entry<EntityBinding, ClassAuditingData> pcDatasEntry : classesAuditingData.getAllEntityBindingAuditedData() ) {
			final EntityBinding entityBinding = pcDatasEntry.getKey();
			final ClassAuditingData auditData = pcDatasEntry.getValue();

			final EntityXmlMappingData xmlMappingData = new EntityXmlMappingData();
			if ( auditData.isAudited() ) {
				if ( !StringTools.isEmpty( auditData.getAuditTable().value() ) ) {
					context.getAuditEntitiesConfiguration().addCustomAuditTableName( entityBinding.getEntityName(), auditData.getAuditTable().value() );
				}

				auditMetaGen.generateFirstPass( entityBinding, auditData, xmlMappingData, true );
			}
			else {
				auditMetaGen.generateFirstPass( entityBinding, auditData, xmlMappingData, false );
			}

			xmlMappings.put( entityBinding, xmlMappingData );
		}

		// Second pass
		for ( Map.Entry<EntityBinding, ClassAuditingData> pcDatasEntry : classesAuditingData.getAllEntityBindingAuditedData() ) {
			final EntityXmlMappingData xmlMappingData = xmlMappings.get( pcDatasEntry.getKey() );

			if ( pcDatasEntry.getValue().isAudited() ) {
				auditMetaGen.generateSecondPass( pcDatasEntry.getKey(), pcDatasEntry.getValue(), xmlMappingData );
				try {
					logDocument( xmlMappingData.getMainXmlMapping() );
					context.addDocument( writer.write( xmlMappingData.getMainXmlMapping() ) );

					for ( Document additionalMapping : xmlMappingData.getAdditionalXmlMappings() ) {
						logDocument( additionalMapping );
						context.addDocument( writer.write( additionalMapping ) );
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
					logDocument( revisionInfoXmlMapping );
					context.addDocument( writer.write( revisionInfoXmlMapping ) );
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

	private void logDocument(Document e) {
		if ( !LOG.isDebugEnabled() ) {
			return;
		}
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final Writer w = new PrintWriter( baos );

		try {
			final XMLWriter xw = new XMLWriter( w, new OutputFormat( " ", true ) );
			xw.write( e );
			w.flush();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}

		LOG.debug( "-----------" );
		LOG.debug( baos.toString() );
		LOG.debug( "-----------" );
	}
}
