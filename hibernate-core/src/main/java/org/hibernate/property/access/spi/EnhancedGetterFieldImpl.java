/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.spi;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.Internal;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * A specialized Getter implementation for handling getting values from
 * a bytecode-enhanced Class. The reason we need specialized handling
 * is to produce the correct {@link java.lang.reflect.Member} while
 * using the {@link Field} to access values and ensure correct functionality.
 *
 * @author Steve Ebersole
 * @author Luis Barreiro
 */
@Internal
public class EnhancedGetterFieldImpl extends GetterFieldImpl {
	public EnhancedGetterFieldImpl(Class<?> containerClass, String propertyName, Field field, Method getterMethod) {
		super( containerClass, propertyName, field, getterMethod );
		assert getterMethod != null;
	}

	@Override
	public @NonNull Method getMethod() {
		return castNonNull( super.getMethod() );
	}

	@Override
	public Member getMember() {
		return getMethod();
	}
}
