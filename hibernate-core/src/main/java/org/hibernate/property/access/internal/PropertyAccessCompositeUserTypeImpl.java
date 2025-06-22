/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.internal;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.Setter;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * {@link PropertyAccess} for accessing the wrapped property via get/set pair, which may be nonpublic.
 *
 * @author Steve Ebersole
 *
 * @see PropertyAccessStrategyBasicImpl
 */
public class PropertyAccessCompositeUserTypeImpl implements PropertyAccess, Getter {

	private final PropertyAccessStrategyCompositeUserTypeImpl strategy;
	private final int propertyIndex;

	public PropertyAccessCompositeUserTypeImpl(PropertyAccessStrategyCompositeUserTypeImpl strategy, String property) {
		this.strategy = strategy;
		this.propertyIndex = strategy.sortedPropertyNames.indexOf( property );
	}

	@Override
	public PropertyAccessStrategy getPropertyAccessStrategy() {
		return strategy;
	}

	@Override
	public Getter getGetter() {
		return this;
	}

	@Override
	public @Nullable Setter getSetter() {
		return null;
	}

	@Override
	public @Nullable Object get(Object owner) {
		return strategy.compositeUserType.getPropertyValue( owner, propertyIndex );
	}

	@SuppressWarnings("rawtypes")
	@Override
	public @Nullable Object getForInsert(Object owner, Map mergeMap, SharedSessionContractImplementor session) {
		return get( owner );
	}

	@Override
	public Class<?> getReturnTypeClass() {
		return ReflectHelper.getClass( strategy.sortedPropertyTypes.get(propertyIndex) );
	}

	@Override
	public Type getReturnType() {
		return strategy.sortedPropertyTypes.get(propertyIndex);
	}

	@Override
	public @Nullable Member getMember() {
		return null;
	}

	@Override
	public @Nullable String getMethodName() {
		return null;
	}

	@Override
	public @Nullable Method getMethod() {
		return null;
	}
}
