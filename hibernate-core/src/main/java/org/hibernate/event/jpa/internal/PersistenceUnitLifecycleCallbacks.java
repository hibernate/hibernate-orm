/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.jpa.internal;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.hibernate.jpa.boot.spi.PersistenceUnitCallbackDefinition;
import org.hibernate.jpa.event.spi.PersistenceUnitCallback;
import org.hibernate.jpa.event.spi.PersistenceUnitCallbackType;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;

import jakarta.annotation.Nonnull;

import static java.util.Collections.emptyMap;

/// Runtime invoker for JPA persistence unit lifecycle callbacks.
///
/// @author Gavin King
public class PersistenceUnitLifecycleCallbacks {
	public static final PersistenceUnitLifecycleCallbacks NO_CALLBACKS =
			new PersistenceUnitLifecycleCallbacks( emptyMap() );

	private final Map<PersistenceUnitCallbackType, List<PersistenceUnitCallback<?>>> callbacks;

	private PersistenceUnitLifecycleCallbacks(Map<PersistenceUnitCallbackType, List<PersistenceUnitCallback<?>>> callbacks) {
		this.callbacks = callbacks;
	}

	public void postCreate(@Nonnull Object target) {
		callback( PersistenceUnitCallbackType.POST_CREATE, target );
	}

	public void preClose(@Nonnull Object target) {
		callback( PersistenceUnitCallbackType.PRE_CLOSE, target );
	}

	private void callback(@Nonnull PersistenceUnitCallbackType callbackType, @Nonnull Object target) {
		final var typeCallbacks = callbacks.get( callbackType );
		if ( typeCallbacks != null ) {
			for ( var callback : typeCallbacks ) {
				if ( callback.getCallbackTargetType().isInstance( target ) ) {
					performCallback( callback, target );
				}
			}
		}
	}

	private <T> void performCallback(@Nonnull PersistenceUnitCallback<T> callback, @Nonnull Object target) {
		callback.performCallback( callback.getCallbackTargetType().cast( target ) );
	}

	public static PersistenceUnitLifecycleCallbacks from(
			@Nonnull List<PersistenceUnitCallbackDefinition> callbackDefinitions,
			ManagedBeanRegistry beanRegistry) {
		if ( callbackDefinitions.isEmpty() ) {
			return NO_CALLBACKS;
		}
		else {
			final EnumMap<PersistenceUnitCallbackType, List<PersistenceUnitCallback<?>>> callbacks =
					new EnumMap<>( PersistenceUnitCallbackType.class );
			for ( var callbackType : PersistenceUnitCallbackType.values() ) {
				callbacks.put( callbackType, new ArrayList<>() );
			}
			for ( var definition : callbackDefinitions ) {
				final var callback = definition.createCallback( beanRegistry );
				callbacks.get( callback.getCallbackType() ).add( callback );
			}
			return new PersistenceUnitLifecycleCallbacks( callbacks );
		}
	}
}
