/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.relation.component;

import java.util.Map;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.internal.entities.EntityInstantiator;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.internal.tools.query.Parameters;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public final class MiddleRelatedComponentMapper extends AbstractMiddleComponentMapper {
	private final MiddleIdData relatedIdData;

	public MiddleRelatedComponentMapper(MiddleIdData relatedIdData) {
		this.relatedIdData = relatedIdData;
	}

	@Override
	public Object mapToObjectFromFullMap(
			EntityInstantiator entityInstantiator, Map<String, Object> data,
			Object dataObject, Number revision) {
		return entityInstantiator.createInstanceFromVersionsEntity( relatedIdData.getEntityName(), data, revision );
	}

	@Override
	public void mapToMapFromObject(
			SharedSessionContractImplementor session,
			Map<String, Object> idData,
			Map<String, Object> data,
			Object obj) {
		relatedIdData.getPrefixedMapper().mapToMapFromEntity( idData, obj );
	}

	@Override
	public void addMiddleEqualToQuery(
			Parameters parameters,
			String idPrefix1,
			String prefix1,
			String idPrefix2,
			String prefix2) {
		relatedIdData.getPrefixedMapper().addIdsEqualToQuery( parameters, idPrefix1, idPrefix2 );
	}
}
