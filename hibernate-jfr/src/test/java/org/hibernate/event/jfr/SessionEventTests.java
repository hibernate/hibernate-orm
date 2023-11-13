/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.event.jfr;

import java.util.List;

import org.hibernate.event.jfr.internal.SessionClosedEvent;
import org.hibernate.event.jfr.internal.SessionOpenEvent;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jdk.jfr.consumer.RecordedEvent;
import org.moditect.jfrunit.EnableEvent;
import org.moditect.jfrunit.JfrEventTest;
import org.moditect.jfrunit.JfrEvents;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Steve Ebersole
 */
@JfrEventTest
@DomainModel
@SessionFactory
public class SessionEventTests {
	public JfrEvents jfrEvents = new JfrEvents();

	@Test
	@EnableEvent(SessionOpenEvent.NAME)
	@EnableEvent(SessionClosedEvent.NAME)
	public void testSessionOpenEvent(SessionFactoryScope scope) {
		jfrEvents.reset();
		final String openedSessionIdentifier = scope.fromSession( (session) -> {
			final List<RecordedEvent> events = jfrEvents.events().filter( recordedEvent -> {
				String eventName = recordedEvent.getEventType().getName();
				return eventName.equals( SessionOpenEvent.NAME );
			} ).toList();
			assertThat( events ).hasSize( 1 );
			final RecordedEvent event = events.get( 0 );
			assertThat( event.getEventType().getName() ).isEqualTo( SessionOpenEvent.NAME );
			assertThat( event.getString( "sessionIdentifier" ) ).isEqualTo( session.getSessionIdentifier().toString() );

			jfrEvents.reset();

			return event.getString( "sessionIdentifier" );
		} );

		final List<RecordedEvent> events = jfrEvents.events().filter( recordedEvent -> {
			String eventName = recordedEvent.getEventType().getName();
			return eventName.equals( SessionClosedEvent.NAME );
		} ).toList();
		assertThat( events ).hasSize( 1 );
		final RecordedEvent event = events.get( 0 );
		assertThat( event.getEventType().getName() ).isEqualTo( SessionClosedEvent.NAME );
		assertThat( event.getString( "sessionIdentifier" ) ).isEqualTo( openedSessionIdentifier );
	}
}
