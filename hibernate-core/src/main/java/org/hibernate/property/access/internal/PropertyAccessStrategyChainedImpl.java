/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.internal;

import org.hibernate.PropertyNotFoundException;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;

/**
 * @author Max Andersen
 * @author Steve Ebersole
 */
public class PropertyAccessStrategyChainedImpl implements PropertyAccessStrategy {
	private final PropertyAccessStrategy[] chain;

	public PropertyAccessStrategyChainedImpl(PropertyAccessStrategy... chain) {
		this.chain = chain;
	}

	@Override
	public PropertyAccess buildPropertyAccess(Class<?> containerJavaType, String propertyName, boolean setterRequired) {
		for ( PropertyAccessStrategy candidate : chain ) {
			try {
				return candidate.buildPropertyAccess( containerJavaType, propertyName, true );
			}
			catch (Exception ignore) {
				// ignore
			}
		}

		throw new PropertyNotFoundException( "Could not resolve PropertyAccess for " + propertyName + " on " + containerJavaType );
	}
}
