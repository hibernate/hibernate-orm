/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.service.internal;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.service.spi.EventActionWithParameter;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistrationException;
import org.hibernate.event.service.spi.JpaBootstrapSensitive;
import org.hibernate.event.spi.EventType;
import org.hibernate.jpa.event.spi.CallbackRegistryConsumer;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 * @author Sanne Grinovero
 */
class EventListenerGroupImpl<T> implements EventListenerGroup<T> {
	private static final Logger log = Logger.getLogger( EventListenerGroupImpl.class );

	private final EventType<T> eventType;
	private final EventListenerRegistryImpl listenerRegistry;

	private final Set<DuplicationStrategy> duplicationStrategies = new LinkedHashSet<>();

	private T[] listeners = null;

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
		duplicationStrategies.clear();
		listeners = null;
	}

	@Override
	public final <U> void fireLazyEventOnEachListener(final Supplier<U> eventSupplier, final BiConsumer<T,U> actionOnEvent) {
		final T[] ls = listeners;
		if ( ls != null && ls.length != 0 ) {
			final U event = eventSupplier.get();
			//noinspection ForLoopReplaceableByForEach
			for ( int i = 0; i < ls.length; i++ ) {
				actionOnEvent.accept( ls[i], event );
			}
		}
	}

	@Override
	public final <U> void fireEventOnEachListener(final U event, final BiConsumer<T,U> actionOnEvent) {
		final T[] ls = listeners;
		if ( ls != null ) {
			//noinspection ForLoopReplaceableByForEach
			for ( int i = 0; i < ls.length; i++ ) {
				actionOnEvent.accept( ls[i], event );
			}
		}
	}

	@Override
	public <U,X> void fireEventOnEachListener(final U event, final X parameter, final EventActionWithParameter<T, U, X> actionOnEvent) {
		final T[] ls = listeners;
		if ( ls != null ) {
			//noinspection ForLoopReplaceableByForEach
			for ( int i = 0; i < ls.length; i++ ) {
				actionOnEvent.applyEventToListener( ls[i], event, parameter );
			}
		}
	}

	@Override
	public void addDuplicationStrategy(DuplicationStrategy strategy) {
		duplicationStrategies.add( strategy );
	}

	@Override
	public void appendListener(T listener) {
		handleListenerAddition( listener, this::internalAppend );
	}

	@Override
	@SafeVarargs
	public final void appendListeners(T... listeners) {
		//noinspection ForLoopReplaceableByForEach
		for ( int i = 0; i < listeners.length; i++ ) {
			handleListenerAddition( listeners[i], this::internalAppend );
		}
	}

	private void internalAppend(T listener) {
		prepareListener( listener );

		if ( listeners == null ) {
			//noinspection unchecked
			this.listeners = (T[]) Array.newInstance( eventType.baseListenerInterface(), 1 );
			this.listeners[0] = listener;
		}
		else {
			final int size = this.listeners.length;

			//noinspection unchecked
			final T[] newCopy = (T[]) Array.newInstance( eventType.baseListenerInterface(), size+1 );

			// first copy the existing listeners
			System.arraycopy( this.listeners, 0, newCopy, 0, size );

			// and then put the new one after them
			newCopy[size] = listener;

			this.listeners = newCopy;
		}

	}

	@Override
	public void prependListener(T listener) {
		handleListenerAddition( listener, this::internalPrepend );
	}

	@Override
	@SafeVarargs
	public final void prependListeners(T... listeners) {
		//noinspection ForLoopReplaceableByForEach
		for ( int i = 0; i < listeners.length; i++ ) {
			handleListenerAddition( listeners[i], this::internalPrepend );
		}
	}

	private void internalPrepend(T listener) {
		prepareListener( listener );

		if ( this.listeners == null ) {
			//noinspection unchecked
			this.listeners = (T[]) Array.newInstance( eventType.baseListenerInterface(), 1 );
			this.listeners[0] = listener;
		}
		else {
			final int size = this.listeners.length;

			//noinspection unchecked
			final T[] newCopy = (T[]) Array.newInstance( eventType.baseListenerInterface(), size+1 );

			// put the new one first
			newCopy[0] = listener;

			// and copy the rest after it
			System.arraycopy( this.listeners, 0, newCopy, 1, size );

			this.listeners = newCopy;
		}
	}

	private void handleListenerAddition(T listener, Consumer<T> additionHandler) {
		if ( listeners == null ) {
			additionHandler.accept( listener );
			return;
		}

		final T[] localListenersRef = this.listeners;
		final boolean debugEnabled = log.isDebugEnabled();

		for ( DuplicationStrategy strategy : duplicationStrategies ) {

			// for each strategy, see if the strategy indicates that any of the existing
			//		listeners match the listener being added.  If so, we want to apply that
			//		strategy's action.  Control it returned immediately after applying the action
			//		on match - meaning no further strategies are checked...

			for ( int i = 0; i < localListenersRef.length; i++ ) {
				final T existingListener = localListenersRef[i];
				if ( debugEnabled ) {
					log.debugf(
							"Checking incoming listener [`%s`] for match against existing listener [`%s`]",
							listener,
							existingListener
					);
				}

				if ( strategy.areMatch( listener,  existingListener ) ) {
					if ( debugEnabled ) {
						log.debugf( "Found listener match between `%s` and `%s`", listener, existingListener );
					}

					switch ( strategy.getAction() ) {
						case ERROR: {
							throw new EventListenerRegistrationException( "Duplicate event listener found" );
						}
						case KEEP_ORIGINAL: {
							if ( debugEnabled ) {
								log.debugf( "Skipping listener registration (%s) : `%s`", strategy.getAction(), listener );
							}
							return;
						}
						case REPLACE_ORIGINAL: {
							if ( debugEnabled ) {
								log.debugf( "Replacing listener registration (%s) : `%s` -> %s", strategy.getAction(), existingListener, listener );
							}
							prepareListener( listener );

							listeners[i] = listener;
						}
					}

					// we've found a match - we should return
					//		- the match action has already been applied at this point
					return;
				}
			}
		}

		// we did not find any match.. add it
		checkAgainstBaseInterface( listener );
		performInjections( listener );
		additionHandler.accept( listener );
	}

	private void prepareListener(T listener) {
		checkAgainstBaseInterface( listener );
		performInjections( listener );
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


	/**
	 * Implementation note: should be final for performance reasons.
	 * @deprecated this is not the most efficient way for iterating the event listeners.
	 * See {@link #fireEventOnEachListener(Object, BiConsumer)} and co. for better alternatives.
	 */
	@Override
	@Deprecated
	public final Iterable<T> listeners() {
		if ( listeners == null ) {
			//noinspection unchecked
			return Collections.EMPTY_LIST;
		}

		return Arrays.asList( listeners );
	}
}
