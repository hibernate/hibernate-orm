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
import org.hibernate.internal.util.NullnessUtil;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessException;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.Setter;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Christian Beikov
 */
public class ChainedPropertyAccessImpl implements PropertyAccess, Getter, Setter {

	private final PropertyAccess[] propertyAccesses;

	public ChainedPropertyAccessImpl(PropertyAccess... propertyAccesses) {
		this.propertyAccesses = propertyAccesses;
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
	public @Nullable Object get(Object owner) {
		@Nullable Object result = owner;
		for ( int i = 0; i < propertyAccesses.length; i++ ) {
			result = propertyAccesses[i].getGetter().get( NullnessUtil.castNonNull( result ) );
		}
		return result;
	}

	@Override
	public @Nullable Object getForInsert(Object owner, Map<Object, Object> mergeMap, SharedSessionContractImplementor session) {
		@Nullable Object result = owner;
		for ( int i = 0; i < propertyAccesses.length; i++ ) {
			if ( result == null ) {
				throw new PropertyAccessException( "Could not chain accessor because result of previous accessor was null" );
			}
			result = propertyAccesses[i].getGetter().getForInsert( result, mergeMap, session );
		}
		return result;
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
