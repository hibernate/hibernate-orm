/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import org.hibernate.Incubating;
import org.hibernate.accessor.AccessorFactory;
import org.hibernate.accessor.Instantiator;
import org.hibernate.accessor.MultiValueAccessorGenerationException;
import org.hibernate.accessor.MultiValueReader;
import org.hibernate.accessor.MultiValueWriter;
import org.hibernate.accessor.ValueReader;
import org.hibernate.accessor.ValueWriter;

/**
 * Decorator around {@link AccessorFactory#reflection()} that disables multi-value
 * accessor generation, causing callers to fall back to per-property access.
 */
@Incubating(since = "8.0")
public class ReflectionNoMultiAccessorFactory implements AccessorFactory {

	private static final String MULTI_VALUE_DISABLED_MESSAGE =
			"Multi-value accessors are disabled by hibernate.accessor.strategy=reflection-no-multi";

	private final AccessorFactory delegate = AccessorFactory.reflection();

	@Override
	public <T> Instantiator<T> instantiator(Constructor<T> constructor) {
		return delegate.instantiator( constructor );
	}

	@Override
	public ValueReader<?> valueReader(Field field) {
		return delegate.valueReader( field );
	}

	@Override
	public ValueReader<?> valueReader(Method method) {
		return delegate.valueReader( method );
	}

	@Override
	public ValueWriter valueWriter(Field field) {
		return delegate.valueWriter( field );
	}

	@Override
	public ValueWriter valueWriter(Method setter) {
		return delegate.valueWriter( setter );
	}

	@Override
	public MultiValueReader multiValueReader(Class<?> declaringClass, Member... members) {
		throw new MultiValueAccessorGenerationException( MULTI_VALUE_DISABLED_MESSAGE );
	}

	@Override
	public MultiValueWriter multiValueWriter(Class<?> declaringClass, Member... members) {
		throw new MultiValueAccessorGenerationException( MULTI_VALUE_DISABLED_MESSAGE );
	}
}
