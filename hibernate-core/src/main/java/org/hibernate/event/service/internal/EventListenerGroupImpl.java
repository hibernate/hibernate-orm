/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.service.internal;

import java.lang.reflect.Array;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.service.spi.EventActionWithParameter;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistrationException;
import org.hibernate.event.service.spi.JpaBootstrapSensitive;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.build.AllowReflection;
import org.hibernate.jpa.event.spi.CallbackRegistry;
import org.hibernate.jpa.event.spi.CallbackRegistryConsumer;

import static java.lang.System.arraycopy;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hibernate.event.internal.EventListenerLogging.EVENT_LISTENER_LOGGER;

/**
 * Standard EventListenerGroup implementation
 *
 * @author Steve Ebersole
 * @author Sanne Grinovero
 */
class EventListenerGroupImpl<T> implements EventListenerGroup<T> {

	private static final DuplicationStrategy DEFAULT_DUPLICATION_STRATEGY =
			new DuplicationStrategy() {
				@Override
				public boolean areMatch(Object listener, Object original) {
					return listener.getClass().equals( original.getClass() );
				}
				@Override
				public Action getAction() {
					return Action.ERROR;
				}
			};
	private static final Set<DuplicationStrategy> DEFAULT_DUPLICATION_STRATEGIES =
			singleton( DEFAULT_DUPLICATION_STRATEGY );

	private static final CompletableFuture<?> COMPLETED = completedFuture( null );
	@SuppressWarnings("unchecked")
	private static <R> CompletableFuture<R> nullCompletion() {
		return (CompletableFuture<R>) COMPLETED;
	}

	private final EventType<T> eventType;
	private final CallbackRegistry callbackRegistry;
	private final boolean isJpaBootstrap;

	//TODO at least the list of listeners should be made constant;
	//unfortunately a number of external integrations rely on being able to make
	//changes to listeners at runtime, so this will require some planning.
	private volatile Set<DuplicationStrategy> duplicationStrategies = DEFAULT_DUPLICATION_STRATEGIES;
	private volatile T[] listeners = null;
	private volatile List<T> listenersAsList = emptyList();

	public EventListenerGroupImpl(EventType<T> eventType, CallbackRegistry callbackRegistry, boolean isJpaBootstrap) {
		this.eventType = eventType;
		this.callbackRegistry = callbackRegistry;
		this.isJpaBootstrap = isJpaBootstrap;
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
		//Odd semantics: we're expected (for backwards compatibility)
		//               to also clear the default DuplicationStrategy.
		duplicationStrategies = new LinkedHashSet<>();
		setListeners( null );
	}

	// For efficiency reasons we use both a representation as List and as array;
	// ensure consistency between the two fields by delegating any mutation to both
	// fields to this method.
	private synchronized void setListeners(T[] newListeners) {
		listeners = newListeners;
		listenersAsList = newListeners == null || newListeners.length == 0
				? emptyList()
				: asList( newListeners );
	}

	@Override
	public void clearListeners() {
		setListeners( null );
	}

	@Override
	public final <U> void fireLazyEventOnEachListener(Supplier<U> eventSupplier, BiConsumer<T,U> actionOnEvent) {
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
	public final <U> void fireEventOnEachListener(U event, BiConsumer<T,U> actionOnEvent) {
		final T[] ls = listeners;
		if ( ls != null ) {
			//noinspection ForLoopReplaceableByForEach
			for ( int i = 0; i < ls.length; i++ ) {
				actionOnEvent.accept( ls[i], event );
			}
		}
	}

	@Override
	public <U,X> void fireEventOnEachListener(U event, X parameter, EventActionWithParameter<T, U, X> actionOnEvent) {
		final T[] ls = listeners;
		if ( ls != null ) {
			//noinspection ForLoopReplaceableByForEach
			for ( int i = 0; i < ls.length; i++ ) {
				actionOnEvent.applyEventToListener( ls[i], event, parameter );
			}
		}
	}

	@Override
	public <R, U, RL> CompletionStage<R> fireEventOnEachListener(
			final U event,
			final Function<RL, Function<U, CompletionStage<R>>> fun) {
		CompletionStage<R> ret = nullCompletion();
		final T[] ls = listeners;
		if ( ls != null ) {
			for ( T listener : ls ) {
				//to preserve atomicity of the Session methods
				//call apply() from within the arg of thenCompose()
				ret = ret.thenCompose( v -> fun.apply( (RL) listener ).apply( event ) );
			}
		}
		return ret;
	}

	@Override
	public <R, U, RL, X> CompletionStage<R> fireEventOnEachListener(
			U event, X param, Function<RL, BiFunction<U, X, CompletionStage<R>>> fun) {
		CompletionStage<R> ret = nullCompletion();
		final T[] ls = listeners;
		if ( ls != null ) {
			for ( T listener : ls ) {
				//to preserve atomicity of the Session methods
				//call apply() from within the arg of thenCompose()
				ret = ret.thenCompose( v -> fun.apply( (RL) listener ).apply( event, param ) );
			}
		}
		return ret;
	}

	@Override
	public <R, U, RL> CompletionStage<R> fireLazyEventOnEachListener(
			Supplier<U> eventSupplier,
			Function<RL, Function<U, CompletionStage<R>>> fun) {
		CompletionStage<R> ret = nullCompletion();
		final T[] ls = listeners;
		if ( ls != null && ls.length != 0 ) {
			final U event = eventSupplier.get();
			for ( T listener : ls ) {
				//to preserve atomicity of the Session methods
				//call apply() from within the arg of thenCompose()
				ret = ret.thenCompose( v -> fun.apply( (RL) listener ).apply( event ) );
			}
		}
		return ret;
	}

	@Override
	public void addDuplicationStrategy(DuplicationStrategy strategy) {
		if ( duplicationStrategies == DEFAULT_DUPLICATION_STRATEGIES ) {
			// At minimum make sure we do not register the same exact listener class multiple times.
			duplicationStrategies = new LinkedHashSet<>( DEFAULT_DUPLICATION_STRATEGIES );
		}
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
		final T[] listenersRead = listeners;
		final T[] listenersWrite;

		if ( listenersRead == null ) {
			listenersWrite = createListenerArrayForWrite( 1 );
			listenersWrite[0] = listener;
		}
		else {
			final int size = listenersRead.length;

			listenersWrite = createListenerArrayForWrite( size + 1 );

			// first copy the existing listeners
			arraycopy( listenersRead, 0, listenersWrite, 0, size );

			// and then put the new one after them
			listenersWrite[size] = listener;
		}
		setListeners( listenersWrite );
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
		final T[] listenersRead = listeners;
		final T[] listenersWrite;

		if ( listenersRead == null ) {
			listenersWrite = createListenerArrayForWrite( 1 );
			listenersWrite[0] = listener;
		}
		else {
			final int size = listenersRead.length;

			listenersWrite = createListenerArrayForWrite( size + 1 );

			// put the new one first
			listenersWrite[0] = listener;

			// and copy the rest after it
			arraycopy( listenersRead, 0, listenersWrite, 1, size );
		}
		setListeners( listenersWrite );
	}

	private void handleListenerAddition(T listener, Consumer<T> additionHandler) {
		final T[] listenersRead = listeners;
		if ( listenersRead == null ) {
			additionHandler.accept( listener );
			return;
		}
		int size = listenersRead.length;

		final T[] listenersWrite = createListenerArrayForWrite( size );
		arraycopy( listenersRead, 0, listenersWrite, 0, size );

		final boolean traceEnabled = EVENT_LISTENER_LOGGER.isTraceEnabled();

		for ( DuplicationStrategy strategy : duplicationStrategies ) {

			// for each strategy, see if the strategy indicates that any of the existing
			//		listeners match the listener being added.  If so, we want to apply that
			//		strategy's action.  Control it returned immediately after applying the action
			//		on match - meaning no further strategies are checked...

			for ( int i = 0; i < size; i++ ) {
				final T existingListener = listenersRead[i];
				if ( traceEnabled ) {
					EVENT_LISTENER_LOGGER.tracef( "Checking incoming listener [`%s`] for match against existing listener [`%s`]",
							listener, existingListener );
				}

				if ( strategy.areMatch( listener,  existingListener ) ) {
					if ( traceEnabled ) {
						EVENT_LISTENER_LOGGER.tracef( "Found listener match between `%s` and `%s`",
								listener, existingListener );
					}

					final DuplicationStrategy.Action action = strategy.getAction();
					switch (action) {
						case ERROR:
							throw new EventListenerRegistrationException( "Duplicate event listener found" );
						case KEEP_ORIGINAL:
							if ( traceEnabled ) {
								EVENT_LISTENER_LOGGER.tracef( "Skipping listener registration (%s) : `%s`",
										action, listener );
							}
							return;
						case REPLACE_ORIGINAL:
							if ( traceEnabled ) {
								EVENT_LISTENER_LOGGER.tracef( "Replacing listener registration (%s) : `%s` -> `%s`",
										action, existingListener, listener );
							}
							prepareListener( listener );
							listenersWrite[i] = listener;
					}

					// we've found a match - we should return: the match action has already been applied at this point
					// apply all pending changes:
					setListeners( listenersWrite );
					return;
				}
			}
		}

		// we did not find any match, add it
		checkAgainstBaseInterface( listener );
		performInjections( listener );
		additionHandler.accept( listener );
	}

	@SuppressWarnings("unchecked")
	@AllowReflection // Possible array types are registered in org.hibernate.graalvm.internal.StaticClassLists.typesNeedingArrayCopy
	private T[] createListenerArrayForWrite(int len) {
		return (T[]) Array.newInstance( eventType.baseListenerInterface(), len );
	}

	private void prepareListener(T listener) {
		checkAgainstBaseInterface( listener );
		performInjections( listener );
	}

	private void performInjections(T listener) {
		if ( listener instanceof CallbackRegistryConsumer consumer ) {
			consumer.injectCallbackRegistry( callbackRegistry );
		}
		if ( listener instanceof JpaBootstrapSensitive sensitive ) {
			sensitive.wasJpaBootstrap( isJpaBootstrap );
		}
	}

	private void checkAgainstBaseInterface(T listener) {
		if ( !eventType.baseListenerInterface().isInstance( listener ) ) {
			throw new EventListenerRegistrationException( "Listener did not implement expected interface ["
					+ eventType.baseListenerInterface().getName() + "]" );
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
		return listenersAsList;
	}
}
