/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.id;

import org.hibernate.envers.internal.entities.PropertyData;

import java.util.Map;

/**
 * Any id mapper that contains a set of composite ids
 *
 * @author Matthew Morrissette (yinzara at gmail dot com)
 */
public interface CompositeIdMapper extends IdMapper {


	/**
	 * @return An unmodifable set of the individiual ids of this composite id
	 */
	Iterable<Map.Entry<PropertyData, SingleIdMapper>> getIds();

	/**
	 * If exists, the class that represents the composite id of this entity
	 *
	 * @return
	 */
	Class getCompositeIdClass();
}
