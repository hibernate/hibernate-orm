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
	private final PropertyValueAccessor propertyValueAccessor;

	public PropertyAccessMapImpl(PropertyAccessStrategyMapImpl strategy, final String propertyName) {
		this.strategy = strategy;
		this.propertyValueAccessor = PropertyValueAccessor.map( propertyName );
		this.getter = new GetterImpl( propertyValueAccessor );
		this.setter = new SetterImpl( propertyValueAccessor );
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

	@Override
	public PropertyValueAccessor getPropertyValueAccessor() {
		return propertyValueAccessor;
	}

	public static class GetterImpl implements Getter {
		private final PropertyValueAccessor propertyValueAccessor;

		public GetterImpl(PropertyValueAccessor propertyValueAccessor) {
			this.propertyValueAccessor = propertyValueAccessor;
		}

		@Override
		public @Nullable Object get(Object owner) {
			return propertyValueAccessor.get( owner );
		}

		@Override
		public @Nullable Object getForInsert(Object owner, Map<Object, Object> mergeMap, SharedSessionContractImplementor session) {
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
		private final PropertyValueAccessor propertyValueAccessor;

		public SetterImpl(PropertyValueAccessor propertyValueAccessor) {
			this.propertyValueAccessor = propertyValueAccessor;
		}

		@Override
		public void set(Object target, @Nullable Object value) {
			propertyValueAccessor.set( target, value );
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
