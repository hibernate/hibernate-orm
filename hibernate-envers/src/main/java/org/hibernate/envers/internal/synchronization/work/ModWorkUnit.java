/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.synchronization.work;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ModWorkUnit extends AbstractAuditWorkUnit implements AuditWorkUnit {
	private final Map<String, Object> data;
	private final boolean changes;

	private final EntityPersister entityPersister;
	private final Object[] oldState;
	private final Object[] newState;

	public ModWorkUnit(
			SessionImplementor sessionImplementor,
			String entityName,
			EnversService enversService,
			Serializable id,
			EntityPersister entityPersister,
			Object[] newState,
			Object[] oldState) {
		super( sessionImplementor, entityName, enversService, id, RevisionType.MOD );

		this.entityPersister = entityPersister;
		this.oldState = oldState;
		this.newState = newState;
		this.data = new HashMap<>();
		this.changes = enversService.getEntitiesConfigurations().get( getEntityName() ).getPropertyMapper().map(
				sessionImplementor,
				data,
				entityPersister.getPropertyNames(),
				newState,
				oldState
		);
	}

	public Map<String, Object> getData() {
		return data;
	}

	@Override
	public boolean containsWork() {
		return changes;
	}

	@Override
	public Map<String, Object> generateData(Object revisionData) {
		fillDataWithId( data, revisionData );

		return data;
	}

	@Override
	public AuditWorkUnit merge(AddWorkUnit second) {
		return this;
	}

	@Override
	public AuditWorkUnit merge(ModWorkUnit second) {
		// In case of multiple subsequent flushes within single transaction, modification flags need to be
		// recalculated against initial and final state of the given entity.
		return new ModWorkUnit(
				second.sessionImplementor,
				second.getEntityName(),
				second.enversService,
				second.id,
				second.entityPersister,
				second.newState,
				this.oldState
		);
	}

	@Override
	public AuditWorkUnit merge(DelWorkUnit second) {
		return second;
	}

	@Override
	public AuditWorkUnit merge(CollectionChangeWorkUnit second) {
		second.mergeCollectionModifiedData( data );
		return this;
	}

	@Override
	public AuditWorkUnit merge(FakeBidirectionalRelationWorkUnit second) {
		return FakeBidirectionalRelationWorkUnit.merge( second, this, second.getNestedWorkUnit() );
	}

	@Override
	public AuditWorkUnit dispatch(WorkUnitMergeVisitor first) {
		return first.merge( this );
	}
}
