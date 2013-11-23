/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers.internal.synchronization.work;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
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
			SessionImplementor sessionImplementor, String entityName, AuditConfiguration verCfg,
			Serializable id, EntityPersister entityPersister, Object[] newState, Object[] oldState) {
		super( sessionImplementor, entityName, verCfg, id, RevisionType.MOD );

		this.entityPersister = entityPersister;
		this.oldState = oldState;
		this.newState = newState;
		data = new HashMap<String, Object>();
		changes = verCfg.getEntCfg().get( getEntityName() ).getPropertyMapper().map(
				sessionImplementor, data,
				entityPersister.getPropertyNames(), newState, oldState
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
				second.sessionImplementor, second.getEntityName(), second.verCfg, second.id,
				second.entityPersister, second.newState, this.oldState
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
