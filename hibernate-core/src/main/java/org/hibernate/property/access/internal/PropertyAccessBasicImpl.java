/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.internal;

import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.GetterMethodImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.property.access.spi.SetterMethodImpl;


import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.internal.util.ReflectHelper.findGetterMethod;
import static org.hibernate.internal.util.ReflectHelper.findSetterMethod;
import static org.hibernate.internal.util.ReflectHelper.setterMethodOrNull;

/**
 * {@link PropertyAccess} for accessing the wrapped property via get/set pair, which may be nonpublic.
 *
 * @author Steve Ebersole
 *
 * @see PropertyAccessStrategyBasicImpl
 */
public class PropertyAccessBasicImpl implements PropertyAccess {

	private final PropertyAccessStrategyBasicImpl strategy;
	private final GetterMethodImpl getter;
	private final @Nullable SetterMethodImpl setter;

	public PropertyAccessBasicImpl(
			PropertyAccessStrategyBasicImpl strategy,
			Class<?> containerJavaType,
			final String propertyName,
			boolean setterRequired) {
		this.strategy = strategy;

		final var getterMethod = findGetterMethod( containerJavaType, propertyName );
		getter = new GetterMethodImpl( containerJavaType, propertyName, getterMethod );

		final var setterMethod = setterRequired
				? findSetterMethod( containerJavaType, propertyName, getterMethod.getReturnType() )
				: setterMethodOrNull( containerJavaType, propertyName, getterMethod.getReturnType() );
		setter = setterMethod != null
				? new SetterMethodImpl( containerJavaType, propertyName, setterMethod )
				: null;
	}

	@Override
	public PropertyAccessStrategy getPropertyAccessStrategy() {
		return strategy;
	}

	@Override
	public Getter getGetter() {
		return getter;
	}

	@Override
	public @Nullable Setter getSetter() {
		return setter;
	}
}
