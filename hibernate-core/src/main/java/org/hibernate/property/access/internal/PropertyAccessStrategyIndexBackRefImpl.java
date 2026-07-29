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
import org.hibernate.property.access.spi.PropertyAccessorService;
import org.hibernate.property.access.spi.PropertyValueAccessor;
import org.hibernate.property.access.spi.Setter;

import jakarta.annotation.Nullable;

/**
 * @author Gavin King
 * @author Steve Ebersole
 */
public class PropertyAccessStrategyIndexBackRefImpl implements PropertyAccessStrategy {
	private final String entityName;
	private final String propertyName;

	public PropertyAccessStrategyIndexBackRefImpl(String collectionRole, String entityName) {
		this.entityName = entityName;
		this.propertyName = collectionRole.substring( entityName.length() + 1 );
	}

	@Override
	public PropertyAccess buildPropertyAccess(PropertyAccessorService propertyAccessorService, Class<?> containerJavaType, String propertyName, boolean setterRequired) {
		return new PropertyAccessIndexBackRefImpl( this );
	}

	private static class PropertyAccessIndexBackRefImpl implements PropertyAccess {
		private final PropertyAccessStrategyIndexBackRefImpl strategy;
		private final PropertyValueAccessor propertyValueAccessor;
		private final GetterImpl getter;

		public PropertyAccessIndexBackRefImpl(PropertyAccessStrategyIndexBackRefImpl strategy) {
			this.strategy = strategy;
			this.propertyValueAccessor = PropertyValueAccessor.indexBackRef( strategy.entityName, strategy.propertyName );
			this.getter = new GetterImpl( propertyValueAccessor );
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
			return SetterImpl.INSTANCE;
		}

		@Override
		public PropertyValueAccessor getPropertyValueAccessor() {
			return propertyValueAccessor;
		}
	}

	private static class GetterImpl implements Getter {
		private final PropertyValueAccessor propertyValueAccessor;

		public GetterImpl(PropertyValueAccessor propertyValueAccessor) {
			this.propertyValueAccessor = propertyValueAccessor;
		}

		@Override
		public Object get(Object owner) {
			return propertyValueAccessor.get( owner );
		}

		@Override
		public Object getForInsert(Object owner, Map<Object, Object> mergeMap, SharedSessionContractImplementor session) {
			return propertyValueAccessor.getForInsert( owner, mergeMap, session );
		}

		@Override
		public Class<?> getReturnTypeClass() {
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

	private static class SetterImpl implements Setter {
		/**
		 * Singleton access
		 */
		public static final Setter INSTANCE = new SetterImpl();

		@Override
		public void set(Object target, @Nullable Object value) {
			// this page intentionally left blank :)
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
