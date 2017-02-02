/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.event.spi;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.entities.EntityConfiguration;
import org.hibernate.envers.internal.entities.RelationDescription;
import org.hibernate.envers.internal.entities.RelationType;
import org.hibernate.envers.internal.entities.mapper.PersistentCollectionChangeData;
import org.hibernate.envers.internal.entities.mapper.id.IdMapper;
import org.hibernate.envers.internal.synchronization.AuditProcess;
import org.hibernate.envers.internal.synchronization.work.AuditWorkUnit;
import org.hibernate.envers.internal.synchronization.work.CollectionChangeWorkUnit;
import org.hibernate.envers.internal.synchronization.work.FakeBidirectionalRelationWorkUnit;
import org.hibernate.envers.internal.synchronization.work.PersistentCollectionChangeWorkUnit;
import org.hibernate.event.spi.AbstractCollectionEvent;
import org.hibernate.persister.collection.AbstractCollectionPersister;

/**
 * Base class for Envers' collection event related listeners
 *
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 * @author Steve Ebersole
 * @author Michal Skowronek (mskowr at o2 dot pl)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
public abstract class BaseEnversCollectionEventListener extends BaseEnversEventListener {
	protected BaseEnversCollectionEventListener(EnversService enversService) {
		super( enversService );
	}

	protected final CollectionEntry getCollectionEntry(AbstractCollectionEvent event) {
		return event.getSession().getPersistenceContext().getCollectionEntry( event.getCollection() );
	}

	protected final void onCollectionAction(
			AbstractCollectionEvent event,
			PersistentCollection newColl,
			Serializable oldColl,
			CollectionEntry collectionEntry) {
		if ( shouldGenerateRevision( event ) ) {
			checkIfTransactionInProgress( event.getSession() );

			final AuditProcess auditProcess = getEnversService().getAuditProcessManager().get( event.getSession() );

			final String entityName = event.getAffectedOwnerEntityName();
			final String ownerEntityName = ( (AbstractCollectionPersister) collectionEntry.getLoadedPersister() ).getOwnerEntityName();
			final String referencingPropertyName = collectionEntry.getRole().substring( ownerEntityName.length() + 1 );

			// Checking if this is not a "fake" many-to-one bidirectional relation. The relation description may be
			// null in case of collections of non-entities.
			final RelationDescription rd = searchForRelationDescription( entityName, referencingPropertyName );
			if ( rd != null && rd.getMappedByPropertyName() != null ) {
				generateFakeBidirecationalRelationWorkUnits(
						auditProcess,
						newColl,
						oldColl,
						entityName,
						referencingPropertyName,
						event,
						rd
				);
			}
			else {
				final PersistentCollectionChangeWorkUnit workUnit = new PersistentCollectionChangeWorkUnit(
						event.getSession(),
						entityName,
						getEnversService(),
						newColl,
						collectionEntry,
						oldColl,
						event.getAffectedOwnerIdOrNull(),
						referencingPropertyName
				);
				auditProcess.addWorkUnit( workUnit );

				if ( workUnit.containsWork() ) {
					// There are some changes: a revision needs also be generated for the collection owner
					auditProcess.addWorkUnit(
							new CollectionChangeWorkUnit(
									event.getSession(),
									event.getAffectedOwnerEntityName(),
									referencingPropertyName,
									getEnversService(),
									event.getAffectedOwnerIdOrNull(),
									event.getAffectedOwnerOrNull()
							)
					);

					generateBidirectionalCollectionChangeWorkUnits( auditProcess, event, workUnit, rd );
				}
			}
		}
	}

	protected final void onCollectionActionInversed(
			AbstractCollectionEvent event,
			PersistentCollection newColl,
			Serializable oldColl,
			CollectionEntry collectionEntry) {
		if ( shouldGenerateRevision( event ) ) {
			final String entityName = event.getAffectedOwnerEntityName();
			final String ownerEntityName = ( (AbstractCollectionPersister) collectionEntry.getLoadedPersister() ).getOwnerEntityName();
			final String referencingPropertyName = collectionEntry.getRole().substring( ownerEntityName.length() + 1 );

			final RelationDescription rd = searchForRelationDescription( entityName, referencingPropertyName );
			if ( rd != null ) {
				if ( rd.getRelationType().equals( RelationType.TO_MANY_NOT_OWNING ) && rd.isIndexed() ) {
					onCollectionAction( event, newColl, oldColl, collectionEntry );
				}
			}
		}
	}

	/**
	 * Forces persistent collection initialization.
	 *
	 * @param event Collection event.
	 *
	 * @return Stored snapshot.
	 */
	protected Serializable initializeCollection(AbstractCollectionEvent event) {
		event.getCollection().forceInitialization();
		return event.getCollection().getStoredSnapshot();
	}

	/**
	 * Checks whether modification of not-owned relation field triggers new revision and owner entity is versioned.
	 *
	 * @param event Collection event.
	 *
	 * @return {@code true} if revision based on given event should be generated, {@code false} otherwise.
	 */
	protected boolean shouldGenerateRevision(AbstractCollectionEvent event) {
		final String entityName = event.getAffectedOwnerEntityName();
		return getEnversService().getGlobalConfiguration().isGenerateRevisionsForCollections()
				&& getEnversService().getEntitiesConfigurations().isVersioned( entityName );
	}

	/**
	 * Looks up a relation description corresponding to the given property in the given entity. If no description is
	 * found in the given entity, the parent entity is checked (so that inherited relations work).
	 *
	 * @param entityName Name of the entity, in which to start looking.
	 * @param referencingPropertyName The name of the property.
	 *
	 * @return A found relation description corresponding to the given entity or {@code null}, if no description can
	 *         be found.
	 */
	private RelationDescription searchForRelationDescription(String entityName, String referencingPropertyName) {
		final EntityConfiguration configuration = getEnversService().getEntitiesConfigurations().get( entityName );
		final String propertyName = sanitizeReferencingPropertyName( referencingPropertyName );
		final RelationDescription rd = configuration.getRelationDescription( propertyName );
		if ( rd == null && configuration.getParentEntityName() != null ) {
			return searchForRelationDescription( configuration.getParentEntityName(), propertyName );
		}

		return rd;
	}

	private String sanitizeReferencingPropertyName(String propertyName) {
		if ( propertyName != null && propertyName.indexOf( '.' ) != -1 ) {
			return propertyName.replaceAll( "\\.", "\\_" );
		}
		return propertyName;
	}

	private void generateFakeBidirecationalRelationWorkUnits(
			AuditProcess auditProcess,
			PersistentCollection newColl,
			Serializable oldColl,
			String collectionEntityName,
			String referencingPropertyName,
			AbstractCollectionEvent event,
			RelationDescription rd) {
		// First computing the relation changes
		final List<PersistentCollectionChangeData> collectionChanges = getEnversService()
				.getEntitiesConfigurations()
				.get( collectionEntityName )
				.getPropertyMapper()
				.mapCollectionChanges(
						event.getSession(),
						referencingPropertyName,
						newColl,
						oldColl,
						event.getAffectedOwnerIdOrNull()
				);

		// Getting the id mapper for the related entity, as the work units generated will correspond to the related
		// entities.
		final String relatedEntityName = rd.getToEntityName();
		final IdMapper relatedIdMapper = getEnversService().getEntitiesConfigurations().get( relatedEntityName ).getIdMapper();

		// For each collection change, generating the bidirectional work unit.
		for ( PersistentCollectionChangeData changeData : collectionChanges ) {
			final Object relatedObj = changeData.getChangedElement();
			final Serializable relatedId = (Serializable) relatedIdMapper.mapToIdFromEntity( relatedObj );
			final RevisionType revType = (RevisionType) changeData.getData().get(
					getEnversService().getAuditEntitiesConfiguration().getRevisionTypePropName()
			);

			// This can be different from relatedEntityName, in case of inheritance (the real entity may be a subclass
			// of relatedEntityName).
			final String realRelatedEntityName = event.getSession().bestGuessEntityName( relatedObj );

			// By default, the nested work unit is a collection change work unit.
			final AuditWorkUnit nestedWorkUnit = new CollectionChangeWorkUnit(
					event.getSession(),
					realRelatedEntityName,
					rd.getMappedByPropertyName(),
					getEnversService(),
					relatedId,
					relatedObj
			);

			auditProcess.addWorkUnit(
					new FakeBidirectionalRelationWorkUnit(
							event.getSession(),
							realRelatedEntityName,
							getEnversService(),
							relatedId,
							referencingPropertyName,
							event.getAffectedOwnerOrNull(),
							rd,
							revType,
							changeData.getChangedElementIndex(),
							nestedWorkUnit
					)
			);
		}

		// We also have to generate a collection change work unit for the owning entity.
		auditProcess.addWorkUnit(
				new CollectionChangeWorkUnit(
						event.getSession(),
						collectionEntityName,
						referencingPropertyName,
						getEnversService(),
						event.getAffectedOwnerIdOrNull(),
						event.getAffectedOwnerOrNull()
				)
		);
	}

	private void generateBidirectionalCollectionChangeWorkUnits(
			AuditProcess auditProcess,
			AbstractCollectionEvent event,
			PersistentCollectionChangeWorkUnit workUnit,
			RelationDescription rd) {
		// Checking if this is enabled in configuration ...
		if ( !getEnversService().getGlobalConfiguration().isGenerateRevisionsForCollections() ) {
			return;
		}

		// Checking if this is not a bidirectional relation - then, a revision needs also be generated for
		// the other side of the relation.
		// relDesc can be null if this is a collection of simple values (not a relation).
		if ( rd != null && rd.isBidirectional() ) {
			final String relatedEntityName = rd.getToEntityName();
			final IdMapper relatedIdMapper = getEnversService().getEntitiesConfigurations().get( relatedEntityName ).getIdMapper();

			final Set<String> toPropertyNames = getEnversService().getEntitiesConfigurations().getToPropertyNames(
					event.getAffectedOwnerEntityName(),
					rd.getFromPropertyName(),
					relatedEntityName
			);
			final String toPropertyName = toPropertyNames.iterator().next();

			for ( PersistentCollectionChangeData changeData : workUnit.getCollectionChanges() ) {
				final Object relatedObj = changeData.getChangedElement();
				final Serializable relatedId = (Serializable) relatedIdMapper.mapToIdFromEntity( relatedObj );

				auditProcess.addWorkUnit(
						new CollectionChangeWorkUnit(
								event.getSession(),
								event.getSession().bestGuessEntityName( relatedObj ),
								toPropertyName,
								getEnversService(),
								relatedId,
								relatedObj
						)
				);
			}
		}
	}
}
