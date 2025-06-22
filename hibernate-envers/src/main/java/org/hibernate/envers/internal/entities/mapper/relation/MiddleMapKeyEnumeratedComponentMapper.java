/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.relation;

import java.util.Map;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.internal.entities.EntityInstantiator;
import org.hibernate.envers.internal.entities.mapper.relation.component.MiddleComponentMapper;
import org.hibernate.envers.internal.tools.query.Parameters;

/**
 * A {@link MiddleComponentMapper} specifically for {@link jakarta.persistence.MapKeyEnumerated}.
 *
 * @author Chris Cranford
 */
public class MiddleMapKeyEnumeratedComponentMapper implements MiddleComponentMapper {
	private final String propertyName;

	public MiddleMapKeyEnumeratedComponentMapper(String propertyPrefix) {
		this.propertyName = propertyPrefix + "_KEY";
	}

	@Override
	public Object mapToObjectFromFullMap(
			EntityInstantiator entityInstantiator,
			Map<String, Object> data,
			Object dataObject,
			Number revision) {
		return data.get( propertyName );
	}

	@Override
	public void mapToMapFromObject(
			SessionImplementor session,
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
