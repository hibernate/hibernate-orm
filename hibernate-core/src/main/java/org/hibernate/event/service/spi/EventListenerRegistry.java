/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.event.service.spi;

import java.io.Serializable;

import org.hibernate.event.spi.EventType;
import org.hibernate.service.Service;

/**
 * Service for accessing each {@link EventListenerGroup} by {@link EventType}, as well as convenience
 * methods for managing the listeners registered in each {@link EventListenerGroup}.
 *
 * @author Steve Ebersole
 */
public interface EventListenerRegistry extends Service, Serializable {
	public <T> EventListenerGroup<T> getEventListenerGroup(EventType<T> eventType);

	public void addDuplicationStrategy(DuplicationStrategy strategy);

	public <T> void setListeners(EventType<T> type, Class<? extends T>... listeners);
	public <T> void setListeners(EventType<T> type, T... listeners);

	public <T> void appendListeners(EventType<T> type, Class<? extends T>... listeners);
	public <T> void appendListeners(EventType<T> type, T... listeners);

	public <T> void prependListeners(EventType<T> type, Class<? extends T>... listeners);
	public <T> void prependListeners(EventType<T> type, T... listeners);
}
