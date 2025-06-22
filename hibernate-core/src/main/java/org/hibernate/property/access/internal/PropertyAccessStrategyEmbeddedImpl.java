/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.internal;

import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;

/**
 * A {@link PropertyAccessStrategy} that deals with non-aggregated composites.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class PropertyAccessStrategyEmbeddedImpl implements PropertyAccessStrategy {
	/**
	 * Singleton access
	 */
	public static final PropertyAccessStrategyEmbeddedImpl INSTANCE = new PropertyAccessStrategyEmbeddedImpl();

	@Override
	public PropertyAccess buildPropertyAccess(Class<?> containerJavaType, String propertyName, boolean setterRequired) {
		return new PropertyAccessEmbeddedImpl( this, containerJavaType, propertyName );
	}
}
