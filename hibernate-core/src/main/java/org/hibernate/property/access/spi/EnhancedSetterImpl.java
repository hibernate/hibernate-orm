/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.spi;

import java.io.Serializable;
import java.lang.reflect.Field;

import org.hibernate.Internal;
import org.hibernate.property.access.internal.AbstractFieldSerialForm;
import org.hibernate.property.access.internal.AccessStrategyHelper;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.property.access.internal.AccessStrategyHelper.determineEnhancementState;

/**
 * A specialized Setter implementation for handling setting values into
 * a bytecode-enhanced Class.  The reason we need specialized handling
 * is to render the fact that we need to account for certain enhancement features
 * during the setting process.
 *
 * @author Steve Ebersole
 * @author Luis Barreiro
 */
@Internal
public class EnhancedSetterImpl extends SetterFieldImpl {
	private final String propertyName;
	private final int enhancementState;

	public EnhancedSetterImpl(Class<?> containerClass, String propertyName, Field field) {
		super( containerClass, propertyName, field );
		this.propertyName = propertyName;
		this.enhancementState = determineEnhancementState( containerClass, field.getType() );
	}

	@Override
	public void set(Object target, @Nullable Object value) {
		super.set( target, value );
		AccessStrategyHelper.handleEnhancedInjection( target, value, enhancementState, propertyName );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// serialization

	private Object writeReplace() {
		return new SerialForm( getContainerClass(), propertyName, getField() );
	}

	private static class SerialForm extends AbstractFieldSerialForm implements Serializable {
		private final Class<?> containerClass;
		private final String propertyName;


		private SerialForm(Class<?> containerClass, String propertyName, Field field) {
			super( field );
			this.containerClass = containerClass;
			this.propertyName = propertyName;
		}

		private Object readResolve() {
			return new EnhancedSetterImpl( containerClass, propertyName, resolveField() );
		}
	}
}
