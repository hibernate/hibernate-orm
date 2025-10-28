/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.spi;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;

import org.hibernate.Internal;
import org.hibernate.property.access.internal.AbstractSetterMethodSerialForm;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.property.access.internal.AccessStrategyHelper.determineEnhancementState;
import static org.hibernate.property.access.internal.AccessStrategyHelper.handleEnhancedInjection;

/**
 * A specialized Setter implementation for handling setting values into a bytecode-enhanced Class
 * using a setter method.  The reason we need specialized handling is to render the fact that we
 * need to account for certain enhancement features during the setting process.
 *
 * @author Steve Ebersole
 * @author Luis Barreiro
 */
@Internal
public class EnhancedSetterMethodImpl extends SetterMethodImpl {
	private final String propertyName;
	private final int enhancementState;

	public EnhancedSetterMethodImpl(Class<?> containerClass, String propertyName, Method setterMethod) {
		super( containerClass, propertyName, setterMethod );
		this.propertyName = propertyName;
		this.enhancementState = determineEnhancementState( containerClass, setterMethod.getReturnType() );
	}

	@Override
	public void set(Object target, @Nullable Object value) {
		super.set( target, value );
		handleEnhancedInjection( target, value, enhancementState, propertyName );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// serialization

	@Serial
	private Object writeReplace() {
		return new SerialForm( getContainerClass(), propertyName, getMethod() );
	}

	private static class SerialForm extends AbstractSetterMethodSerialForm implements Serializable {
		private SerialForm(Class<?> containerClass, String propertyName, Method method) {
			super( containerClass, propertyName, method );
		}

		@Serial
		private Object readResolve() {
			return new EnhancedSetterMethodImpl( getContainerClass(), getPropertyName(), resolveMethod() );
		}
	}
}
