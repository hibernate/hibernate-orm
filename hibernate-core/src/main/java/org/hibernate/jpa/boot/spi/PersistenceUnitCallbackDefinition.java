/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.spi;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nonnull;
import org.hibernate.boot.models.spi.PersistenceUnitLifecycleEventHandler;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.jpa.event.internal.PersistenceUnitListenerCallback;
import org.hibernate.jpa.event.spi.PersistenceUnitCallback;
import org.hibernate.jpa.event.spi.PersistenceUnitCallbackType;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;

import static java.util.Collections.emptyList;

/// Boot model representation of a Jakarta Persistence persistence-unit lifecycle callback.
///
/// @author Gavin King
///
/// @since 8.0
public class PersistenceUnitCallbackDefinition implements Serializable {
	private final Class<?> listenerClass;
	private final Method callbackMethod;
	private final PersistenceUnitCallbackType callbackType;
	private final Class<?> callbackTargetType;

	public PersistenceUnitCallbackDefinition(
			@Nonnull Class<?> listenerClass,
			@Nonnull Method callbackMethod,
			@Nonnull PersistenceUnitCallbackType callbackType,
			@Nonnull Class<?> callbackTargetType) {
		this.listenerClass = listenerClass;
		this.callbackMethod = callbackMethod;
		this.callbackType = callbackType;
		this.callbackTargetType = callbackTargetType;
	}

	@Nonnull
	public PersistenceUnitCallback<?> createCallback(@Nonnull ManagedBeanRegistry beanRegistry) {
		return new PersistenceUnitListenerCallback<>(
				beanRegistry.getBean( listenerClass ),
				callbackMethod,
				callbackType,
				callbackTargetType
		);
	}

	@Nonnull
	public static List<PersistenceUnitCallbackDefinition> from(
			@Nonnull List<PersistenceUnitLifecycleEventHandler> lifecycleEventHandlers) {
		if ( lifecycleEventHandlers.isEmpty() ) {
			return emptyList();
		}

		final List<PersistenceUnitCallbackDefinition> callbackDefinitions = new ArrayList<>();
		for ( var lifecycleEventHandler : lifecycleEventHandlers ) {
			final var listenerClass = lifecycleEventHandler.getCallbackClass().toJavaClass();
			for ( var callbackMethod : lifecycleEventHandler.getCallbackMethods() ) {
				final var method = callbackMethod.methodDetails().toJavaMember();
				ReflectHelper.ensureAccessibility( method );
				callbackDefinitions.add( new PersistenceUnitCallbackDefinition(
						listenerClass,
						method,
						callbackMethod.callbackType(),
						method.getParameterTypes()[0]
				) );
			}
		}
		return List.copyOf( callbackDefinitions );
	}
}
