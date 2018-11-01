/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.service.internal;

import org.hibernate.event.internal.DefaultMergeEventListener;
import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.MergeEventListener;
import org.hibernate.jpa.event.spi.CallbackRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Frank Doherty
 */
public class EventListenerGroupAppendListenerTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testAppendListenerWithNoStrategy() {
		DefaultMergeEventListener mergeEventListener = new DefaultMergeEventListener() {
		};
		runAppendListenerTest( null, mergeEventListener );
	}

    @Test
	@TestForIssue(jiraKey = "HHH-13070")
	public void testAppendListenerWithReplaceOriginalStrategy() {
		DefaultMergeEventListener mergeEventListener = new DefaultMergeEventListener() {
		};
		runAppendListenerTest( DUPLICATION_STRATEGY_REPLACE_ORIGINAL, mergeEventListener );
    }

	private static final DuplicationStrategy DUPLICATION_STRATEGY_REPLACE_ORIGINAL = new DuplicationStrategy() {

		@Override
		public boolean areMatch(
				Object added, Object existing) {
			return true;
		}

		@Override
		public DuplicationStrategy.Action getAction() {
			return Action.REPLACE_ORIGINAL;
		}
	};

	private static CallbackRegistry getCallbackRegistry(
			MergeEventListener listener)
			throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
		Class<?> clazz = Class.forName( "org.hibernate.event.internal.AbstractSaveEventListener" );
		Field field = clazz.getDeclaredField( "callbackRegistry" );
		field.setAccessible( true );
		return (CallbackRegistry) field.get( listener );
	}

	private void assertCallbackRegistry(
    		MergeEventListener listener) {
        try {
            assertNotNull( "callbackRegistry should not be null", getCallbackRegistry( listener ) );
        } catch ( ClassNotFoundException | NoSuchFieldException | IllegalAccessException e ) {
            fail("Unable to get callbackRegistry field on listener");
        }
    }

	private void runAppendListenerTest(
			DuplicationStrategy duplicationStrategy, DefaultMergeEventListener mergeEventListener) {
		doInHibernate( this::sessionFactory, session -> {
			ServiceRegistryImplementor serviceRegistry = sessionFactory().getServiceRegistry();
			EventListenerRegistry listenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );

			EventListenerGroup<MergeEventListener> group = listenerRegistry.getEventListenerGroup( EventType.MERGE );
			if (duplicationStrategy != null) {
				group.addDuplicationStrategy( duplicationStrategy );
			}
			group.appendListener( mergeEventListener );

			Iterable<MergeEventListener> listeners = group.listeners();
			assertTrue( "Should have at least one listener", listeners.iterator().hasNext() );
			listeners.forEach( this::assertCallbackRegistry);
		} );
	}
}
