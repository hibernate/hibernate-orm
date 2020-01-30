package org.hibernate.event.service.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistrationException;
import org.hibernate.event.spi.ClearEvent;
import org.hibernate.event.spi.ClearEventListener;
import org.hibernate.event.spi.EventType;

import org.hibernate.testing.TestForIssue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test that a listener replacing the original one is actually called when the event is fired for each listener.
 * <p>
 *     Note: I'm using ClearEvent for the tests because it's the simpler one I've found.
 * </p>
 */
@TestForIssue(jiraKey = "HHH-13831")
public class EventListenerDuplicationStrategyTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	Tracker tracker = new Tracker();
	ClearEvent event = new ClearEvent( null );
	EventListenerGroup<ClearEventListener> listenerGroup = new EventListenerGroupImpl( EventType.CLEAR, null );

	@Test
	public void testListenersIterator() {
		listenerGroup.addDuplicationStrategy( ReplaceOriginalStrategy.INSTANCE );
		listenerGroup.appendListener( new OriginalListener( tracker ) );
		listenerGroup.listeners().forEach( listener -> listener.onClear( event ) );

		assertThat( tracker.callers ).containsExactly( OriginalListener.class );

		tracker.reset();
		listenerGroup.appendListener( new ExpectedListener( tracker ) );
		listenerGroup.listeners().forEach( listener -> listener.onClear( event ) );

		assertThat( tracker.callers ).containsExactly( ExpectedListener.class );
	}

	@Test
	public void testFireLazyEventOnEachListener() {
		listenerGroup.addDuplicationStrategy( ReplaceOriginalStrategy.INSTANCE );
		listenerGroup.appendListener( new OriginalListener( tracker ) );
		listenerGroup.fireLazyEventOnEachListener( () -> event, ClearEventListener::onClear );

		assertThat( tracker.callers ).containsExactly( OriginalListener.class );

		tracker.reset();
		listenerGroup.appendListener( new ExpectedListener( tracker ) );
		listenerGroup.fireLazyEventOnEachListener( () -> event, ClearEventListener::onClear );

		assertThat( tracker.callers ).containsExactly( ExpectedListener.class );
	}

	@Test
	public void testFireEventOnEachListener() {
		listenerGroup.addDuplicationStrategy( ReplaceOriginalStrategy.INSTANCE );
		listenerGroup.appendListener( new OriginalListener( tracker ) );
		listenerGroup.fireEventOnEachListener( event, ClearEventListener::onClear );

		assertThat( tracker.callers ).containsExactly( OriginalListener.class );

		tracker.reset();
		listenerGroup.appendListener( new ExpectedListener( tracker ) );
		listenerGroup.fireEventOnEachListener( event, ClearEventListener::onClear );

		assertThat( tracker.callers ).containsExactly( ExpectedListener.class );
	}

	@Test
	public void testListenersIteratorWithMultipleListenersAndNoStrategy() {
		listenerGroup.appendListener( new OriginalListener( tracker ) );
		listenerGroup.appendListener( new ExpectedListener( tracker ) );
		listenerGroup.appendListener( new ExtraListener( tracker ) );
		listenerGroup.listeners().forEach( listener -> listener.onClear( event ) );

		assertThat( tracker.callers ).containsExactly( OriginalListener.class, ExpectedListener.class, ExtraListener.class );
	}

	@Test
	public void testFireLazyEventOnEachListenerWithMultipleListenersAndNoStrategy() {
		listenerGroup.appendListener( new OriginalListener( tracker ) );
		listenerGroup.appendListener( new ExpectedListener( tracker ) );
		listenerGroup.appendListener( new ExtraListener( tracker ) );
		listenerGroup.fireLazyEventOnEachListener( () -> event, ClearEventListener::onClear );

		assertThat( tracker.callers ).containsExactly( OriginalListener.class, ExpectedListener.class, ExtraListener.class );
	}

	@Test
	public void testFireEventOnEachListenerWithMultipleListenersAndNoStrategy() {
		listenerGroup.appendListener( new OriginalListener( tracker ) );
		listenerGroup.appendListener( new ExpectedListener( tracker ) );
		listenerGroup.appendListener( new ExtraListener( tracker ) );
		listenerGroup.fireEventOnEachListener( event, ClearEventListener::onClear );

		assertThat( tracker.callers ).containsExactly( OriginalListener.class, ExpectedListener.class, ExtraListener.class );
	}

	@Test
	public void testListenersIteratorWithMultipleListenersAndReplacementStrategy() {
		listenerGroup.addDuplicationStrategy( ReplaceOriginalStrategy.INSTANCE );
		listenerGroup.appendListener( new OriginalListener( tracker ) );
		listenerGroup.appendListener( new ExpectedListener( tracker ) );
		listenerGroup.appendListener( new ExtraListener( tracker ) );
		listenerGroup.listeners().forEach( listener -> listener.onClear( event ) );

		assertThat( tracker.callers ).containsExactly( ExpectedListener.class, ExtraListener.class );
	}

	@Test
	public void testFireLazyEventOnEachListenerWithMultipleListenersAndReplacementStrategy() {
		listenerGroup.addDuplicationStrategy( ReplaceOriginalStrategy.INSTANCE );
		listenerGroup.appendListener( new OriginalListener( tracker ) );
		listenerGroup.appendListener( new ExpectedListener( tracker ) );
		listenerGroup.appendListener( new ExtraListener( tracker ) );
		listenerGroup.fireLazyEventOnEachListener( () -> event, ClearEventListener::onClear );

		assertThat( tracker.callers ).containsExactly( ExpectedListener.class, ExtraListener.class );
	}

	@Test
	public void testFireEventOnEachListenerWithMultipleListenersAndReplacementStrategy() {
		listenerGroup.addDuplicationStrategy( ReplaceOriginalStrategy.INSTANCE );
		listenerGroup.appendListener( new OriginalListener( tracker ) );
		listenerGroup.appendListener( new ExpectedListener( tracker ) );
		listenerGroup.appendListener( new ExtraListener( tracker ) );
		listenerGroup.fireEventOnEachListener( event, ClearEventListener::onClear );

		assertThat( tracker.callers ).containsExactly( ExpectedListener.class, ExtraListener.class );
	}

	@Test
	public void testListenersIteratorWithMultipleListenersAndKeepOriginalStrategy() {
		listenerGroup.addDuplicationStrategy( KeepOriginalStrategy.INSTANCE );
		listenerGroup.appendListener( new OriginalListener( tracker ) );
		listenerGroup.appendListener( new ExpectedListener( tracker ) );
		listenerGroup.appendListener( new ExtraListener( tracker ) );
		listenerGroup.listeners().forEach( listener -> listener.onClear( event ) );

		assertThat( tracker.callers ).containsExactly( OriginalListener.class, ExtraListener.class );
	}

	@Test
	public void testFireLazyEventOnEachListenerWithMultipleListenersAndKeepOriginalStrategy() {
		listenerGroup.addDuplicationStrategy( KeepOriginalStrategy.INSTANCE );
		listenerGroup.appendListener( new OriginalListener( tracker ) );
		listenerGroup.appendListener( new ExpectedListener( tracker ) );
		listenerGroup.appendListener( new ExtraListener( tracker ) );
		listenerGroup.fireLazyEventOnEachListener( () -> event, ClearEventListener::onClear );

		assertThat( tracker.callers ).containsExactly( OriginalListener.class, ExtraListener.class );
	}

	@Test
	public void testFireEventOnEachListenerWithMultipleListenersAndKeepOriginalStrategy() {
		listenerGroup.addDuplicationStrategy( KeepOriginalStrategy.INSTANCE );
		listenerGroup.appendListener( new OriginalListener( tracker ) );
		listenerGroup.appendListener( new ExpectedListener( tracker ) );
		listenerGroup.appendListener( new ExtraListener( tracker ) );
		listenerGroup.fireEventOnEachListener( event, ClearEventListener::onClear );

		assertThat( tracker.callers ).containsExactly( OriginalListener.class, ExtraListener.class );
	}

	@Test
	public void testErrorStrategyOnAppend() {
		thrown.expect( EventListenerRegistrationException.class );
		thrown.expectMessage( "Duplicate event listener found" );

		listenerGroup.addDuplicationStrategy( ErrorStrategy.INSTANCE );
		listenerGroup.appendListener( new OriginalListener( tracker ) );
		listenerGroup.appendListener( new ExpectedListener( tracker ) );
	}

	/**
	 * Keep track of which listener is called and how many listeners are called.
	 */
	private class Tracker {
		private List<Class<?>> callers = new ArrayList<>();

		public void calledBy(Class<?> caller) {
			callers.add( caller );
		}

		public void reset() {
			callers.clear();
		}
	}

	/**
	 * The initial listener for the test
	 */
	private static class OriginalListener implements ClearEventListener {
		private final Tracker tracker;

		public OriginalListener(Tracker tracker) {
			this.tracker = tracker;
		}

		@Override
		public void onClear(ClearEvent event) {
			tracker.calledBy( this.getClass() );
		}
	}

	/**
	 * The expected listener to be called if everything goes well
	 */
	private static class ExpectedListener implements ClearEventListener {
		private final Tracker tracker;

		public ExpectedListener(Tracker tracker) {
			this.tracker = tracker;
		}

		@Override
		public void onClear(ClearEvent event) {
			tracker.calledBy( this.getClass() );
		}
	}

	/**
	 * An additional listener to test the case of multiple listeners registered for the same event
	 */
	private static class ExtraListener implements ClearEventListener {
		private final Tracker tracker;

		public ExtraListener(Tracker tracker) {
			this.tracker = tracker;
		}

		@Override
		public void onClear(ClearEvent event) {
			tracker.calledBy( this.getClass() );
		}
	}

	private static class ReplaceOriginalStrategy implements DuplicationStrategy {

		static final ReplaceOriginalStrategy INSTANCE = new ReplaceOriginalStrategy();

		@Override
		public boolean areMatch(Object listener, Object original) {
			// We just want to replace the original listener with the extra so that we can test with multiple listeners
			return original instanceof OriginalListener && listener instanceof ExpectedListener;
		}

		@Override
		public Action getAction() {
			return Action.REPLACE_ORIGINAL;
		}
	}

	private static class KeepOriginalStrategy implements DuplicationStrategy {

		static final KeepOriginalStrategy INSTANCE = new KeepOriginalStrategy();

		@Override
		public boolean areMatch(Object listener, Object original) {
			// We just want this to work for original and expected listener
			return original instanceof OriginalListener && listener instanceof ExpectedListener;
		}

		@Override
		public Action getAction() {
			return Action.KEEP_ORIGINAL;
		}
	}

	private static class ErrorStrategy implements DuplicationStrategy {

		static final ErrorStrategy INSTANCE = new ErrorStrategy();

		@Override
		public boolean areMatch(Object listener, Object original) {
			// We just want this to work for original and expected listener
			return original instanceof OriginalListener && listener instanceof ExpectedListener;
		}

		@Override
		public Action getAction() {
			return Action.ERROR;
		}
	}
}
