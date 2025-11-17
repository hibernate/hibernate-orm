/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper;

import java.util.Map;

import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public interface CompositeMapperBuilder extends SimpleMapperBuilder {
	CompositeMapperBuilder addComponent(
			PropertyData propertyData,
			Class componentClass, EmbeddableInstantiator instantiator);

	void addComposite(PropertyData propertyData, PropertyMapper propertyMapper);

	Map<PropertyData, PropertyMapper> getProperties();
}
