/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmDiscriminatorSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmEntityBaseDefinition;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmJoinedSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSubclassEntityBaseDefinition;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmUnionSubclassEntityType;
import org.hibernate.boot.model.source.spi.EntitySource;

import static org.hibernate.boot.BootLogging.BOOT_LOGGER;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/**
 * Helper for dealing with entity inheritance hierarchies in {@code hbm.xml}
 * processing.  In {@code hbm.xml} the super/sub may be split across documents.
 * The purpose of this class is to:<ol>
 *     <li>
 *         validate that all hierarchies are complete (make sure a mapping does not reference
 *         an unknown entity as its super)
 *     </li>
 *     <li>
 *         ultimately order the processing of every entity to make sure we process each
 *         hierarchy "downward" (from super to sub(s)).
 *     </li>
 * </ol>
 *
 * @author Steve Ebersole
 */
public class EntityHierarchyBuilder {

	private final List<EntityHierarchySourceImpl> entityHierarchyList = new ArrayList<>();

	private final Map<String,AbstractEntitySourceImpl> entitySourceByNameMap = new HashMap<>();
	private Map<String,List<ExtendsQueueEntry>> toBeLinkedQueue;

	public EntityHierarchyBuilder() {
	}

	/**
	 * To be called after all mapping documents have been processed (via {@link #indexMappingDocument})
	 *
	 * @return The built hierarchies
	 *
	 * @throws HibernateException Indicates subclass mappings still waiting to be linked to their super types
	 */
	public List<EntityHierarchySourceImpl> buildHierarchies() throws HibernateException {
		if ( toBeLinkedQueue != null && !toBeLinkedQueue.isEmpty() ) {
			if ( BOOT_LOGGER.isDebugEnabled() ) {
				for ( var waitingListEntry : toBeLinkedQueue.entrySet() ) {
					for ( var waitingEntry : waitingListEntry.getValue() ) {
						BOOT_LOGGER.entitySupertypeExtendsNotFound(
								waitingListEntry.getKey(),
								waitingEntry.sourceMappingDocument.getOrigin(),
								waitingEntry.sourceMappingDocument.determineEntityName( waitingEntry.jaxbSubEntityMapping )
						);
					}
				}
			}
			throw new HibernateException(
					"Not all named supertypes (extends) were found : " + toBeLinkedQueue.keySet()
			);
		}

		return entityHierarchyList;
	}

	/**
	 * Called for each mapping document.
	 *
	 * @param mappingDocument The {@code hbm.xml} document to index
	 */
	public void indexMappingDocument(MappingDocument mappingDocument) {
		BOOT_LOGGER.indexingMappingDocumentForHierarchyOrdering( String.valueOf( mappingDocument.getOrigin() ) );
		final var mappingBinding = mappingDocument.getDocumentRoot();

		// iterate all root class definitions at the hibernate-mapping level
		for ( var jaxbRootEntity : mappingBinding.getClazz() ) {
			// we can immediately handle <class/> elements in terms of creating the hierarchy entry
			final var rootEntitySource = new RootEntitySourceImpl( mappingDocument, jaxbRootEntity );
			entitySourceByNameMap.put( rootEntitySource.getEntityNamingSource().getEntityName(), rootEntitySource );
			entityHierarchyList.add( new EntityHierarchySourceImpl( rootEntitySource, mappingDocument ) );
			linkAnyWaiting( mappingDocument, rootEntitySource );
			// process any of its nested sub-entity definitions
			processRootEntitySubEntityElements( mappingDocument, jaxbRootEntity, rootEntitySource );
		}

		// iterate all discriminator-based subclass definitions at the hibernate-mapping level
		for ( var discriminatorSubclassEntityBinding : mappingBinding.getSubclass() ) {
			processTopLevelSubClassBinding( mappingDocument, discriminatorSubclassEntityBinding );
		}

		// iterate all joined-subclass definitions at the hibernate-mapping level
		for ( var joinedSubclassEntityBinding : mappingBinding.getJoinedSubclass() ) {
			processTopLevelSubClassBinding( mappingDocument, joinedSubclassEntityBinding );
		}

		// iterate all union-subclass definitions at the hibernate-mapping level
		for ( var unionSubclassEntityBinding : mappingBinding.getUnionSubclass() ) {
			processTopLevelSubClassBinding( mappingDocument, unionSubclassEntityBinding );
		}
	}

	private void processRootEntitySubEntityElements(
			MappingDocument mappingDocument,
			JaxbHbmRootEntityType jaxbRootEntity,
			RootEntitySourceImpl rootEntitySource) {
		// todo : technically we should only allow one mutually-exclusive; should we enforce that here?
		//		I believe the DTD/XSD does enforce that, so maybe not a big deal
		processElements( mappingDocument, jaxbRootEntity.getSubclass(), rootEntitySource );
		processElements( mappingDocument, jaxbRootEntity.getJoinedSubclass(), rootEntitySource );
		processElements( mappingDocument, jaxbRootEntity.getUnionSubclass(), rootEntitySource );
	}

	private void processSubEntityElements(
			MappingDocument mappingDocument,
			JaxbHbmEntityBaseDefinition entityBinding,
			AbstractEntitySourceImpl container) {
		if ( entityBinding instanceof JaxbHbmDiscriminatorSubclassEntityType jaxbSubclass ) {
			processElements( mappingDocument, jaxbSubclass.getSubclass(), container );
		}
		else if ( entityBinding instanceof JaxbHbmJoinedSubclassEntityType jaxbJoinedSubclass ) {
			processElements( mappingDocument, jaxbJoinedSubclass.getJoinedSubclass(), container );
		}
		else if ( entityBinding instanceof JaxbHbmUnionSubclassEntityType jaxbUnionSubclass ) {
			processElements( mappingDocument, jaxbUnionSubclass.getUnionSubclass(), container );
		}
	}

	private void processElements(
			MappingDocument mappingDocument,
			List<? extends JaxbHbmSubclassEntityBaseDefinition> nestedSubEntityList,
			AbstractEntitySourceImpl container) {
		for ( final var jaxbSubEntity : nestedSubEntityList ) {
			final var subClassEntitySource = createSubClassEntitySource(
					mappingDocument,
					jaxbSubEntity,
					container
			);
			entitySourceByNameMap.put(
					subClassEntitySource.getEntityNamingSource().getEntityName(),
					subClassEntitySource
			);
			container.add( subClassEntitySource );
			linkAnyWaiting( mappingDocument, subClassEntitySource );
			// Re-run the subelement to handle subclasses within the subclass.
			processSubEntityElements( mappingDocument, jaxbSubEntity, subClassEntitySource );
		}
	}

	private SubclassEntitySourceImpl createSubClassEntitySource(
			MappingDocument mappingDocument,
			JaxbHbmSubclassEntityBaseDefinition jaxbSubEntity,
			EntitySource superEntity) {
		return jaxbSubEntity instanceof JaxbHbmJoinedSubclassEntityType jaxbJoinedSubclass
				? new JoinedSubclassEntitySourceImpl( mappingDocument, jaxbJoinedSubclass, superEntity )
				: new SubclassEntitySourceImpl( mappingDocument, jaxbSubEntity, superEntity );
	}

	private void processTopLevelSubClassBinding(
			MappingDocument mappingDocument,
			JaxbHbmSubclassEntityBaseDefinition jaxbSubEntityMapping) {
		final var entityItExtends = locateExtendedEntitySource( mappingDocument, jaxbSubEntityMapping );
		if ( entityItExtends == null ) {
			// we have not seen its declared super-type yet, add it to the queue to be linked up
			// later when (if) we do
			addToToBeLinkedQueue( mappingDocument, jaxbSubEntityMapping );
		}
		else {
			// we have seen its super-type already
			final var subEntitySource = createSubClassEntitySource(
					mappingDocument,
					jaxbSubEntityMapping,
					entityItExtends
			);
			entitySourceByNameMap.put( subEntitySource.getEntityNamingSource().getEntityName(), subEntitySource );
			entityItExtends.add( subEntitySource );
			// this may have been a "middle type".  So link any sub entities that may be waiting on it
			linkAnyWaiting( mappingDocument, subEntitySource );
			processSubEntityElements( mappingDocument, jaxbSubEntityMapping, subEntitySource );
		}
	}

	private AbstractEntitySourceImpl locateExtendedEntitySource(
			MappingDocument mappingDocument,
			JaxbHbmSubclassEntityBaseDefinition jaxbSubEntityMapping) {
		// NOTE: extends may refer to either an entity-name or a class-name, we need to check each
		// first check using the entity name
		var entityItExtends = entitySourceByNameMap.get( jaxbSubEntityMapping.getExtends() );
		if ( entityItExtends == null ) {
			// next, check using the class name
			entityItExtends = entitySourceByNameMap.get(
					mappingDocument.qualifyClassName( jaxbSubEntityMapping.getExtends() )
			);
		}
		return entityItExtends;
	}

	private void addToToBeLinkedQueue(
			MappingDocument mappingDocument,
			JaxbHbmSubclassEntityBaseDefinition jaxbSubEntityMapping) {
		List<ExtendsQueueEntry> waitingList = null;

		if ( toBeLinkedQueue == null ) {
			toBeLinkedQueue = new HashMap<>();
		}
		else {
			waitingList = toBeLinkedQueue.get( jaxbSubEntityMapping.getExtends() );
		}

		if ( waitingList == null ) {
			waitingList = new ArrayList<>();
			toBeLinkedQueue.put( jaxbSubEntityMapping.getExtends(), waitingList );
		}

		waitingList.add( new ExtendsQueueEntry( mappingDocument, jaxbSubEntityMapping ) );
	}

	private void linkAnyWaiting(
			MappingDocument mappingDocument,
			AbstractEntitySourceImpl entitySource) {
		if ( toBeLinkedQueue != null ) {
			var waitingList = toBeLinkedQueue.remove( entitySource.jaxbEntityMapping().getEntityName() );
			if ( waitingList != null ) {
				processWaitingSubEntityMappings( entitySource, waitingList );
				waitingList.clear();
			}

			if ( isNotEmpty( entitySource.jaxbEntityMapping().getName() ) ) {
				final String entityClassName = entitySource.jaxbEntityMapping().getName();
				waitingList = toBeLinkedQueue.remove( entityClassName );
				if ( waitingList != null ) {
					processWaitingSubEntityMappings( entitySource, waitingList );
					waitingList.clear();
				}

				final String qualifiedEntityClassName = mappingDocument.qualifyClassName( entityClassName );
				if ( !entityClassName.equals( qualifiedEntityClassName ) ) {
					waitingList = toBeLinkedQueue.remove( qualifiedEntityClassName );
					if ( waitingList != null ) {
						processWaitingSubEntityMappings( entitySource, waitingList );
						waitingList.clear();
					}
				}
			}
		}
	}

	private void processWaitingSubEntityMappings(
			AbstractEntitySourceImpl entitySource,
			List<ExtendsQueueEntry> waitingList) {
		for ( var entry : waitingList ) {
			final var subEntitySource = createSubClassEntitySource(
					entry.sourceMappingDocument,
					entry.jaxbSubEntityMapping,
					entitySource
			);
			entitySourceByNameMap.put( subEntitySource.getEntityNamingSource().getEntityName(), subEntitySource );
			entitySource.add( subEntitySource );
			// this may have been a "middle type".  So link any sub entities that may be waiting on it
			linkAnyWaiting( entry.sourceMappingDocument, subEntitySource );
			processSubEntityElements( entry.sourceMappingDocument, entry.jaxbSubEntityMapping, subEntitySource );
		}
	}

	private static class ExtendsQueueEntry {
		private final MappingDocument sourceMappingDocument;
		private final JaxbHbmSubclassEntityBaseDefinition jaxbSubEntityMapping;

		public ExtendsQueueEntry(
				MappingDocument sourceMappingDocument,
				JaxbHbmSubclassEntityBaseDefinition jaxbSubEntityMapping) {
			this.sourceMappingDocument = sourceMappingDocument;
			this.jaxbSubEntityMapping = jaxbSubEntityMapping;
		}
	}

}
