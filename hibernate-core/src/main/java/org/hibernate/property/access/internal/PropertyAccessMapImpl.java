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
import org.hibernate.property.access.spi.Setter;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * {@link PropertyAccess} implementation that deals with an underlying {@code Map}
 * as the container, using {@link Map#get} and {@link Map#put}.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class PropertyAccessMapImpl implements PropertyAccess {
	private final Getter getter;
	private final Setter setter;
	private final PropertyAccessStrategyMapImpl strategy;

	public PropertyAccessMapImpl(PropertyAccessStrategyMapImpl strategy, final String propertyName) {
		this.strategy = strategy;
		this.getter = new GetterImpl( propertyName );
		this.setter = new SetterImpl( propertyName );
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
	public Setter getSetter() {
		return setter;
	}

	public static class GetterImpl implements Getter {
		private final String propertyName;

		public GetterImpl(String propertyName) {
			this.propertyName = propertyName;
		}

		@Override
		@SuppressWarnings("rawtypes")
		public @Nullable Object get(Object owner) {
			return ( (Map) owner ).get( propertyName );
		}

		@Override
		public @Nullable Object getForInsert(Object owner, Map mergeMap, SharedSessionContractImplementor session) {
			return get( owner );
		}

		@Override
		public Class<?> getReturnTypeClass() {
			// we just don't know...
			return Object.class;
		}

		@Override
		public Type getReturnType() {
			return Object.class;
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

	public static class SetterImpl implements Setter {
		private final String propertyName;

		public SetterImpl(String propertyName) {
			this.propertyName = propertyName;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void set(Object target, @Nullable Object value) {
			( (Map) target ).put( propertyName, value );
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
}
