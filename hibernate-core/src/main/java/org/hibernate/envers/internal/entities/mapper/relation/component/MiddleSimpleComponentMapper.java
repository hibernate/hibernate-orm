/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation.component;

import java.util.Map;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.internal.entities.EntityInstantiator;
import org.hibernate.envers.internal.tools.query.Parameters;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public final class MiddleSimpleComponentMapper implements MiddleComponentMapper {
	private final String propertyName;
	private final String originalIdPropertyName;

	public MiddleSimpleComponentMapper(String originalIdPropertyName, String propertyName) {
		this.propertyName = propertyName;
		this.originalIdPropertyName = originalIdPropertyName;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public Object mapToObjectFromFullMap(
			EntityInstantiator entityInstantiator, Map<String, Object> data,
			Object dataObject, Number revision) {
		return ( (Map<String, Object>) data.get( originalIdPropertyName ) ).get( propertyName );
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
		parameters.addWhere( idPrefix1 + "." + propertyName, false, "=", idPrefix2 + "." + propertyName, false );
	}
}
