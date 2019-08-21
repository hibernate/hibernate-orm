/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.service.internal;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.service.spi.EventActionWithParameter;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistrationException;
import org.hibernate.event.service.spi.JpaBootstrapSensitive;
import org.hibernate.event.spi.EventType;
import org.hibernate.jpa.event.spi.CallbackRegistryConsumer;

/**
 * @author Steve Ebersole
 * @author Sanne Grinovero
 */
class EventListenerGroupImpl<T> implements EventListenerGroup<T> {
	private EventType<T> eventType;
	private final EventListenerRegistryImpl listenerRegistry;

	private final Set<DuplicationStrategy> duplicationStrategies = new LinkedHashSet<>();

	// Performance: make sure iteration on this type is efficient; in particular we do not want to allocate iterators,
	// not having to capture state in lambdas.
	// So we keep the listeners in both a List (for convenience) and in an array (for iteration). Make sure
	// their content stays in synch!
	private T[] listeners = null;

	//Update both fields when making changes!
	private List<T> listenersAsList;

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
		final T[] ls = listeners;
		return ls == null ? 0 : ls.length;
	}

	@Override
	public void clear() {
		if ( duplicationStrategies != null ) {
			duplicationStrategies.clear();
		}
		listeners = null;
		listenersAsList = null;
	}

	@Override
	public final <U> void fireLazyEventOnEachListener(final Supplier<U> eventSupplier, final BiConsumer<T,U> actionOnEvent) {
		final T[] ls = listeners;
		if ( ls != null && ls.length != 0 ) {
			final U event = eventSupplier.get();
			for ( T listener : ls ) {
				actionOnEvent.accept( listener, event );
			}
		}
	}

	@Override
	public final <U> void fireEventOnEachListener(final U event, final BiConsumer<T,U> actionOnEvent) {
		final T[] ls = listeners;
		if ( ls != null ) {
			for ( T listener : ls ) {
				actionOnEvent.accept( listener, event );
			}
		}
	}

	@Override
	public <U,X> void fireEventOnEachListener(final U event, final X parameter, final EventActionWithParameter<T, U, X> actionOnEvent) {
		final T[] ls = listeners;
		if ( ls != null ) {
			for ( T listener : ls ) {
				actionOnEvent.applyEventToListener( listener, event, parameter );
			}
		}
	}

	@Override
	public void addDuplicationStrategy(DuplicationStrategy strategy) {
		duplicationStrategies.add( strategy );
	}

	/**
	 * Implementation note: should be final for performance reasons.
	 * @deprecated this is not the most efficient way for iterating the event listeners.
	 * See {@link #fireEventOnEachListener(Object, BiConsumer)} and co. for better alternatives.
	 */
	@Override
	@Deprecated
	public final Iterable<T> listeners() {
		final List<T> ls = listenersAsList;
		return ls == null ? Collections.EMPTY_LIST : ls;
	}

	@Override
	@SafeVarargs
	public final void appendListeners(T... listeners) {
		internalAppendListeners( listeners );
		checkForArrayRefresh();
	}

	private void checkForArrayRefresh() {
		final List<T> list = listenersAsList;
		if ( this.listeners == null ) {
			T[] a = (T[]) Array.newInstance( eventType.baseListenerInterface(), list.size() );
			listeners = list.<T>toArray( a );
		}
	}

	private void internalAppendListeners(T[] listeners) {
		for ( T listener : listeners ) {
			internalAppendListener( listener );
		}
	}

	@Override
	public void appendListener(T listener) {
		internalAppendListener( listener );
		checkForArrayRefresh();
	}

	private void internalAppendListener(T listener) {
		if ( listenerShouldGetAdded( listener ) ) {
			internalAppend( listener );
		}
	}

	@Override
	@SafeVarargs
	public final void prependListeners(T... listeners) {
		internalPrependListeners( listeners );
		checkForArrayRefresh();
	}

	private void internalPrependListeners(T[] listeners) {
		for ( T listener : listeners ) {
			internalPreprendListener( listener );
		}
	}

	@Override
	public void prependListener(T listener) {
		internalPreprendListener( listener );
		checkForArrayRefresh();
	}

	private void internalPreprendListener(T listener) {
		if ( listenerShouldGetAdded( listener ) ) {
			internalPrepend( listener );
		}
	}

	private boolean listenerShouldGetAdded(T listener) {
		final List<T> ts = listenersAsList;
		if ( ts == null ) {
			listenersAsList = new ArrayList<>();
			return true;
			// no need to do de-dup checks
		}

		boolean doAdd = true;
		strategy_loop: for ( DuplicationStrategy strategy : duplicationStrategies ) {
			final ListIterator<T> itr = ts.listIterator();
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
		listenersAsList.add( 0, listener );
		listeners = null; //Marks it for refreshing
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
		listenersAsList.add( listener );
		listeners = null; //Marks it for refreshing
	}
}
