/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper;

import java.util.Map;

import org.hibernate.envers.internal.entities.PropertyData;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public interface CompositeMapperBuilder extends SimpleMapperBuilder {
	public CompositeMapperBuilder addComponent(PropertyData propertyData, Class componentClass);

	public void addComposite(PropertyData propertyData, PropertyMapper propertyMapper);

	public Map<PropertyData, PropertyMapper> getProperties();
}
