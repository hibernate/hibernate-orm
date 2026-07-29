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
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.PropertyValueAccessor;
import org.hibernate.property.access.spi.Setter;

import jakarta.annotation.Nullable;

/**
 * @author Christian Beikov
 */
public class ChainedPropertyAccessImpl implements PropertyAccess, Getter, Setter {

	private final PropertyAccess[] propertyAccesses;
	private final PropertyValueAccessor propertyValueAccessor;

	public ChainedPropertyAccessImpl(PropertyAccess... propertyAccesses) {
		this.propertyAccesses = propertyAccesses;
		final PropertyValueAccessor[] accessors = new PropertyValueAccessor[propertyAccesses.length];
		for (int i = 0; i < accessors.length; i++) {
			accessors[i] = propertyAccesses[i].getPropertyValueAccessor();
		}
		this.propertyValueAccessor = PropertyValueAccessor.chained( accessors );
	}

	@Override
	public PropertyAccessStrategy getPropertyAccessStrategy() {
		return propertyAccesses[0].getPropertyAccessStrategy();
	}

	@Override
	public Getter getGetter() {
		return this;
	}

	@Override
	public Setter getSetter() {
		return this;
	}

	@Override
	public PropertyValueAccessor getPropertyValueAccessor() {
		return propertyValueAccessor;
	}

	@Override
	public @Nullable Object get(Object owner) {
		return propertyValueAccessor.get( owner );
	}

	@Override
	public @Nullable Object getForInsert(Object owner, Map<Object, Object> mergeMap, SharedSessionContractImplementor session) {
		return propertyValueAccessor.getForInsert( owner, mergeMap, session );
	}

	@Override
	public void set(Object target, @Nullable Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Class<?> getReturnTypeClass() {
		return propertyAccesses[propertyAccesses.length - 1].getGetter().getReturnTypeClass();
	}

	@Override
	public Type getReturnType() {
		return propertyAccesses[propertyAccesses.length - 1].getGetter().getReturnType();
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
