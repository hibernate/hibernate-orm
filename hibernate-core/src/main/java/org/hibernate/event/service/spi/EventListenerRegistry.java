/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.service.spi;

import org.hibernate.event.spi.EventType;
import org.hibernate.service.Service;
import jakarta.annotation.Nonnull;

/**
 * Service for accessing each {@link EventListenerGroup} by {@link EventType},
 * along with convenience methods for managing the listeners registered in
 * each {@link EventListenerGroup}.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("unchecked") // heap pollution due to varargs
public interface EventListenerRegistry extends Service {

	@Nonnull
	<T> EventListenerGroup<T> getEventListenerGroup(@Nonnull EventType<T> eventType);

	void addDuplicationStrategy(@Nonnull DuplicationStrategy strategy);

	<T> void setListeners(@Nonnull EventType<T> type, @Nonnull Class<? extends T>... listeners);
	<T> void setListeners(@Nonnull EventType<T> type, @Nonnull T... listeners);

	<T> void appendListeners(@Nonnull EventType<T> type, @Nonnull Class<? extends T>... listeners);
	<T> void appendListeners(@Nonnull EventType<T> type, @Nonnull T... listeners);

	<T> void prependListeners(@Nonnull EventType<T> type, @Nonnull Class<? extends T>... listeners);
	<T> void prependListeners(@Nonnull EventType<T> type, @Nonnull T... listeners);
}
