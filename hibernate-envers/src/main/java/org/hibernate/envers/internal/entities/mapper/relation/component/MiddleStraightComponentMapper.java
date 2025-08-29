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
 * A mapper for reading and writing a property straight to/from maps. This mapper cannot be used with middle tables,
 * but only with "fake" bidirectional indexed relations.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public final class MiddleStraightComponentMapper extends AbstractMiddleComponentMapper {
	private final String propertyName;

	public MiddleStraightComponentMapper(String propertyName) {
		this.propertyName = propertyName;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object mapToObjectFromFullMap(
			EntityInstantiator entityInstantiator, Map<String, Object> data,
			Object dataObject, Number revision) {
		return data.get( propertyName );
	}

	@Override
	public void mapToMapFromObject(
			SharedSessionContractImplementor session,
			Map<String, Object> idData,
			Map<String, Object> data,
			Object obj) {
		idData.put( propertyName, obj );
	}

	@Override
	public void addMiddleEqualToQuery(
			Parameters parameters,
			String idPrefix1,
			String prefix1,
			String idPrefix2,
			String prefix2) {
		throw new UnsupportedOperationException( "Cannot use this mapper with a middle table!" );
	}
}
