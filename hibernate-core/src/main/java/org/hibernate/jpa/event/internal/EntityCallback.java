/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.event.internal;

import org.hibernate.jpa.event.spi.CallbackType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

/// Runtime handling for Jakarta Persistence style callback defined on an entity class.
///
/// @author Kabir Khan
/// @author Steve Ebersole
public class EntityCallback extends AbstractCallback {
	private final Method callbackMethod;

	public EntityCallback(Method callbackMethod, CallbackType callbackType) {
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
