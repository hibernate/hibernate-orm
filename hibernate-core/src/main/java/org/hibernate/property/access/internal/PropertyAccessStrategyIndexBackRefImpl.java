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
	public PropertyAccess buildPropertyAccess(Class<?> containerJavaType, String propertyName, boolean setterRequired) {
		return new PropertyAccessIndexBackRefImpl( this );
	}

	private static class PropertyAccessIndexBackRefImpl implements PropertyAccess {
		private final PropertyAccessStrategyIndexBackRefImpl strategy;
		private final GetterImpl getter;

		public PropertyAccessIndexBackRefImpl(PropertyAccessStrategyIndexBackRefImpl strategy) {
			this.strategy = strategy;
			this.getter = new GetterImpl( strategy.entityName, strategy.propertyName );
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
	}

	private static class GetterImpl implements Getter {
		private final String entityName;
		private final String propertyName;

		public GetterImpl(String entityName, String propertyName) {
			this.entityName = entityName;
			this.propertyName = propertyName;
		}

		@Override
		public Object get(Object owner) {
			return PropertyAccessStrategyBackRefImpl.UNKNOWN;
		}

		@Override
		public Object getForInsert(Object owner, Map<Object, Object> mergeMap, SharedSessionContractImplementor session) {
			return session.getPersistenceContextInternal().getIndexInOwner( entityName, propertyName, owner, mergeMap );
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
