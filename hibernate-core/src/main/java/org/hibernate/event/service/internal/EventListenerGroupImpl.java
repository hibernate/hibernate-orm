/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.event.service.spi.JpaBootstrapSensitive;
import org.hibernate.event.spi.EventType;
import org.hibernate.jpa.event.spi.CallbackRegistryConsumer;

/**
 * @author Steve Ebersole
 */
class EventListenerGroupImpl<T> implements EventListenerGroup<T> {
	private EventType<T> eventType;
	private final EventListenerRegistryImpl listenerRegistry;

	private final Set<DuplicationStrategy> duplicationStrategies = new LinkedHashSet<>();
	private List<T> listeners;

	public EventListenerGroupImpl(EventType<T> eventType, EventListenerRegistryImpl listenerRegistry) {
		this.eventType = eventType;
		this.listenerRegistry = listenerRegistry;

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

	@Override
	public Iterable<T> listeners() {
		return listeners == null ? Collections.emptyList() : listeners;
	}

	@Override
	@SafeVarargs
	public final void appendListeners(T... listeners) {
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
	@SafeVarargs
	public final void prependListeners(T... listeners) {
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
			listeners = new ArrayList<>();
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
							checkAgainstBaseInterface( listener );
							performInjections( listener );
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
		performInjections( listener );
		listeners.add( 0, listener );
	}

	private void performInjections(T listener) {
		if ( CallbackRegistryConsumer.class.isInstance( listener ) ) {
			( (CallbackRegistryConsumer) listener ).injectCallbackRegistry( listenerRegistry.getCallbackRegistry() );
		}

		if ( JpaBootstrapSensitive.class.isInstance( listener ) ) {
			( (JpaBootstrapSensitive) listener ).wasJpaBootstrap(
					listenerRegistry.getSessionFactory().getSessionFactoryOptions().isJpaBootstrap()
			);
		}
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
		performInjections( listener );
		listeners.add( listener );
	}
}
