/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.event.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

import org.hibernate.jpa.event.spi.Callback;
import org.hibernate.jpa.event.spi.CallbackDefinition;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;

/**
 * Represents a JPA callback method declared by the entity itself.
 *
 * @author Kabir Khan
 * @author Steve Ebersole
 */
public class EntityCallback extends AbstractCallback {

	public static class Definition implements CallbackDefinition {
		private final Method callbackMethod;
		private final CallbackType callbackType;

		public Definition(Method callbackMethod, CallbackType callbackType) {
			this.callbackMethod = callbackMethod;
			this.callbackType = callbackType;
		}

		public Definition(MethodDetails callbackMethod, CallbackType callbackType) {
			this.callbackMethod = (Method) callbackMethod.toJavaMember();
			this.callbackType = callbackType;
		}

		@Override
		public Callback createCallback(ManagedBeanRegistry beanRegistry) {
			return new EntityCallback( callbackMethod, callbackType );
		}
	}

	private final Method callbackMethod;

	private EntityCallback(Method callbackMethod, CallbackType callbackType) {
		super( callbackType );
		this.callbackMethod = callbackMethod;
	}

	@Override
	public void performCallback(Object entity) {
		try {
			callbackMethod.invoke( entity );
		}
		catch (InvocationTargetException e) {
			//keep runtime exceptions as is
			if ( e.getTargetException() instanceof RuntimeException runtimeException ) {
				throw runtimeException;
			}
			else {
				throw new RuntimeException( e.getTargetException() );
			}
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"EntityCallback([%s] %s.%s)",
				getCallbackType().name(),
				callbackMethod.getDeclaringClass().getName(),
				callbackMethod.getName()
		);
	}
}
