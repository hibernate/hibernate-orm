/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.synchronization.work;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.AuditService;
import org.hibernate.envers.internal.tools.ArraysTools;
import org.hibernate.envers.internal.tools.EntityTools;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
public class DelWorkUnit extends AbstractAuditWorkUnit implements AuditWorkUnit {
	private final Object[] state;
	private final EntityTypeDescriptor entityDescriptor;
	private final String[] propertyNames;

	public DelWorkUnit(
			SessionImplementor sessionImplementor,
			String entityName,
			AuditService auditService,
			Object id,
			EntityTypeDescriptor entityDescriptor,
			Object[] state) {
		super( sessionImplementor, entityName, auditService, id, RevisionType.DEL );

		this.state = state;
		this.entityDescriptor = entityDescriptor;
		this.propertyNames = EntityTools.getPropertyNames( entityDescriptor );
	}

	@Override
	public boolean containsWork() {
		return true;
	}

	@Override
	public Map<String, Object> generateData(Object revisionData) {
		final Map<String, Object> data = new HashMap<>();
		fillDataWithId( data, revisionData );

		if ( auditService.getOptions().isStoreDataAtDeleteEnabled() ) {
			auditService.getEntityBindings().get( getEntityName() ).getPropertyMapper().map(
					sessionImplementor,
					data,
					propertyNames,
					state,
					state
			);
		}
		else {
			auditService.getEntityBindings().get( getEntityName() ).getPropertyMapper().map(
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
		return new ModWorkUnit(
				sessionImplementor,
				entityName,
				auditService,
				id,
				entityDescriptor,
				second.getState(),
				state
		);
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
