/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.spi;

import org.hibernate.event.jpa.spi.Callback;
import org.hibernate.jpa.event.internal.EntityCallback;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;

import java.lang.reflect.Method;

/// Boot model representation of a Jakarta Persistence style callback defined on an entity class.
///
/// @see EntityCallback
///
/// @author Steve Ebersole
/// @author Kabir Khan
public class EntityCallbackDefinition implements CallbackDefinition {
	private final Method callbackMethod;
	private final CallbackType callbackType;

	public EntityCallbackDefinition(Method callbackMethod, CallbackType callbackType) {
		this.callbackMethod = callbackMethod;
		this.callbackType = callbackType;
	}

	public EntityCallbackDefinition(MethodDetails callbackMethod, CallbackType callbackType) {
		this.callbackMethod = (Method) callbackMethod.toJavaMember();
		this.callbackType = callbackType;
	}

	@Override
	public Callback createCallback(ManagedBeanRegistry beanRegistry) {
		return new EntityCallback( callbackMethod, callbackType );
	}
}
