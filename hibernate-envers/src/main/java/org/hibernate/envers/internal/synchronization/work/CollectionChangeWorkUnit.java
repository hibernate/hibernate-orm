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

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class CollectionChangeWorkUnit extends AbstractAuditWorkUnit implements AuditWorkUnit {
	private Object entity;
	private final String collectionPropertyName;
	private final Map<String, Object> data = new HashMap<String, Object>();

	public CollectionChangeWorkUnit(
			SessionImplementor session, String entityName, String collectionPropertyName,
			AuditConfiguration verCfg, Serializable id, Object entity) {
		super( session, entityName, verCfg, id, RevisionType.MOD );

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
		verCfg.getEntCfg().get( getEntityName() ).getPropertyMapper()
				.mapToMapFromEntity( sessionImplementor, data, entity, null );
		verCfg.getEntCfg().get( getEntityName() ).getPropertyMapper()
				.mapModifiedFlagsToMapFromEntity( sessionImplementor, data, entity, entity );
		verCfg.getEntCfg().get( getEntityName() ).getPropertyMapper()
				.mapModifiedFlagsToMapForCollectionChange( collectionPropertyName, data );
		data.putAll( preGenerateData );
		return data;
	}

	public void mergeCollectionModifiedData(Map<String, Object> data) {
		verCfg.getEntCfg().get( getEntityName() ).getPropertyMapper().mapModifiedFlagsToMapForCollectionChange(
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
