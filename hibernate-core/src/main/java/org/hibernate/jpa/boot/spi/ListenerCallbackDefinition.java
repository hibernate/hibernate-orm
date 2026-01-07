/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.spi;

import org.hibernate.event.jpa.spi.Callback;
import org.hibernate.jpa.event.internal.ListenerCallback;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;

import java.lang.reflect.Method;

/// Boot model representation of a Jakarta Persistence style callback defined as a separate listener class.
///
/// @see jakarta.persistence.EntityListeners
/// @see ListenerCallback
///
/// @author Steve Ebersole
/// @author Kabir Khan
public class ListenerCallbackDefinition implements CallbackDefinition {
	private final Class<?> listenerClass;
	private final Method callbackMethod;
	private final CallbackType callbackType;

	public ListenerCallbackDefinition(Class<?> listenerClass, Method callbackMethod, CallbackType callbackType) {
		this.listenerClass = listenerClass;
		this.callbackMethod = callbackMethod;
		this.callbackType = callbackType;
	}

	@Override
	public Callback createCallback(ManagedBeanRegistry beanRegistry) {
		return new ListenerCallback( beanRegistry.getBean( listenerClass ), callbackMethod, callbackType );
	}
}
