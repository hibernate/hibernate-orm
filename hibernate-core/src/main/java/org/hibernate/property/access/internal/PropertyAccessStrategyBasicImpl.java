/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.internal;

import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.PropertyAccessorService;

/**
 * Defines a strategy for accessing property values via a get/set pair, which may be nonpublic.  This
 * is the default (and recommended) strategy.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class PropertyAccessStrategyBasicImpl implements PropertyAccessStrategy {
	/**
	 * Singleton access
	 */
	public static final PropertyAccessStrategy INSTANCE = new PropertyAccessStrategyBasicImpl();

	@Override
	public PropertyAccess buildPropertyAccess(PropertyAccessorService propertyAccessorService, Class<?> containerJavaType, final String propertyName, boolean setterRequired) {
		return new PropertyAccessBasicImpl( propertyAccessorService, this, containerJavaType, propertyName, setterRequired );
	}
}
