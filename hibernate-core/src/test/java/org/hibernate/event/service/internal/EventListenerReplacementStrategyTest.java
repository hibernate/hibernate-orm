package org.hibernate.event.service.internal;

import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.spi.ClearEvent;
import org.hibernate.event.spi.ClearEventListener;
import org.hibernate.event.spi.EventType;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test that a listener replacing the original one is actually called when the event is fired for each listener.
 * <p>
 *     Note: I'm using ClearEvent for the tests because it's the simpler one I've found.
 * </p>
 */
@TestForIssue(jiraKey = "HHH-13831")
public class EventListenerReplacementStrategyTest {

	@Test
	public void testListenersIterator() {
		Tracker tracker = new Tracker();
		ClearEvent event = new ClearEvent( null );
		EventListenerRegistryImpl listenerRegistry = null;
		EventListenerGroup<ClearEventListener> listenerGroup = new EventListenerGroupImpl( EventType.CLEAR, listenerRegistry );
		listenerGroup.addDuplicationStrategy( ReplaceOriginalStrategy.INSTANCE );
		listenerGroup.appendListener( new OriginalListener( tracker ) );
		listenerGroup.listeners().forEach( listener -> listener.onClear( event ) );

		assertThat( tracker.listenerClass ).as( "Unexpected listener called" ).isEqualTo( OriginalListener.class );
		assertThat( tracker.calls ).as( "It should have been called only once" ).hasValue( 1 );

		tracker = new Tracker(); // Reset tracker
		listenerGroup.appendListener( new ExpectedListener( tracker ) );
		listenerGroup.listeners().forEach( listener -> listener.onClear( event ) );

		assertThat( tracker.listenerClass ).as( "Unexpected listener called" ).isEqualTo( ExpectedListener.class );
		assertThat( tracker.calls ).as( "It should have been called only once" ).hasValue( 1 );
	}

	@Test
	public void testFireLazyEventOnEachListener() {
		Tracker tracker = new Tracker();
		ClearEvent event = new ClearEvent( null );
		EventListenerRegistryImpl listenerRegistry = null;
		EventListenerGroup<ClearEventListener> listenerGroup = new EventListenerGroupImpl( EventType.CLEAR, listenerRegistry );
		listenerGroup.addDuplicationStrategy( ReplaceOriginalStrategy.INSTANCE );
		listenerGroup.appendListener( new OriginalListener( tracker ) );
		listenerGroup.fireLazyEventOnEachListener( () -> event, ClearEventListener::onClear );

		assertThat( tracker.listenerClass ).as( "Unexpected listener called" ).isEqualTo( OriginalListener.class );
		assertThat( tracker.calls ).as( "It should have been called only once" ).hasValue( 1 );

		tracker = new Tracker(); // Reset tracker
		listenerGroup.appendListener( new ExpectedListener( tracker ) );
		listenerGroup.fireLazyEventOnEachListener( () -> event, ClearEventListener::onClear );

		assertThat( tracker.listenerClass ).as( "Unexpected listener called" ).isEqualTo( ExpectedListener.class );
		assertThat( tracker.calls ).as( "It should have been called only once" ).hasValue( 1 );
	}

	@Test
	public void testFireEventOnEachListener() {
		Tracker tracker = new Tracker();
		ClearEvent event = new ClearEvent( null );
		EventListenerRegistryImpl listenerRegistry = null;
		EventListenerGroup<ClearEventListener> listenerGroup = new EventListenerGroupImpl( EventType.CLEAR, listenerRegistry );
		listenerGroup.addDuplicationStrategy( ReplaceOriginalStrategy.INSTANCE );
		listenerGroup.appendListener( new OriginalListener( tracker ) );
		listenerGroup.fireEventOnEachListener( event, ClearEventListener::onClear );

		assertThat( tracker.listenerClass ).as( "Unexpected listener called" ).isEqualTo( OriginalListener.class );
		assertThat( tracker.calls ).as( "It should have been called only once" ).hasValue( 1 );

		tracker = new Tracker(); // Reset tracker
		listenerGroup.appendListener( new ExpectedListener( tracker ) );
		listenerGroup.fireEventOnEachListener( event, ClearEventListener::onClear );

		assertThat( tracker.listenerClass ).as( "Unexpected listener called" ).isEqualTo( ExpectedListener.class );
		assertThat( tracker.calls ).as( "It should have been called only once" ).hasValue( 1 );
	}

	/**
	 * Keep track of which listener is called and how many listeners are called.
	 */
	private class Tracker {
		private final AtomicInteger calls = new AtomicInteger( 0 );
		private Class<?> listenerClass = null;

		public void calledBy(Class<?> caller) {
			listenerClass = caller;
			calls.incrementAndGet();
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

	private static class ReplaceOriginalStrategy implements DuplicationStrategy {

		static final ReplaceOriginalStrategy INSTANCE = new ReplaceOriginalStrategy();

		@Override
		public boolean areMatch(Object listener, Object original) {
			return original instanceof ClearEventListener && listener instanceof ClearEventListener;
		}

		@Override
		public Action getAction() {
			return Action.REPLACE_ORIGINAL;
		}
	}
}
