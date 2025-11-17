/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.relation.component;

import java.util.Map;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.internal.entities.EntityInstantiator;
import org.hibernate.envers.internal.tools.query.Parameters;

/**
 * A component mapper for the @MapKey mapping with the name parameter specified: the value of the map's key
 * is a property of the entity. This doesn't have an effect on the data stored in the versions tables,
 * so <code>mapToMapFromObject</code> is empty.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class MiddleMapKeyPropertyComponentMapper extends AbstractMiddleComponentMapper {
	private final String propertyName;
	private final String accessType;

	public MiddleMapKeyPropertyComponentMapper(String propertyName, String accessType) {
		this.propertyName = propertyName;
		this.accessType = accessType;
	}

	@Override
	public Object mapToObjectFromFullMap(
			final EntityInstantiator entityInstantiator,
			final Map<String, Object> data,
			final Object dataObject,
			Number revision) {
		// dataObject is not null, as this mapper can only be used in an index.
		return getValueFromObject(
				propertyName,
				accessType,
				dataObject,
				entityInstantiator.getEnversService().getServiceRegistry()
		);
	}

	@Override
	public void mapToMapFromObject(
			SharedSessionContractImplementor session,
			Map<String, Object> idData,
			Map<String, Object> data,
			Object obj) {
		// Doing nothing.
	}

	@Override
	public void addMiddleEqualToQuery(
			Parameters parameters,
			String idPrefix1,
			String prefix1,
			String idPrefix2,
			String prefix2) {
		// Doing nothing.
	}
}
