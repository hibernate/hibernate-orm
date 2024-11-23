/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.internal;

import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;

import jakarta.persistence.AccessType;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Defines a strategy for accessing property values via a get/set pair, which may be nonpublic.  This
 * is the default (and recommended) strategy.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class PropertyAccessStrategyEnhancedImpl implements PropertyAccessStrategy {
	public static PropertyAccessStrategyEnhancedImpl with(AccessType getterAccessType) {
		if ( getterAccessType == AccessType.FIELD ) {
			return FIELD;
		}
		return STANDARD;
	}

	private final @Nullable AccessType getterAccessType;

	public static PropertyAccessStrategyEnhancedImpl STANDARD = new PropertyAccessStrategyEnhancedImpl( null );
	public static PropertyAccessStrategyEnhancedImpl FIELD = new PropertyAccessStrategyEnhancedImpl( AccessType.FIELD );

	public PropertyAccessStrategyEnhancedImpl(@Nullable AccessType getterAccessType) {
		this.getterAccessType = getterAccessType;
	}

	@Override
	public PropertyAccess buildPropertyAccess(Class<?> containerJavaType, final String propertyName, boolean setterRequired) {
		return new PropertyAccessEnhancedImpl( this, containerJavaType, propertyName, getterAccessType );
	}
}
