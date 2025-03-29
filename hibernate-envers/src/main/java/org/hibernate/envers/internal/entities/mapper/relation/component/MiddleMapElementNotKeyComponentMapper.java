/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.relation.component;

import java.util.Map;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.internal.entities.EntityInstantiator;
import org.hibernate.envers.internal.tools.query.Parameters;

/**
 * A middle table component mapper which assigns a Map-type's element as part
 * of the data-portion of the mapping rather than the identifier.
 * <p>
 * This is useful for mappings where the database does not support CLOB or NCLOB
 * data types as part of the primary key for the table.
 * </p>
 * An example:
 * <pre>
 *     &#64;ElementCollection
 *     &#64;Lob
 *     private Map&lt;String, String&gt; values;
 * </pre>
 *
 * @author Chris Cranford
 */
public class MiddleMapElementNotKeyComponentMapper extends AbstractMiddleComponentMapper {
	private final String propertyName;

	public MiddleMapElementNotKeyComponentMapper(String propertyName) {
		this.propertyName = propertyName;
	}

	@Override
	@SuppressWarnings("unchecked")
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
		data.put( propertyName, obj );
	}

	@Override
	public void addMiddleEqualToQuery(
			Parameters parameters,
			String idPrefix1,
			String prefix1,
			String idPrefix2,
			String prefix2) {
		parameters.addWhere( prefix1 + "." + propertyName, false, "=", prefix2 + "." + propertyName, false );
	}
}
