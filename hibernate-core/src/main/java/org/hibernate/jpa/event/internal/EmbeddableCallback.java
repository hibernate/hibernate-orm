/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.event.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

import org.hibernate.jpa.event.spi.Callback;
import org.hibernate.jpa.event.spi.CallbackDefinition;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;

/**
 * Represents a JPA callback method declared by an embeddable type.
 *
 * @apiNote The JPA specification does not require that we allow
 * entity lifecycle callbacks on embeddable classes, and this is
 * at least arguably a misfeature. It would by OK to simply remove
 * or deprecate this.
 *
 * @author Vlad Mihalcea
 */
public class EmbeddableCallback extends AbstractCallback {

	public static class Definition implements CallbackDefinition {
		private final Getter embeddableGetter;
		private final Method callbackMethod;
		private final CallbackType callbackType;

		public Definition(Getter embeddableGetter, Method callbackMethod, CallbackType callbackType) {
			this.embeddableGetter = embeddableGetter;
			this.callbackMethod = callbackMethod;
			this.callbackType = callbackType;
		}

		@Override
		public Callback createCallback(ManagedBeanRegistry beanRegistry) {
			return new EmbeddableCallback( embeddableGetter, callbackMethod, callbackType );
		}
	}

	private final Getter embeddableGetter;
	private final Method callbackMethod;

	private EmbeddableCallback(Getter embeddableGetter, Method callbackMethod, CallbackType callbackType) {
		super( callbackType );
		this.embeddableGetter = embeddableGetter;
		this.callbackMethod = callbackMethod;
	}

	@Override
	public void performCallback(Object entity) {
		try {
			final Object embeddable = embeddableGetter.get( entity );
			if ( embeddable != null ) {
				callbackMethod.invoke( embeddable );
			}
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
				"EmbeddableCallback([%s] %s.%s)",
				getCallbackType().name(),
				callbackMethod.getDeclaringClass().getName(),
				callbackMethod.getName()
		);
	}
}
