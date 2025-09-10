/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.service.spi;

import org.hibernate.event.spi.EventType;
import org.hibernate.service.Service;

/**
 * Service for accessing each {@link EventListenerGroup} by {@link EventType},
 * along with convenience methods for managing the listeners registered in
 * each {@link EventListenerGroup}.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("unchecked") // heap pollution due to varargs
public interface EventListenerRegistry extends Service {

	<T> EventListenerGroup<T> getEventListenerGroup(EventType<T> eventType);

	void addDuplicationStrategy(DuplicationStrategy strategy);

	<T> void setListeners(EventType<T> type, Class<? extends T>... listeners);
	<T> void setListeners(EventType<T> type, T... listeners);

	<T> void appendListeners(EventType<T> type, Class<? extends T>... listeners);
	<T> void appendListeners(EventType<T> type, T... listeners);

	<T> void prependListeners(EventType<T> type, Class<? extends T>... listeners);
	<T> void prependListeners(EventType<T> type, T... listeners);
}
