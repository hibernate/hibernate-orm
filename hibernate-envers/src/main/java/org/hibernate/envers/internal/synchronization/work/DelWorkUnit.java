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
import org.hibernate.envers.internal.tools.ArraysTools;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class DelWorkUnit extends AbstractAuditWorkUnit implements AuditWorkUnit {
	private final Object[] state;
	private final EntityPersister entityPersister;
	private final String[] propertyNames;

	public DelWorkUnit(
			SessionImplementor sessionImplementor,
			String entityName,
			EnversService enversService,
			Serializable id,
			EntityPersister entityPersister,
			Object[] state) {
		super( sessionImplementor, entityName, enversService, id, RevisionType.DEL );

		this.state = state;
		this.entityPersister = entityPersister;
		this.propertyNames = entityPersister.getPropertyNames();
	}

	@Override
	public boolean containsWork() {
		return true;
	}

	@Override
	public Map<String, Object> generateData(Object revisionData) {
		final Map<String, Object> data = new HashMap<>();
		fillDataWithId( data, revisionData );

		if ( enversService.getGlobalConfiguration().isStoreDataAtDelete() ) {
			enversService.getEntitiesConfigurations().get( getEntityName() ).getPropertyMapper().map(
					sessionImplementor,
					data,
					propertyNames,
					state,
					state
			);
		}
		else {
			enversService.getEntitiesConfigurations().get( getEntityName() ).getPropertyMapper().map(
					sessionImplementor,
					data,
					propertyNames,
					null,
					state
			);
		}

		return data;
	}

	@Override
	public AuditWorkUnit merge(AddWorkUnit second) {
		if ( ArraysTools.arraysEqual( second.getState(), state ) ) {
			// Return null if object's state has not changed.
			return null;
		}
		return new ModWorkUnit( sessionImplementor, entityName, enversService, id, entityPersister, second.getState(), state );
	}

	@Override
	public AuditWorkUnit merge(ModWorkUnit second) {
		return null;
	}

	@Override
	public AuditWorkUnit merge(DelWorkUnit second) {
		return this;
	}

	@Override
	public AuditWorkUnit merge(CollectionChangeWorkUnit second) {
		return this;
	}

	@Override
	public AuditWorkUnit merge(FakeBidirectionalRelationWorkUnit second) {
		return this;
	}

	@Override
	public AuditWorkUnit dispatch(WorkUnitMergeVisitor first) {
		return first.merge( this );
	}
}
