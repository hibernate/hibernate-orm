/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.service.spi;

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.Incubating;
import org.hibernate.event.spi.EventType;

/**
 * Contract for a groups of events listeners for a particular event type.
 *
 * @author Steve Ebersole
 */
public interface EventListenerGroup<T> {

	/**
	 * Retrieve the event type associated with this groups of listeners.
	 *
	 * @return The event type.
	 */
	EventType<T> getEventType();

	/**
	 * Are there no listeners registered?
	 *
	 * @return {@literal true} if no listeners are registered; {@literal false} otherwise.
	 */
	boolean isEmpty();

	int count();

	/**
	 * @deprecated this is not the most efficient way for iterating the event listeners.
	 * See {@link #fireEventOnEachListener(Object, BiConsumer)} and its overloaded variants for better alternatives.
	 * @return The Iterable.
	 */
	@Deprecated
	Iterable<T> listeners();

	/**
	 * Mechanism to more finely control the notion of duplicates.
	 * <p>
	 * For example, say you are registering listeners for an extension library. This
	 * extension library could define a "marker interface" which indicates listeners
	 * related to it and register a strategy that checks against that marker interface.
	 *
	 * @param strategy The duplication strategy
	 */
	void addDuplicationStrategy(DuplicationStrategy strategy);

	/**
	 * Add a listener to the group.
	 */
	void appendListener(T listener);

	/**
	 * Add the given listeners to the group.
	 */
	@SuppressWarnings("unchecked") // heap pollution due to varargs
	void appendListeners(T... listeners);

	/**
	 * Add a listener to the group.
	 */
	void prependListener(T listener);

	/**
	 * Add the given listeners to the group.
	 */
	@SuppressWarnings("unchecked") // heap pollution due to varargs
	void prependListeners(T... listeners);

	/**
	 * Clears both the list of event listeners and every {@link DuplicationStrategy},
	 * including the default duplication strategy.
	 *
	 * @deprecated Use {@link #clearListeners()} instead, which doesn't also reset
	 *             the registered {@link DuplicationStrategy}s.
	 */
	@Deprecated
	void clear();

	/**
	 * Removes all registered listeners
	 */
	void clearListeners();

	/**
	 * Fires an event on each registered event listener of this group.
	 *
	 * @implNote The first argument is a supplier so that events can avoid being created
	 *           when no listener is registered; The second argument is specifically
	 *           designed to avoid needing a capturing lambda.
	 *
	 * @param <U> the kind of event
	 */
	@Incubating
	<U> void fireLazyEventOnEachListener(Supplier<U> eventSupplier, BiConsumer<T,U> actionOnEvent);

	/**
	 * Similar as {@link #fireLazyEventOnEachListener(Supplier, BiConsumer)} except it
	 * doesn't use a {{@link Supplier}}. Useful when there is no need to lazily initialize
	 * the event.
	 *
	 * @param <U> the kind of event
	 */
	@Incubating
	<U> void fireEventOnEachListener(U event, BiConsumer<T,U> actionOnEvent);

	/**
	 * Similar to {@link #fireEventOnEachListener(Object, BiConsumer)}, but allows passing
	 * a third parameter to the consumer; our code based occasionally needs a third parameter:
	 * having this additional variant allows using the optimal iteration more extensively and
	 * reduce allocations.
	 */
	@Incubating
	<U,X> void fireEventOnEachListener(U event, X param, EventActionWithParameter<T,U,X> actionOnEvent);

	/**
	 * Similar to {@link #fireEventOnEachListener(Object, BiConsumer)}, but Reactive friendly:
	 * it chains processing of the same event on each Reactive Listener, and returns a
	 * {@link CompletionStage} of type R. The various generic types allow using this for each
	 * concrete event type and flexible return types.
	 * <p>
	 * <em>Used by Hibernate Reactive</em>
	 *
	 * @param event The event being fired
	 * @param fun The function combining each event listener with the event
	 * @param <R> the return type of the returned CompletionStage
	 * @param <U> the type of the event being fired on each listener
	 * @param <RL> the type of ReactiveListener: each listener of type T will be cast to this type
	 * @return the composite completion stage of invoking fun(event) on each listener.
	 */
	@Incubating
	<R, U, RL> CompletionStage<R> fireEventOnEachListener(U event, Function<RL, Function<U, CompletionStage<R>>> fun);

	/**
	 * Similar to {@link #fireEventOnEachListener(Object, Object, EventActionWithParameter)},
	 * but Reactive friendly: it chains processing of the same event on each Reactive Listener,
	 * and returns a {@link CompletionStage} of type R. The various generic types allow using
	 * this for each concrete event type and flexible return types.
	 * <p>
	 * <em>Used by Hibernate Reactive</em>
	 *
	 * @param event The event being fired
	 * @param fun The function combining each event listener with the event
	 * @param <R> the return type of the returned CompletionStage
	 * @param <U> the type of the event being fired on each listener
	 * @param <RL> the type of ReactiveListener: each listener of type T will be cast to this type
	 * @param <X> an additional parameter to be passed to the function fun
	 * @return the composite completion stage of invoking fun(event) on each listener.
	 */
	@Incubating
	<R, U, RL, X> CompletionStage<R> fireEventOnEachListener(U event, X param, Function<RL, BiFunction<U, X, CompletionStage<R>>> fun);

	/**
	 * Similar to {@link #fireLazyEventOnEachListener(Supplier, BiConsumer)}, but Reactive
	 * friendly: it chains processing of the same event on each Reactive Listener, and returns
	 * a {@link CompletionStage} of type R. The various generic types allow using this for
	 * each concrete event type and flexible return types.
	 * <p>
	 * This variant expects a Supplier of the event, rather than the event directly; this is
	 * useful for the event types which are commonly configured with no listeners at all, so
	 * to allow skipping creating the event; use only for event types which are known to be
	 * expensive while the listeners are commonly empty.
	 * <p>
	 * <em>Used by Hibernate Reactive</em>
	 *
	 * @param eventSupplier A supplier able to produce the actual event
	 * @param fun The function combining each event listener with the event
	 * @param <R> the return type of the returned CompletionStage
	 * @param <U> the type of the event being fired on each listener
	 * @param <RL> the type of ReactiveListener: each listener of type T will be to this type
	 * @return the composite completion stage of invoking fun(event) on each listener.
	 */
	@Incubating
	<R, U, RL> CompletionStage<R> fireLazyEventOnEachListener(Supplier<U> eventSupplier, Function<RL, Function<U, CompletionStage<R>>> fun);

}
