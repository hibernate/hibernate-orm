/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.hbm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.MappingException;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jaxb.spi.hbm.EntityElement;
import org.hibernate.jaxb.spi.hbm.JaxbClassElement;
import org.hibernate.jaxb.spi.hbm.JaxbHibernateMapping;
import org.hibernate.jaxb.spi.hbm.JaxbJoinedSubclassElement;
import org.hibernate.jaxb.spi.hbm.JaxbQueryElement;
import org.hibernate.jaxb.spi.hbm.JaxbSqlQueryElement;
import org.hibernate.jaxb.spi.hbm.JaxbSubclassElement;
import org.hibernate.jaxb.spi.hbm.JaxbUnionSubclassElement;
import org.hibernate.jaxb.spi.hbm.SubEntityElement;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.source.EntitySource;
import org.hibernate.metamodel.spi.source.SubclassEntityContainer;
import org.hibernate.metamodel.spi.source.SubclassEntitySource;

/**
 * @author Steve Ebersole
 */
public class HierarchyBuilder {
	private static final CoreMessageLogger LOG = Logger
			.getMessageLogger( CoreMessageLogger.class, HierarchyBuilder.class.getName() );
	private final MetadataImplementor metadata;
	
	private final List<EntityHierarchyImpl> entityHierarchies = new ArrayList<EntityHierarchyImpl>();

	// process state
	private final Map<String,SubclassEntityContainer> subEntityContainerMap = new HashMap<String, SubclassEntityContainer>();
	private final List<ExtendsQueueEntry> extendsQueue = new ArrayList<ExtendsQueueEntry>();

	// mapping file specific state
	private MappingDocument currentMappingDocument;
	
	public HierarchyBuilder( MetadataImplementor metadata ) {
		this.metadata = metadata;
	}

	public void processMappingDocument(MappingDocument mappingDocument) {
		this.currentMappingDocument = mappingDocument;
		try {
			LOG.tracef( "Processing mapping document: %s ", mappingDocument.getOrigin() );
			processCurrentMappingDocument();
		}
		finally {
			this.currentMappingDocument = null;
		}
	}

	private void processCurrentMappingDocument() {
		final JaxbHibernateMapping root = currentMappingDocument.getMappingRoot();
		for ( final JaxbClassElement jaxbClass : root.getClazz() ) {
			// we can immediately handle <class/> elements in terms of creating the hierarchy entry
			final RootEntitySourceImpl rootEntitySource = new RootEntitySourceImpl(
					currentMappingDocument,
					jaxbClass
			);
			final EntityHierarchyImpl hierarchy = new EntityHierarchyImpl( rootEntitySource );
			entityHierarchies.add( hierarchy );
			subEntityContainerMap.put( rootEntitySource.getEntityName(), rootEntitySource );
			processSubElements( jaxbClass, rootEntitySource );
		}
		List<SubEntityElement> subEntityElements = new ArrayList<SubEntityElement>( root.getJoinedSubclass() );
		subEntityElements.addAll( root.getUnionSubclass() );
		subEntityElements.addAll( root.getSubclass() );

		for ( final SubEntityElement element : subEntityElements ) {
			processSubclassElement( element );
		}

	}

	private void processSubclassElement(SubEntityElement element) {
		// we have to see if this things super-type has been found yet, and if not add it to the
		// extends queue
		final String entityItExtends = currentMappingDocument.getMappingLocalBindingContext()
				.qualifyClassName( element.getExtends() );
		final SubclassEntityContainer container = subEntityContainerMap.get( entityItExtends );
		final SubclassEntitySourceImpl subClassEntitySource = createSubClassEntitySource( element, (EntitySource) container );
		final String entityName = subClassEntitySource.getEntityName();
		subEntityContainerMap.put( entityName, subClassEntitySource );
		processSubElements( element, subClassEntitySource );
		if ( container != null ) {
			// we already have this entity's super, attach it and continue
			container.add( subClassEntitySource );
		}
		else {
			// we do not yet have the super and have to wait, so add it fto the extends queue
			extendsQueue.add( new ExtendsQueueEntry( subClassEntitySource, entityItExtends ) );
		}
	}

	public List<EntityHierarchyImpl> groupEntityHierarchies() {
		while ( ! extendsQueue.isEmpty() ) {
			// set up a pass over the queue
			int numberOfMappingsProcessed = 0;
			Iterator<ExtendsQueueEntry> iterator = extendsQueue.iterator();
			while ( iterator.hasNext() ) {
				final ExtendsQueueEntry entry = iterator.next();
				final SubclassEntityContainer container = subEntityContainerMap.get( entry.entityItExtends );
				if ( container != null ) {
					// we now have this entity's super, attach it and remove entry from extends queue
					container.add( entry.subClassEntitySource );
					iterator.remove();
					numberOfMappingsProcessed++;
				}
			}

			if ( numberOfMappingsProcessed == 0 ) {

				// todo : we could log the waiting dependencies...
				throw currentMappingDocument.getMappingLocalBindingContext().makeMappingException( "Unable to process extends dependencies in hbm files" );
			}
		}

		return entityHierarchies;
	}

	private void processSubElements(EntityElement entityElement, SubclassEntityContainer container) {
		if ( JaxbClassElement.class.isInstance( entityElement ) ) {
			final JaxbClassElement jaxbClass = (JaxbClassElement) entityElement;
			processElements( jaxbClass.getJoinedSubclass(), container );
			processElements( jaxbClass.getSubclass(), container );
			processElements( jaxbClass.getUnionSubclass(), container );
		}
		else if ( JaxbSubclassElement.class.isInstance( entityElement ) ) {
			final JaxbSubclassElement jaxbSubclass = (JaxbSubclassElement) entityElement;
			processElements( jaxbSubclass.getSubclass(), container );
		}
		else if ( JaxbJoinedSubclassElement.class.isInstance( entityElement ) ) {
			final JaxbJoinedSubclassElement jaxbJoinedSubclass = (JaxbJoinedSubclassElement) entityElement;
			processElements( jaxbJoinedSubclass.getJoinedSubclass(), container );
		}
		else if ( JaxbUnionSubclassElement.class.isInstance( entityElement ) ) {
			final JaxbUnionSubclassElement jaxbUnionSubclass = (JaxbUnionSubclassElement) entityElement;
			processElements( jaxbUnionSubclass.getUnionSubclass(), container );
		}
		processNamedQueries( entityElement );
	}

	private void processElements(List<? extends SubEntityElement> subElements, SubclassEntityContainer container) {
		for ( final SubEntityElement subElement : subElements ) {
			final SubclassEntitySourceImpl subclassEntitySource = createSubClassEntitySource( subElement, ( EntitySource ) container );
			container.add( subclassEntitySource );
			final String subEntityName = subclassEntitySource.getEntityName();
			subEntityContainerMap.put( subEntityName, subclassEntitySource );
			
			// Re-run the sub element to handle, as an example, subclasses
			// within a subclass.
			processSubElements(subElement, subclassEntitySource);
		}
	}

	private SubclassEntitySourceImpl createSubClassEntitySource(SubEntityElement subEntityElement, EntitySource entitySource) {
		if ( JaxbJoinedSubclassElement.class.isInstance( subEntityElement ) ) {
			return new JoinedSubclassEntitySourceImpl(
					currentMappingDocument,
					JaxbJoinedSubclassElement.class.cast( subEntityElement ),
					entitySource
			);
		}
		else {
			return new SubclassEntitySourceImpl( currentMappingDocument, subEntityElement, entitySource );
		}
	}

	private void processNamedQueries( EntityElement entityElement ) {
		// For backward compatibility, store named queries prefixed with
		// the class name.
		String queryNamePrefix = entityElement.getEntityName();
		queryNamePrefix = StringHelper.isNotEmpty( queryNamePrefix ) ? queryNamePrefix : currentMappingDocument.getMappingLocalBindingContext().qualifyClassName( entityElement.getName() );
		for ( final JaxbQueryElement element : entityElement.getQuery() ) {
			element.setName( queryNamePrefix + "." + element.getName() );
			NamedQueryBindingHelper.bindNamedQuery( element, metadata );
		}
		for ( final JaxbSqlQueryElement element : entityElement.getSqlQuery() ) {
			element.setName( queryNamePrefix + "." + element.getName() );
			NamedQueryBindingHelper.bindNamedSQLQuery(
					element,
					currentMappingDocument.getMappingLocalBindingContext(),
					metadata
			);
		}
	}

	private static class ExtendsQueueEntry {
		private final SubclassEntitySource subClassEntitySource;
		private final String entityItExtends;

		private ExtendsQueueEntry(SubclassEntitySource subClassEntitySource, String entityItExtends) {
			this.subClassEntitySource = subClassEntitySource;
			this.entityItExtends = entityItExtends;
		}
	}
}
