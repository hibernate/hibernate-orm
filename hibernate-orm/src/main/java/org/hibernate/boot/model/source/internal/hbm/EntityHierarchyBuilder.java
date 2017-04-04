/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmDiscriminatorSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmEntityBaseDefinition;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmJoinedSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSubclassEntityBaseDefinition;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmUnionSubclassEntityType;
import org.hibernate.boot.model.source.spi.EntitySource;
import org.hibernate.internal.util.StringHelper;

import org.jboss.logging.Logger;

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
	private static final Logger log = Logger.getLogger( EntityHierarchyBuilder.class );

	private final List<EntityHierarchySourceImpl> entityHierarchyList = new ArrayList<EntityHierarchySourceImpl>();

	private final Map<String,AbstractEntitySourceImpl> entitySourceByNameMap = new HashMap<String, AbstractEntitySourceImpl>();
	private Map<String,List<ExtendsQueueEntry>> toBeLinkedQueue;

	public EntityHierarchyBuilder() {
	}

	/**
	 * To be called afterQuery all mapping documents have been processed (via {@link #indexMappingDocument})
	 *
	 * @return The built hierarchies
	 *
	 * @throws HibernateException Indicates subclass mappings still waiting to be linked to their super types
	 */
	public List<EntityHierarchySourceImpl> buildHierarchies() throws HibernateException {
		if ( toBeLinkedQueue != null && !toBeLinkedQueue.isEmpty() ) {
			if ( log.isDebugEnabled() ) {
				for ( Map.Entry<String, List<ExtendsQueueEntry>> waitingListEntry : toBeLinkedQueue.entrySet() ) {
					for ( ExtendsQueueEntry waitingEntry : waitingListEntry.getValue() ) {
						log.debugf(
								"Entity super-type named as extends [%s] for subclass [%s:%s] not found",
								waitingListEntry.getKey(),
								waitingEntry.sourceMappingDocument.getOrigin(),
								waitingEntry.sourceMappingDocument.determineEntityName( waitingEntry.jaxbSubEntityMapping )
						);
					}
				}
			}
			throw new HibernateException(
					"Not all named super-types (extends) were found : " + toBeLinkedQueue.keySet()
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
		log.tracef( "Indexing mapping document [%s] for purpose of building entity hierarchy ordering", mappingDocument.getOrigin() );
		final JaxbHbmHibernateMapping mappingBinding = mappingDocument.getDocumentRoot();

		// iterate all root class definitions at the hibernate-mapping level
		for ( JaxbHbmRootEntityType jaxbRootEntity : mappingBinding.getClazz() ) {

			// we can immediately handle <class/> elements in terms of creating the hierarchy entry
			final RootEntitySourceImpl rootEntitySource = new RootEntitySourceImpl( mappingDocument, jaxbRootEntity );
			entitySourceByNameMap.put( rootEntitySource.getEntityNamingSource().getEntityName(), rootEntitySource );

			final EntityHierarchySourceImpl hierarchy = new EntityHierarchySourceImpl( rootEntitySource );
			entityHierarchyList.add( hierarchy );

			linkAnyWaiting( mappingDocument, rootEntitySource );

			// process any of its nested sub-entity definitions
			processRootEntitySubEntityElements( mappingDocument, jaxbRootEntity, rootEntitySource );
		}

		// iterate all discriminator-based subclass definitions at the hibernate-mapping level
		for ( JaxbHbmDiscriminatorSubclassEntityType discriminatorSubclassEntityBinding : mappingBinding.getSubclass() ) {
			processTopLevelSubClassBinding( mappingDocument, discriminatorSubclassEntityBinding );
		}

		// iterate all joined-subclass definitions at the hibernate-mapping level
		for ( JaxbHbmJoinedSubclassEntityType joinedSubclassEntityBinding : mappingBinding.getJoinedSubclass() ) {
			processTopLevelSubClassBinding( mappingDocument, joinedSubclassEntityBinding );
		}

		// iterate all union-subclass definitions at the hibernate-mapping level
		for ( JaxbHbmUnionSubclassEntityType unionSubclassEntityBinding : mappingBinding.getUnionSubclass() ) {
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
		if ( JaxbHbmDiscriminatorSubclassEntityType.class.isInstance( entityBinding ) ) {
			final JaxbHbmDiscriminatorSubclassEntityType jaxbSubclass = (JaxbHbmDiscriminatorSubclassEntityType) entityBinding;
			processElements( mappingDocument, jaxbSubclass.getSubclass(), container );
		}
		else if ( JaxbHbmJoinedSubclassEntityType.class.isInstance( entityBinding ) ) {
			final JaxbHbmJoinedSubclassEntityType jaxbJoinedSubclass = (JaxbHbmJoinedSubclassEntityType) entityBinding;
			processElements( mappingDocument, jaxbJoinedSubclass.getJoinedSubclass(), container );
		}
		else if ( JaxbHbmUnionSubclassEntityType.class.isInstance( entityBinding ) ) {
			final JaxbHbmUnionSubclassEntityType jaxbUnionSubclass = (JaxbHbmUnionSubclassEntityType) entityBinding;
			processElements( mappingDocument, jaxbUnionSubclass.getUnionSubclass(), container );
		}
	}

	private void processElements(
			MappingDocument mappingDocument,
			List<? extends JaxbHbmSubclassEntityBaseDefinition> nestedSubEntityList,
			AbstractEntitySourceImpl container) {
		for ( final JaxbHbmSubclassEntityBaseDefinition jaxbSubEntity : nestedSubEntityList ) {
			final SubclassEntitySourceImpl subClassEntitySource = createSubClassEntitySource(
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

			// Re-run the sub element to handle subclasses within the subclass.
			processSubEntityElements( mappingDocument, jaxbSubEntity, subClassEntitySource );
		}
	}

	private SubclassEntitySourceImpl createSubClassEntitySource(
			MappingDocument mappingDocument,
			JaxbHbmSubclassEntityBaseDefinition jaxbSubEntity,
			EntitySource superEntity) {
		if ( JaxbHbmJoinedSubclassEntityType.class.isInstance( jaxbSubEntity ) ) {
			return new JoinedSubclassEntitySourceImpl(
					mappingDocument,
					JaxbHbmJoinedSubclassEntityType.class.cast( jaxbSubEntity ),
					superEntity
			);
		}
		else {
			return new SubclassEntitySourceImpl( mappingDocument, jaxbSubEntity, superEntity );
		}
	}

	private void processTopLevelSubClassBinding(
			MappingDocument mappingDocument,
			JaxbHbmSubclassEntityBaseDefinition jaxbSubEntityMapping) {
		final AbstractEntitySourceImpl entityItExtends = locateExtendedEntitySource( mappingDocument, jaxbSubEntityMapping );

		if ( entityItExtends == null ) {
			// we have not seen its declared super-type yet, add it to the queue to be linked up
			// later when (if) we do
			addToToBeLinkedQueue( mappingDocument, jaxbSubEntityMapping );
		}
		else {
			// we have seen its super-type already
			final SubclassEntitySourceImpl subEntitySource = createSubClassEntitySource(
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
		// NOTE : extends may refer to either an entity-name or a class-name, we need to check each

		// first check using entity-name
		AbstractEntitySourceImpl entityItExtends = entitySourceByNameMap.get( jaxbSubEntityMapping.getExtends() );
		if ( entityItExtends == null ) {
			// next, check using class name
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
			toBeLinkedQueue = new HashMap<String, List<ExtendsQueueEntry>>();
		}
		else {
			waitingList = toBeLinkedQueue.get( jaxbSubEntityMapping.getExtends() );
		}

		if ( waitingList == null ) {
			waitingList = new ArrayList<ExtendsQueueEntry>();
			toBeLinkedQueue.put( jaxbSubEntityMapping.getExtends(), waitingList );
		}

		waitingList.add( new ExtendsQueueEntry( mappingDocument, jaxbSubEntityMapping ) );
	}

	private void linkAnyWaiting(
			MappingDocument mappingDocument,
			AbstractEntitySourceImpl entitySource) {
		if ( toBeLinkedQueue == null ) {
			return;
		}

		List<ExtendsQueueEntry> waitingList = toBeLinkedQueue.remove( entitySource.jaxbEntityMapping().getEntityName() );
		if ( waitingList != null ) {
			processWaitingSubEntityMappings( entitySource, waitingList );
			waitingList.clear();
		}

		if ( StringHelper.isNotEmpty( entitySource.jaxbEntityMapping().getName() ) ) {
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

	private void processWaitingSubEntityMappings(
			AbstractEntitySourceImpl entitySource,
			List<ExtendsQueueEntry> waitingList) {
		for ( ExtendsQueueEntry entry : waitingList ) {
			final SubclassEntitySourceImpl subEntitySource = createSubClassEntitySource(
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
