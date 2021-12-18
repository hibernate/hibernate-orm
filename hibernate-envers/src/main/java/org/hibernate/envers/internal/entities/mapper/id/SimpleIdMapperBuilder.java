/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.id;

import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.mapper.SimpleMapperBuilder;

/**
 * A simple identifier builder contract.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public interface SimpleIdMapperBuilder extends IdMapper, SimpleMapperBuilder {
	/**
	 * Add a custom identifier mapper to the builder.
	 *
	 * @param propertyData the property data
	 * @param idMapper the mapper
	 */
	void add(PropertyData propertyData, AbstractIdMapper idMapper);
}
