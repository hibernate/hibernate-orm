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
package org.hibernate.event.service.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistrationException;
import org.hibernate.event.spi.EventType;

/**
 * @author Steve Ebersole
 */
public class EventListenerGroupImpl<T> implements EventListenerGroup<T> {
	private EventType<T> eventType;

	private final Set<DuplicationStrategy> duplicationStrategies = new LinkedHashSet<DuplicationStrategy>();
	private List<T> listeners;

	public EventListenerGroupImpl(EventType<T> eventType) {
		this.eventType = eventType;
		duplicationStrategies.add(
				// At minimum make sure we do not register the same exact listener class multiple times.
				new DuplicationStrategy() {
					@Override
					public boolean areMatch(Object listener, Object original) {
						return listener.getClass().equals( original.getClass() );
					}

					@Override
					public Action getAction() {
						return Action.ERROR;
					}
				}
		);
	}

	@Override
	public EventType<T> getEventType() {
		return eventType;
	}

	@Override
	public boolean isEmpty() {
		return count() <= 0;
	}

	@Override
	public int count() {
		return listeners == null ? 0 : listeners.size();
	}

	@Override
	public void clear() {
		if ( duplicationStrategies != null ) {
			duplicationStrategies.clear();
		}
		if ( listeners != null ) {
			listeners.clear();
		}
	}

	@Override
	public void addDuplicationStrategy(DuplicationStrategy strategy) {
		duplicationStrategies.add( strategy );
	}

	public Iterable<T> listeners() {
		return listeners == null ? Collections.<T>emptyList() : listeners;
	}

	@Override
	public void appendListeners(T... listeners) {
		for ( T listener : listeners ) {
			appendListener( listener );
		}
	}

	@Override
	public void appendListener(T listener) {
		if ( listenerShouldGetAdded( listener ) ) {
			internalAppend( listener );
		}
	}

	@Override
	public void prependListeners(T... listeners) {
		for ( T listener : listeners ) {
			prependListener( listener );
		}
	}

	@Override
	public void prependListener(T listener) {
		if ( listenerShouldGetAdded( listener ) ) {
			internalPrepend( listener );
		}
	}

	private boolean listenerShouldGetAdded(T listener) {
		if ( listeners == null ) {
			listeners = new ArrayList<T>();
			return true;
			// no need to do de-dup checks
		}

		boolean doAdd = true;
		strategy_loop: for ( DuplicationStrategy strategy : duplicationStrategies ) {
			final ListIterator<T> itr = listeners.listIterator();
			while ( itr.hasNext() ) {
				final T existingListener = itr.next();
				if ( strategy.areMatch( listener,  existingListener ) ) {
					switch ( strategy.getAction() ) {
						// todo : add debug logging of what happens here...
						case ERROR: {
							throw new EventListenerRegistrationException( "Duplicate event listener found" );
						}
						case KEEP_ORIGINAL: {
							doAdd = false;
							break strategy_loop;
						}
						case REPLACE_ORIGINAL: {
							itr.set( listener );
							doAdd = false;
							break strategy_loop;
						}
					}
				}
			}
		}
		return doAdd;
	}

	private void internalPrepend(T listener) {
		checkAgainstBaseInterface( listener );
		listeners.add( 0, listener );
	}

	private void checkAgainstBaseInterface(T listener) {
		if ( !eventType.baseListenerInterface().isInstance( listener ) ) {
			throw new EventListenerRegistrationException(
					"Listener did not implement expected interface [" + eventType.baseListenerInterface().getName() + "]"
			);
		}
	}

	private void internalAppend(T listener) {
		checkAgainstBaseInterface( listener );
		listeners.add( listener );
	}
}
