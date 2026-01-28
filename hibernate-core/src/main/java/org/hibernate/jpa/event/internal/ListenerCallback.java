/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.event.internal;

import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.resource.beans.spi.ManagedBean;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Represents a JPA callback method declared by an entity listener class.
 *
 * @see jakarta.persistence.EntityListeners
 *
 * @author Kabir Khan
 * @author Steve Ebersole
 */
public class ListenerCallback extends AbstractCallback {
	private final Method callbackMethod;
	private final ManagedBean<?> listenerManagedBean;

	public ListenerCallback(ManagedBean<?> listenerManagedBean, Method callbackMethod, CallbackType callbackType) {
		super( callbackType );
		this.listenerManagedBean = listenerManagedBean;
		this.callbackMethod = callbackMethod;
	}

	@Override
	public void performCallback(Object entity) {
		try {
			callbackMethod.invoke( listenerManagedBean.getBeanInstance(), entity );
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
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"ListenerCallback([%s] %s.%s)",
				getCallbackType().name(),
				callbackMethod.getDeclaringClass().getName(),
				callbackMethod.getName()
		);
	}
}
