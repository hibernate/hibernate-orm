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
package org.hibernate.metamodel.binder.source.hbm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.metamodel.binder.source.MetadataImplementor;
import org.hibernate.metamodel.binder.source.hbm.xml.mapping.EntityElement;
import org.hibernate.metamodel.binder.source.hbm.xml.mapping.SubEntityElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLJoinedSubclassElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSubclassElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLUnionSubclassElement;

/**
 * @author Steve Ebersole
 */
public class HierarchyBuilder {
	private final MetadataImplementor metadata;

	private final List<EntityHierarchy> entityHierarchies = new ArrayList<EntityHierarchy>();

	// process state
	private final Map<String,SubEntityContainer> subEntityContainerMap = new HashMap<String, SubEntityContainer>();
	private final List<ExtendsQueueEntry> extendsQueue = new ArrayList<ExtendsQueueEntry>();

	// mapping file specific state
	private MappingDocument currentMappingDocument;

	public HierarchyBuilder(MetadataImplementor metadata) {
		this.metadata = metadata;
	}

	public void processMappingDocument(MappingDocument mappingDocument) {
		this.currentMappingDocument = mappingDocument;
		try {
			processCurrentMappingDocument();
		}
		finally {
			this.currentMappingDocument = null;
		}
	}

	private void processCurrentMappingDocument() {
		for ( Object entityElementO : currentMappingDocument.getMappingRoot().getClazzOrSubclassOrJoinedSubclass() ) {
			final EntityElement entityElement = (EntityElement) entityElementO;
			if ( XMLHibernateMapping.XMLClass.class.isInstance( entityElement ) ) {
				// we can immediately handle <class/> elements in terms of creating the hierarchy entry
				final XMLHibernateMapping.XMLClass xmlClass = (XMLHibernateMapping.XMLClass) entityElement;
				final EntityHierarchy hierarchy = new EntityHierarchy( xmlClass, currentMappingDocument );
				entityHierarchies.add( hierarchy );
				subEntityContainerMap.put( hierarchy.getEntitySourceInformation().getMappedEntityName(), hierarchy );
				processSubElements( entityElement, hierarchy );
			}
			else {
				// we have to see if this things super-type has been found yet, and if not add it to the
				// extends queue
				final EntityHierarchySubEntity subEntityDescriptor = new EntityHierarchySubEntity(
						entityElement,
						currentMappingDocument
				);
				final String entityName = subEntityDescriptor.getEntitySourceInformation().getMappedEntityName();
				subEntityContainerMap.put( entityName, subEntityDescriptor );
				final String entityItExtends = currentMappingDocument.getMappingLocalBindingContext().qualifyClassName(
						((SubEntityElement) entityElement).getExtends()
				);
				processSubElements( entityElement, subEntityDescriptor );
				final SubEntityContainer container = subEntityContainerMap.get( entityItExtends );
				if ( container != null ) {
					// we already have this entity's super, attach it and continue
					container.addSubEntityDescriptor( subEntityDescriptor );
				}
				else {
					// we do not yet have the super and have to wait, so add it fto the extends queue
					extendsQueue.add( new ExtendsQueueEntry( subEntityDescriptor, entityItExtends ) );
				}
			}
		}
	}

	public List<EntityHierarchy> groupEntityHierarchies() {
		while ( ! extendsQueue.isEmpty() ) {
			// set up a pass over the queue
			int numberOfMappingsProcessed = 0;
			Iterator<ExtendsQueueEntry> iterator = extendsQueue.iterator();
			while ( iterator.hasNext() ) {
				final ExtendsQueueEntry entry = iterator.next();
				final SubEntityContainer container = subEntityContainerMap.get( entry.entityItExtends );
				if ( container != null ) {
					// we now have this entity's super, attach it and remove entry from extends queue
					container.addSubEntityDescriptor( entry.subEntityDescriptor );
					iterator.remove();
					numberOfMappingsProcessed++;
				}
			}

			if ( numberOfMappingsProcessed == 0 ) {
				// todo : we could log the waiting dependencies...
				throw new MappingException( "Unable to process extends dependencies in hbm files" );
			}
		}

		return entityHierarchies;
	}

	private void processSubElements(EntityElement entityElement, SubEntityContainer container) {
		if ( XMLHibernateMapping.XMLClass.class.isInstance( entityElement ) ) {
			final XMLHibernateMapping.XMLClass xmlClass = (XMLHibernateMapping.XMLClass) entityElement;
			processElements( xmlClass.getJoinedSubclass(), container );
			processElements( xmlClass.getSubclass(), container );
			processElements( xmlClass.getUnionSubclass(), container );
		}
		else if ( XMLSubclassElement.class.isInstance( entityElement ) ) {
			final XMLSubclassElement xmlSubclass = (XMLSubclassElement) entityElement;
			processElements( xmlSubclass.getSubclass(), container );
		}
		else if ( XMLJoinedSubclassElement.class.isInstance( entityElement ) ) {
			final XMLJoinedSubclassElement xmlJoinedSubclass = (XMLJoinedSubclassElement) entityElement;
			processElements( xmlJoinedSubclass.getJoinedSubclass(), container );
		}
		else if ( XMLUnionSubclassElement.class.isInstance( entityElement ) ) {
			final XMLUnionSubclassElement xmlUnionSubclass = (XMLUnionSubclassElement) entityElement;
			processElements( xmlUnionSubclass.getUnionSubclass(), container );
		}
	}

	private void processElements(List subElements, SubEntityContainer container) {
		for ( Object subElementO : subElements ) {
			final SubEntityElement subElement = (SubEntityElement) subElementO;
			final EntityHierarchySubEntity subEntityDescriptor = new EntityHierarchySubEntity(
					subElement,
					currentMappingDocument
			);
			container.addSubEntityDescriptor( subEntityDescriptor );
			final String subEntityName = subEntityDescriptor.getEntitySourceInformation().getMappedEntityName();
			subEntityContainerMap.put( subEntityName, subEntityDescriptor );
		}
	}

	private static class ExtendsQueueEntry {
		private final EntityHierarchySubEntity subEntityDescriptor;
		private final String entityItExtends;

		private ExtendsQueueEntry(EntityHierarchySubEntity subEntityDescriptor, String entityItExtends) {
			this.subEntityDescriptor = subEntityDescriptor;
			this.entityItExtends = entityItExtends;
		}
	}
}
