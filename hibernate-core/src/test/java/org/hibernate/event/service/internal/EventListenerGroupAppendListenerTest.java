/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.service.internal;

import java.lang.reflect.Field;

import org.hibernate.event.internal.DefaultMergeEventListener;
import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.MergeEventListener;
import org.hibernate.jpa.event.spi.CallbackRegistry;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Frank Doherty
 */
@JiraKey(value = "HHH-13070")
public class EventListenerGroupAppendListenerTest extends BaseSessionFactoryFunctionalTest {

	private static final DuplicationStrategy DUPLICATION_STRATEGY_REPLACE_ORIGINAL = new DuplicationStrategy() {

		@Override
		public boolean areMatch(
				Object added, Object existing) {
			return true;
		}

		@Override
		public Action getAction() {
			return Action.REPLACE_ORIGINAL;
		}
	};

	@Test
	public void testAppendListenerWithNoStrategy() {
		SpecificMergeEventListener1 mergeEventListener = new SpecificMergeEventListener1();

		runAppendListenerTest( null, mergeEventListener );
	}

	@Test
	public void testAppendListenerWithReplaceOriginalStrategy() {
		SpecificMergeEventListener2 mergeEventListener = new SpecificMergeEventListener2();

		runAppendListenerTest( DUPLICATION_STRATEGY_REPLACE_ORIGINAL, mergeEventListener );
	}

	private void runAppendListenerTest(
			DuplicationStrategy duplicationStrategy,
			DefaultMergeEventListener mergeEventListener) {
		inTransaction( session -> {

            EventListenerGroup<MergeEventListener> group =
					sessionFactory().getServiceRegistry()
							.requireService( EventListenerRegistry.class )
							.getEventListenerGroup( EventType.MERGE );
			if ( duplicationStrategy != null ) {
				group.addDuplicationStrategy( duplicationStrategy );
			}
			group.appendListener( mergeEventListener );

			Iterable<MergeEventListener> listeners = group.listeners();
			assertTrue( listeners.iterator().hasNext(), "Should have at least one listener" );
			listeners.forEach( this::assertCallbackRegistry );
		} );
	}

	private void assertCallbackRegistry(
			MergeEventListener listener) {
		try {
			assertNotNull( getCallbackRegistry( listener ), "callbackRegistry should not be null" );
		}
		catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
			fail( "Unable to get callbackRegistry field on listener" );
		}
	}

	private static CallbackRegistry getCallbackRegistry(
			MergeEventListener listener)
			throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
		Class<?> clazz = Class.forName( "org.hibernate.event.internal.AbstractSaveEventListener" );
		Field field = clazz.getDeclaredField( "callbackRegistry" );
		field.setAccessible( true );
		return (CallbackRegistry) field.get( listener );
	}

	private static class SpecificMergeEventListener1 extends DefaultMergeEventListener {

		// we need a specific class, otherwise the default duplication strategy avoiding listeners from the same classes
		// will be triggered.
	}

	private static class SpecificMergeEventListener2 extends DefaultMergeEventListener {

		// we need a specific class, otherwise the default duplication strategy avoiding listeners from the same classes
		// will be triggered.
	}
}
