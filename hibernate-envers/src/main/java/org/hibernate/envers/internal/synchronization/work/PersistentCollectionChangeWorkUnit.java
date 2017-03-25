/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.synchronization.work;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.envers.internal.entities.mapper.PersistentCollectionChangeData;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class PersistentCollectionChangeWorkUnit extends AbstractAuditWorkUnit implements AuditWorkUnit {
	private final List<PersistentCollectionChangeData> collectionChanges;
	private final String referencingPropertyName;

	public PersistentCollectionChangeWorkUnit(
			SessionImplementor sessionImplementor,
			String entityName,
			EnversService enversService,
			PersistentCollection collection,
			CollectionEntry collectionEntry,
			Serializable snapshot,
			Serializable id,
			String referencingPropertyName) {
		super(
				sessionImplementor,
				entityName,
				enversService,
				new PersistentCollectionChangeWorkUnitId( id, collectionEntry.getRole() ),
				RevisionType.MOD
		);

		this.referencingPropertyName = referencingPropertyName;

		collectionChanges = enversService.getEntitiesConfigurations().get( getEntityName() ).getPropertyMapper()
				.mapCollectionChanges( sessionImplementor, referencingPropertyName, collection, snapshot, id );
	}

	public PersistentCollectionChangeWorkUnit(
			SessionImplementor sessionImplementor,
			String entityName,
			EnversService enversService,
			Serializable id,
			List<PersistentCollectionChangeData> collectionChanges,
			String referencingPropertyName) {
		super( sessionImplementor, entityName, enversService, id, RevisionType.MOD );

		this.collectionChanges = collectionChanges;
		this.referencingPropertyName = referencingPropertyName;
	}

	@Override
	public boolean containsWork() {
		return collectionChanges != null && collectionChanges.size() != 0;
	}

	@Override
	public Map<String, Object> generateData(Object revisionData) {
		throw new UnsupportedOperationException( "Cannot generate data for a collection change work unit!" );
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public void perform(Session session, Object revisionData) {
		final AuditEntitiesConfiguration entitiesCfg = enversService.getAuditEntitiesConfiguration();

		for ( PersistentCollectionChangeData persistentCollectionChangeData : collectionChanges ) {
			// Setting the revision number
			( (Map<String, Object>) persistentCollectionChangeData.getData().get( entitiesCfg.getOriginalIdPropName() ) )
					.put( entitiesCfg.getRevisionFieldName(), revisionData );

			auditStrategy.performCollectionChange(
					session,
					getEntityName(),
					referencingPropertyName,
					enversService,
					persistentCollectionChangeData,
					revisionData
			);
		}
	}

	public String getReferencingPropertyName() {
		return referencingPropertyName;
	}

	public List<PersistentCollectionChangeData> getCollectionChanges() {
		return collectionChanges;
	}

	@Override
	public AuditWorkUnit merge(AddWorkUnit second) {
		return null;
	}

	@Override
	public AuditWorkUnit merge(ModWorkUnit second) {
		return null;
	}

	@Override
	public AuditWorkUnit merge(DelWorkUnit second) {
		return null;
	}

	@Override
	public AuditWorkUnit merge(CollectionChangeWorkUnit second) {
		return null;
	}

	@Override
	public AuditWorkUnit merge(FakeBidirectionalRelationWorkUnit second) {
		return null;
	}

	@Override
	public AuditWorkUnit dispatch(WorkUnitMergeVisitor first) {
		if ( first instanceof PersistentCollectionChangeWorkUnit ) {
			final PersistentCollectionChangeWorkUnit original = (PersistentCollectionChangeWorkUnit) first;

			// Merging the collection changes in both work units.

			// First building a map from the ids of the collection-entry-entities from the "second" collection changes,
			// to the PCCD objects. That way, we will be later able to check if an "original" collection change
			// should be added, or if it is overshadowed by a new one.
			final Map<Object, PersistentCollectionChangeData> newChangesIdMap = new HashMap<>();
			for ( PersistentCollectionChangeData persistentCollectionChangeData : getCollectionChanges() ) {
				newChangesIdMap.put(
						getOriginalId( persistentCollectionChangeData ),
						persistentCollectionChangeData
				);
			}

			// This will be the list with the resulting (merged) changes.
			final List<PersistentCollectionChangeData> mergedChanges = new ArrayList<>();

			// Including only those original changes, which are not overshadowed by new ones.
			for ( PersistentCollectionChangeData originalCollectionChangeData : original.getCollectionChanges() ) {
				final Object originalOriginalId = getOriginalId( originalCollectionChangeData );
				if ( !newChangesIdMap.containsKey( originalOriginalId ) ) {
					mergedChanges.add( originalCollectionChangeData );
				}
				else {
					// If the changes collide, checking if the first one isn't a DEL, and the second a subsequent ADD
					// If so, removing the change alltogether.
					final String revTypePropName = enversService.getAuditEntitiesConfiguration().getRevisionTypePropName();
					if ( RevisionType.ADD.equals( newChangesIdMap.get( originalOriginalId ).getData().get( revTypePropName ) )
							&& RevisionType.DEL.equals( originalCollectionChangeData.getData().get( revTypePropName ) ) ) {
						newChangesIdMap.remove( originalOriginalId );
					}
				}
			}

			// Finally adding all of the new changes to the end of the list (the map values may differ from
			// getCollectionChanges() because of the last operation above).
			mergedChanges.addAll( newChangesIdMap.values() );

			return new PersistentCollectionChangeWorkUnit(
					sessionImplementor,
					entityName,
					enversService,
					id,
					mergedChanges,
					referencingPropertyName
			);
		}
		else {
			throw new RuntimeException(
					"Trying to merge a " + first + " with a PersitentCollectionChangeWorkUnit. " +
							"This is not really possible."
			);
		}
	}

	private Object getOriginalId(PersistentCollectionChangeData persistentCollectionChangeData) {
		return persistentCollectionChangeData.getData().get( enversService.getAuditEntitiesConfiguration().getOriginalIdPropName() );
	}

	/**
	 * A unique identifier for a collection work unit. Consists of an id of the owning entity and the name of
	 * the entity plus the name of the field (the role). This is needed because such collections aren't entities
	 * in the "normal" mapping, but they are entities for Envers.
	 */
	public static class PersistentCollectionChangeWorkUnitId implements Serializable {
		private static final long serialVersionUID = -8007831518629167537L;

		private final Serializable ownerId;
		private final String role;

		public PersistentCollectionChangeWorkUnitId(Serializable ownerId, String role) {
			this.ownerId = ownerId;
			this.role = role;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			final PersistentCollectionChangeWorkUnitId that = (PersistentCollectionChangeWorkUnitId) o;

			if ( ownerId != null ? !ownerId.equals( that.ownerId ) : that.ownerId != null ) {
				return false;
			}
			//noinspection RedundantIfStatement
			if ( role != null ? !role.equals( that.role ) : that.role != null ) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = ownerId != null ? ownerId.hashCode() : 0;
			result = 31 * result + (role != null ? role.hashCode() : 0);
			return result;
		}

		public Serializable getOwnerId() {
			return ownerId;
		}
	}
}
