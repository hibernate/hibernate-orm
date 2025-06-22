/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.internal;

import java.util.Map;

import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Steve Ebersole
 * @author Gavin King
 */
public class PropertyAccessStrategyMapImpl implements PropertyAccessStrategy {
	/**
	 * Singleton access
	 */
	public static final PropertyAccessStrategyMapImpl INSTANCE = new PropertyAccessStrategyMapImpl();

	@Override
	public PropertyAccess buildPropertyAccess(@Nullable Class<?> containerJavaType, String propertyName, boolean setterRequired) {

		// Sometimes containerJavaType is null, but if it isn't, make sure it's a Map.
		if (containerJavaType != null && !Map.class.isAssignableFrom( containerJavaType)) {
			throw new IllegalArgumentException(
				String.format(
					"Expecting class: [%1$s], but containerJavaType is of type: [%2$s] for propertyName: [%3$s]",
					Map.class.getName(),
					containerJavaType.getName(),
					propertyName
				)
			);
		}

		return new PropertyAccessMapImpl( this, propertyName );
	}
}
