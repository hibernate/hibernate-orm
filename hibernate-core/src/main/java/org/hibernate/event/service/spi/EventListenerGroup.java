/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.service.spi;

import java.io.Serializable;
import java.util.Map;
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
public interface EventListenerGroup<T> extends Serializable {

	/**
	 * Retrieve the event type associated with this groups of listeners.
	 *
	 * @return The event type.
	 */
	public EventType<T> getEventType();

	/**
	 * Are there no listeners registered?
	 *
	 * @return {@literal true} if no listeners are registered; {@literal false} otherwise.
	 */
	public boolean isEmpty();

	public int count();

	/**
	 * @deprecated this is not the most efficient way for iterating the event listeners.
	 * See {@link #fireEventOnEachListener(Object, BiConsumer)} and its overloaded variants for better alternatives.
	 * @return The Iterable.
	 */
	@Deprecated
	public Iterable<T> listeners();

	/**
	 * Mechanism to more finely control the notion of duplicates.
	 * <p/>
	 * For example, say you are registering listeners for an extension library.  This extension library
	 * could define a "marker interface" which indicates listeners related to it and register a strategy
	 * that checks against that marker interface.
	 *
	 * @param strategy The duplication strategy
	 */
	public void addDuplicationStrategy(DuplicationStrategy strategy);

	public void appendListener(T listener);
	public void appendListeners(T... listeners);

	public void prependListener(T listener);
	public void prependListeners(T... listeners);

	/**
	 * Clears both the list of event listeners and all DuplicationStrategy,
	 * including the default duplication strategy.
	 * @deprecated likely want to use {@link #clearListeners()} instead, which doesn't
	 * also reset the registered DuplicationStrategy(ies).
	 */
	@Deprecated
	public void clear();

	/**
	 * Removes all registered listeners
	 */
	public void clearListeners();

	/**
	 * Fires an event on each registered event listener of this group.
	 *
	 * Implementation note (performance):
	 * the first argument is a supplier so that events can avoid being created when no listener is registered.
	 * the second argument is specifically designed to avoid needing a capturing lambda.
	 *
	 * @param eventSupplier
	 * @param actionOnEvent
	 * @param <U> the kind of event
	 */
	@Incubating
	<U> void fireLazyEventOnEachListener(final Supplier<U> eventSupplier, final BiConsumer<T,U> actionOnEvent);

	/**
	 * Similar as {@link #fireLazyEventOnEachListener(Supplier, BiConsumer)} except it doesn't use a {{@link Supplier}}:
	 * useful when there is no need to lazily initialize the event.
	 * @param event
	 * @param actionOnEvent
	 * @param <U> the kind of event
	 */
	@Incubating
	<U> void fireEventOnEachListener(final U event, final BiConsumer<T,U> actionOnEvent);

	/**
	 * Similar to {@link #fireEventOnEachListener(Object, BiConsumer)}, but allows passing a third parameter
	 * to the consumer; our code based occasionally needs a third parameter: having this additional variant
	 * allows using the optimal iteration more extensively and reduce allocations.
	 * @param event
	 * @param param
	 * @param actionOnEvent
	 * @param <U>
	 * @param <X>
	 */
	@Incubating
	<U,X> void fireEventOnEachListener(final U event, X param, final EventActionWithParameter<T,U,X> actionOnEvent);

	/**
	 * Similar to {@link #fireEventOnEachListener(Object, Function)}, but Reactive friendly: it chains
	 * processing of the same event on each Reactive Listener, and returns a {@link CompletionStage} of type R.
	 * The various generic types allow using this for each concrete event type and flexible return types.
	 * <p>Used by Hibernate Reactive</p>
	 * @param event The event being fired
	 * @param fun The function combining each event listener with the event
	 * @param <R> the return type of the returned CompletionStage
	 * @param <U> the type of the event being fired on each listener
	 * @param <RL> the type of ReactiveListener: each listener of type T will be casted to it.
	 * @return the composite completion stage of invoking fun(event) on each listener.
	 */
	@Incubating
	<R, U, RL> CompletionStage<R> fireEventOnEachListener(final U event, final Function<RL, Function<U, CompletionStage<R>>> fun);

	/**
	 * Similar to {@link #fireEventOnEachListener(Object, Object, Function)}, but Reactive friendly: it chains
	 * processing of the same event on each Reactive Listener, and returns a {@link CompletionStage} of type R.
	 * The various generic types allow using this for each concrete event type and flexible return types.
	 * <p>Used by Hibernate Reactive</p>
	 * @param event The event being fired
	 * @param fun The function combining each event listener with the event
	 * @param <R> the return type of the returned CompletionStage
	 * @param <U> the type of the event being fired on each listener
	 * @param <RL> the type of ReactiveListener: each listener of type T will be casted to it.
	 * @param <X> an additional parameter to be passed to the function fun
	 * @return the composite completion stage of invoking fun(event) on each listener.
	 */
	@Incubating
	public <R, U, RL, X> CompletionStage<R> fireEventOnEachListener(U event, X param, Function<RL, BiFunction<U, X, CompletionStage<R>>> fun);

	/**
	 * Similar to {@link #fireLazyEventOnEachListener(Supplier, BiConsumer)}, but Reactive friendly: it chains
	 * processing of the same event on each Reactive Listener, and returns a {@link CompletionStage} of type R.
	 * The various generic types allow using this for each concrete event type and flexible return types.
	 * <p>This variant expects a Supplier of the event, rather than the event directly; this is useful for the
	 * event types which are commonly configured with no listeners at all, so to allow skipping creating the
	 * event; use only for event types which are known to be expensive while the listeners are commonly empty.</p>
	 * <p>Used by Hibernate Reactive</p>
	 * @param eventSupplier A supplier able to produce the actual event
	 * @param fun The function combining each event listener with the event
	 * @param <R> the return type of the returned CompletionStage
	 * @param <U> the type of the event being fired on each listener
	 * @param <RL> the type of ReactiveListener: each listener of type T will be casted to it.
	 * @return the composite completion stage of invoking fun(event) on each listener.
	 */
	@Incubating
	<R, U, RL> CompletionStage<R> fireLazyEventOnEachListener(final Supplier<U> eventSupplier, final Function<RL, Function<U, CompletionStage<R>>> fun);

}
