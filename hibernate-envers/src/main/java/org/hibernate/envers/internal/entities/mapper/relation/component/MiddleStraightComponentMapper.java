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
package org.hibernate.envers.internal.entities.mapper.relation.component;

import java.util.Map;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.internal.entities.EntityInstantiator;
import org.hibernate.envers.internal.tools.query.Parameters;

/**
 * A mapper for reading and writing a property straight to/from maps. This mapper cannot be used with middle tables,
 * but only with "fake" bidirectional indexed relations.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public final class MiddleStraightComponentMapper implements MiddleComponentMapper {
	private final String propertyName;

	public MiddleStraightComponentMapper(String propertyName) {
		this.propertyName = propertyName;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public Object mapToObjectFromFullMap(
			EntityInstantiator entityInstantiator, Map<String, Object> data,
			Object dataObject, Number revision) {
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
