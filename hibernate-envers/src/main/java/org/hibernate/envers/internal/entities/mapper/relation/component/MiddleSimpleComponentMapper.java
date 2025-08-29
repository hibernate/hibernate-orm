/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.relation.component;

import java.util.Map;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.internal.entities.EntityInstantiator;
import org.hibernate.envers.internal.tools.query.Parameters;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public final class MiddleSimpleComponentMapper extends AbstractMiddleComponentMapper {
	private final Configuration configuration;
	private final String propertyName;

	public MiddleSimpleComponentMapper(Configuration configuration, String propertyName) {
		this.configuration = configuration;
		this.propertyName = propertyName;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object mapToObjectFromFullMap(
			EntityInstantiator entityInstantiator, Map<String, Object> data,
			Object dataObject, Number revision) {
		return ( (Map<String, Object>) data.get( configuration.getOriginalIdPropertyName() ) ).get( propertyName );
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
		parameters.addWhere( idPrefix1 + "." + propertyName, false, "=", idPrefix2 + "." + propertyName, false );
	}
}
