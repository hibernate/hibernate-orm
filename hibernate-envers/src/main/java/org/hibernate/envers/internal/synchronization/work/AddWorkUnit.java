/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.synchronization.work;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.mapper.ExtendedPropertyMapper;
import org.hibernate.envers.internal.tools.ArraysTools;
import org.hibernate.persister.entity.EntityPersister;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class AddWorkUnit extends AbstractAuditWorkUnit implements AuditWorkUnit {
	private final Object[] state;
	private final Map<String, Object> data;

	public AddWorkUnit(
			SharedSessionContractImplementor sessionImplementor,
			String entityName,
			EnversService enversService,
			Object id, EntityPersister entityPersister, Object[] state) {
		super( sessionImplementor, entityName, enversService, id, RevisionType.ADD );

		this.data = new HashMap<>();
		this.state = state;
		this.enversService.getEntitiesConfigurations().get( getEntityName() ).getPropertyMapper().map(
				sessionImplementor,
				data,
				entityPersister.getPropertyNames(),
				state,
				null
		);
	}

	public AddWorkUnit(
			SharedSessionContractImplementor sessionImplementor,
			String entityName,
			EnversService enversService,
			Object id,
			Map<String, Object> data) {
		super( sessionImplementor, entityName, enversService, id, RevisionType.ADD );

		this.data = data;
		final String[] propertyNames = sessionImplementor.getFactory()
				.getMappingMetamodel()
				.getEntityDescriptor( getEntityName() )
				.getPropertyNames();
		this.state = ArraysTools.mapToArray( data, propertyNames );
	}

	@Override
	public boolean containsWork() {
		return true;
	}

	@Override
	public Map<String, Object> generateData(Object revisionData) {
		fillDataWithId( data, revisionData );
		return data;
	}

	public Object[] getState() {
		return state;
	}

	@Override
	public AuditWorkUnit merge(AddWorkUnit second) {
		return second;
	}

	@Override
	public AuditWorkUnit merge(ModWorkUnit second) {
		return new AddWorkUnit(
				sessionImplementor,
				entityName,
				enversService,
				id,
				mergeModifiedFlags( data, second.getData() )
		);
	}

	@Override
	public AuditWorkUnit merge(DelWorkUnit second) {
		return null;
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

	private Map<String, Object> mergeModifiedFlags(Map<String, Object> lhs, Map<String, Object> rhs) {
		final ExtendedPropertyMapper mapper = enversService.getEntitiesConfigurations().get( getEntityName() ).getPropertyMapper();
		// Designed to take any lhs modified flag values of true and merge those into the data set for the rhs
		// This makes sure that when merging ModAuditWork with AddWorkUnit within the same transaction for the
		// same entity that the modified flags are tracked correctly.
		for ( PropertyData propertyData : mapper.getProperties().keySet() ) {
			if ( propertyData.isUsingModifiedFlag() && !propertyData.isSynthetic() ) {
				Boolean lhsValue = (Boolean) lhs.get( propertyData.getModifiedFlagPropertyName() );
				if ( lhsValue != null && lhsValue ) {
					Boolean rhsValue = (Boolean) rhs.get( propertyData.getModifiedFlagPropertyName() );
					if ( rhsValue == null || !rhsValue ) {
						rhs.put( propertyData.getModifiedFlagPropertyName(), true );
					}
				}
			}
		}
		return rhs;
	}
}
