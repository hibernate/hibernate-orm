/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.mapping.Backref;
import org.hibernate.mapping.IndexBackref;
import org.hibernate.mapping.Property;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyIndexBackRefImpl;
import org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies;
import org.hibernate.property.access.spi.PropertyAccessStrategy;

import static org.hibernate.internal.util.StringHelper.isNotEmpty;

class PropertyAccessHelper {
	static PropertyAccessStrategy propertyAccessStrategy(
			Property bootAttributeDescriptor,
			Class<?> mappedClass,
			StrategySelector strategySelector) {
		final var strategy = bootAttributeDescriptor.getPropertyAccessStrategy( mappedClass );
		if ( strategy != null ) {
			return strategy;
		}
		else {
			final String propertyAccessorName = bootAttributeDescriptor.getPropertyAccessorName();
			if ( isNotEmpty( propertyAccessorName ) ) {
				// handle explicitly specified attribute accessor
				return strategySelector.resolveStrategy( PropertyAccessStrategy.class, propertyAccessorName );
			}
			else {
				if ( bootAttributeDescriptor instanceof Backref backref ) {
					return new PropertyAccessStrategyBackRefImpl(
							backref.getCollectionRole(),
							backref.getEntityName()
					);
				}
				else if ( bootAttributeDescriptor instanceof IndexBackref indexBackref ) {
					return new PropertyAccessStrategyIndexBackRefImpl(
							indexBackref.getCollectionRole(),
							indexBackref.getEntityName()
					);
				}
				else {
					// for now...
					return BuiltInPropertyAccessStrategies.MIXED.getStrategy();
				}
			}
		}
	}
}
