/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import java.util.function.Consumer;

import org.hibernate.event.service.spi.EventListenerGroup;

/**
 * Callback for {@link EventEngineContributor}
 *
 * @author Steve Ebersole
 */
public interface EventEngineContributions {
	/**
	 * Return the EventType by name, if one
	 */
	<T> EventType<T> findEventType(String name);

	/**
	 * Register a custom event type.
	 *
	 * @apiNote We except the "raw" state rather than an `EventType` instance to account for
	 * the `EventType#ordinal` property.  All registered types must be contiguous, so we handle
	 * the ordinality behind the scenes
	 */
	<T> EventType<T> contributeEventType(String name, Class<T> listenerRole);

	/**
	 * Register a custom event type with a default listener.
	 */
	<T> EventType<T> contributeEventType(String name, Class<T> listenerRole, T... defaultListener);

	/**
	 * Perform an action against the listener group for the specified event-type
	 */
	<T> void configureListeners(EventType<T> eventType, Consumer<EventListenerGroup<T>> action);
}
