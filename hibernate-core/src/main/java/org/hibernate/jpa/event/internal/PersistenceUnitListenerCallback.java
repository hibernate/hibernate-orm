/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.event.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

import org.hibernate.jpa.event.spi.PersistenceUnitCallback;
import org.hibernate.jpa.event.spi.PersistenceUnitCallbackType;
import org.hibernate.resource.beans.spi.ManagedBean;

import jakarta.annotation.Nonnull;

/// Persistence-unit lifecycle callback method declared by an entity listener class.
///
/// @author Steve Ebersole
public class PersistenceUnitListenerCallback<T> implements PersistenceUnitCallback<T> {
	private final ManagedBean<?> listenerManagedBean;
	private final Method callbackMethod;
	private final PersistenceUnitCallbackType callbackType;
	private final Class<T> callbackTargetType;

	public PersistenceUnitListenerCallback(
			ManagedBean<?> listenerManagedBean,
			Method callbackMethod,
			PersistenceUnitCallbackType callbackType,
			Class<T> callbackTargetType) {
		this.listenerManagedBean = listenerManagedBean;
		this.callbackMethod = callbackMethod;
		this.callbackType = callbackType;
		this.callbackTargetType = callbackTargetType;
	}

	@Override
	@Nonnull
	public PersistenceUnitCallbackType getCallbackType() {
		return callbackType;
	}

	@Override
	@Nonnull
	public Class<T> getCallbackTargetType() {
		return callbackTargetType;
	}

	@Override
	public void performCallback(@Nonnull T target) {
		try {
			callbackMethod.invoke( listenerManagedBean.getBeanInstance(), target );
		}
		catch (InvocationTargetException e) {
			final var targetException = e.getTargetException();
			throw targetException instanceof RuntimeException runtimeException
					? runtimeException
					: new RuntimeException( targetException );
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
				"PersistenceUnitListenerCallback([%s] %s.%s)",
				callbackType.name(),
				callbackMethod.getDeclaringClass().getName(),
				callbackMethod.getName()
		);
	}
}
