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

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class CollectionChangeWorkUnit extends AbstractAuditWorkUnit implements AuditWorkUnit {
	private Object entity;
	private final String collectionPropertyName;
	private final Map<String, Object> data = new HashMap<String, Object>();

	public CollectionChangeWorkUnit(
			SessionImplementor session,
			String entityName,
			String collectionPropertyName,
			EnversService enversService,
			Serializable id,
			Object entity) {
		super( session, entityName, enversService, id, RevisionType.MOD );

		this.entity = entity;
		this.collectionPropertyName = collectionPropertyName;
	}

	@Override
	public boolean containsWork() {
		return true;
	}

	@Override
	public Map<String, Object> generateData(Object revisionData) {
		fillDataWithId( data, revisionData );
		final Map<String, Object> preGenerateData = new HashMap<String, Object>( data );
		enversService.getEntitiesConfigurations().get( getEntityName() ).getPropertyMapper().mapToMapFromEntity(
				sessionImplementor,
				data,
				entity,
				null
		);
		enversService.getEntitiesConfigurations().get( getEntityName() ).getPropertyMapper().mapModifiedFlagsToMapFromEntity(
				sessionImplementor,
				data,
				entity,
				entity
		);
		enversService.getEntitiesConfigurations().get( getEntityName() ).getPropertyMapper().mapModifiedFlagsToMapForCollectionChange(
				collectionPropertyName,
				data
		);
		data.putAll( preGenerateData );
		return data;
	}

	public void mergeCollectionModifiedData(Map<String, Object> data) {
		enversService.getEntitiesConfigurations().get( getEntityName() ).getPropertyMapper().mapModifiedFlagsToMapForCollectionChange(
				collectionPropertyName,
				data
		);
	}

	@Override
	public AuditWorkUnit merge(AddWorkUnit second) {
		return second;
	}

	@Override
	public AuditWorkUnit merge(ModWorkUnit second) {
		mergeCollectionModifiedData( second.getData() );
		return second;
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
		return second;
	}

	@Override
	public AuditWorkUnit dispatch(WorkUnitMergeVisitor first) {
		return first.merge( this );
	}
}
