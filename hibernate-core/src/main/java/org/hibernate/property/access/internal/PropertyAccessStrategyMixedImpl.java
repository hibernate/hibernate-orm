/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.internal;

import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;

/**
 * A PropertyAccessStrategy that selects between available getter/setter method and/or field.
 *
 * @author Steve Ebersole
 */
public class PropertyAccessStrategyMixedImpl implements PropertyAccessStrategy {
	/**
	 * Singleton access
	 */
	public static final PropertyAccessStrategyMixedImpl INSTANCE = new PropertyAccessStrategyMixedImpl();

	@Override
	public PropertyAccess buildPropertyAccess(Class<?> containerJavaType, String propertyName, boolean setterRequired) {
		return new PropertyAccessMixedImpl( this, containerJavaType, propertyName );
	}
}
