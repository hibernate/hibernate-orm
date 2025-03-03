/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.internal;

import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;

/**
 * A PropertyAccessStrategy that selects between available getter method or field.
 *
 * @author Gavin King
 */
public class PropertyAccessStrategyGetterImpl implements PropertyAccessStrategy {
	/**
	 * Singleton access
	 */
	public static final PropertyAccessStrategyGetterImpl INSTANCE = new PropertyAccessStrategyGetterImpl();

	@Override
	public PropertyAccess buildPropertyAccess(Class<?> containerJavaType, String propertyName, boolean setterRequired) {
		return new PropertyAccessGetterImpl( this, containerJavaType, propertyName );
	}
}
