/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.service.spi;

import java.io.Serializable;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.event.spi.EventType;
import org.hibernate.service.Service;

/**
 * Service for accessing each {@link EventListenerGroup} by {@link EventType}, as well as convenience
 * methods for managing the listeners registered in each {@link EventListenerGroup}.
 *
 * @author Steve Ebersole
 */
public interface EventListenerRegistry extends Service, Serializable {
	void prepare(MetadataImplementor metadata);

	<T> EventListenerGroup<T> getEventListenerGroup(EventType<T> eventType);

	void addDuplicationStrategy(DuplicationStrategy strategy);

	<T> void setListeners(EventType<T> type, Class<? extends T>... listeners);
	<T> void setListeners(EventType<T> type, T... listeners);

	<T> void appendListeners(EventType<T> type, Class<? extends T>... listeners);
	<T> void appendListeners(EventType<T> type, T... listeners);

	<T> void prependListeners(EventType<T> type, Class<? extends T>... listeners);
	<T> void prependListeners(EventType<T> type, T... listeners);
}
