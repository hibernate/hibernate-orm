/*
 * SPDX-License-Identifier: Apache-2.0
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
	public static PropertyAccessStrategy with(AccessType getterAccessType) {
		return getterAccessType == null
				? STANDARD
				: switch ( getterAccessType ) {
					case FIELD -> FIELD;
					case PROPERTY -> PROPERTY;
				};
	}

	private final @Nullable AccessType classAccessType;

	public static PropertyAccessStrategy STANDARD = new PropertyAccessStrategyEnhancedImpl( null );
	public static PropertyAccessStrategy FIELD = new PropertyAccessStrategyEnhancedImpl( AccessType.FIELD );
	public static PropertyAccessStrategy PROPERTY = new PropertyAccessStrategyEnhancedImpl( AccessType.PROPERTY );

	public PropertyAccessStrategyEnhancedImpl(@Nullable AccessType classAccessType) {
		this.classAccessType = classAccessType;
	}

	@Override
	public PropertyAccess buildPropertyAccess(Class<?> containerJavaType, final String propertyName, boolean setterRequired) {
		return new PropertyAccessEnhancedImpl( this, containerJavaType, propertyName, classAccessType );
	}
}
