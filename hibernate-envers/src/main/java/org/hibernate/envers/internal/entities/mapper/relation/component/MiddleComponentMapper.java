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
 * @author Adam Warski (adam at warski dot org)
 */
public interface MiddleComponentMapper {
	/**
	 * Maps from full object data, contained in the given map (or object representation of the map, if
	 * available), to an object.
	 *
	 * @param entityInstantiator An entity instatiator bound with an open versions reader.
	 * @param data Full object data.
	 * @param dataObject An optional object representation of the data.
	 * @param revision Revision at which the data is read.
	 *
	 * @return An object with data corresponding to the one found in the given map.
	 */
	Object mapToObjectFromFullMap(
			EntityInstantiator entityInstantiator, Map<String, Object> data,
			Object dataObject, Number revision);

	/**
	 * Maps from an object to the object's map representation (for an entity - only its id).
	 *
	 * @param session The current session.
	 * @param idData Map to which composite-id data should be added.
	 * @param data Map to which data should be added.
	 * @param obj Object to map from.
	 */
	void mapToMapFromObject(
			SessionImplementor session,
			Map<String, Object> idData,
			Map<String, Object> data,
			Object obj);

	/**
	 * Adds query statements, which contains restrictions, which express the property that part of the middle
	 * entity with alias prefix1, is equal to part of the middle entity with alias prefix2 (the entity is the same).
	 * The part is the component's representation in the middle entity.
	 *
	 * @param parameters Parameters, to which to add the statements.
	 * @param idPrefix1 First alias of the entity + prefix + id to add to the properties.
	 * @param prefix1 First alias of the entity + prefix to add to the properties.
	 * @param idPrefix2 Second alias of the entity + prefix + id to add to the properties.
	 * @param prefix2 Second alias of the entity + prefix to add to the properties.
	 */
	void addMiddleEqualToQuery(
			Parameters parameters,
			String idPrefix1,
			String prefix1,
			String idPrefix2,
			String prefix2);
}
