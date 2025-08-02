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
 * @author Michael Bartmann
 * @author Gavin King
 * @author Steve Ebersole
 */
public class PropertyAccessStrategyNoopImpl implements PropertyAccessStrategy {
	/**
	 * Singleton access
	 */
	public static final PropertyAccessStrategy INSTANCE = new PropertyAccessStrategyNoopImpl();

	@Override
	public PropertyAccess buildPropertyAccess(Class<?> containerJavaType, String propertyName, boolean setterRequired) {
		return PropertyAccessNoopImpl.INSTANCE;
	}

	private static class PropertyAccessNoopImpl implements PropertyAccess {
		/**
		 * Singleton access
		 */
		public static final PropertyAccessNoopImpl INSTANCE = new PropertyAccessNoopImpl();

		@Override
		public PropertyAccessStrategy getPropertyAccessStrategy() {
			return PropertyAccessStrategyNoopImpl.INSTANCE;
		}

		@Override
		public Getter getGetter() {
			return GetterImpl.INSTANCE;
		}

		@Override
		public Setter getSetter() {
			return SetterImpl.INSTANCE;
		}
	}

	private static class GetterImpl implements Getter {
		/**
		 * Singleton access
		 */
		public static final GetterImpl INSTANCE = new GetterImpl();

		@Override
		public @Nullable Object get(Object owner) {
			return null;
		}

		@Override
		public @Nullable Object getForInsert(Object owner, Map<Object, Object> mergeMap, SharedSessionContractImplementor session) {
			return null;
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
		public static final SetterImpl INSTANCE = new SetterImpl();

		@Override
		public void set(Object target, @Nullable Object value) {
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
