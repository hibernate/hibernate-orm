/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event;

import org.hibernate.HibernateException;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.LoadEventListener;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Gail Badner
 */

@TestForIssue( jiraKey = "HHH-13890" )
public class CustomEventTypeTest {
	private final String EVENT_TYPE_NAME = "operation";
	private final String OTHER_EVENT_TYPE_NAME = "other-operation";

	@Test
	public void testAddCustomEventType() {
		final int numberOfEventTypesOriginal = EventType.values().size();

		try {
			EventType.resolveEventTypeByName( EVENT_TYPE_NAME );
			fail( "Should have thrown HibernateException" );
		}
		catch(HibernateException expected) {
		}

		final EventType<CustomListener> eventType = EventType.addCustomEventType( EVENT_TYPE_NAME, CustomListener.class );
		assertEquals( EVENT_TYPE_NAME, eventType.eventName() );
		assertEquals( CustomListener.class, eventType.baseListenerInterface() );
		assertEquals( numberOfEventTypesOriginal, eventType.ordinal() );
		assertTrue( EventType.values().contains( eventType ) );
		assertEquals( numberOfEventTypesOriginal + 1, EventType.values().size() );

		final EventType<OtherCustomListener> otherEventType = EventType.addCustomEventType( OTHER_EVENT_TYPE_NAME, OtherCustomListener.class );
		assertEquals( OTHER_EVENT_TYPE_NAME, otherEventType.eventName() );
		assertEquals( OtherCustomListener.class, otherEventType.baseListenerInterface() );
		assertEquals( numberOfEventTypesOriginal + 1, otherEventType.ordinal() );
		assertEquals( numberOfEventTypesOriginal + 2, EventType.values().size() );

		// Adding an event type with the same name and base listener as one that exists, should be OK.
		EventType.addCustomEventType( "load", LoadEventListener.class );

		// Adding an event type with the same name but different listener as one that exists, should fail.
		try {
			EventType.addCustomEventType( "load", CustomListener.class );
			fail( "Should have thrown HibernateException" );
		}
		catch (HibernateException expected) {
		}
	}

	public interface CustomListener {
	}

	public interface OtherCustomListener {
	}
}
