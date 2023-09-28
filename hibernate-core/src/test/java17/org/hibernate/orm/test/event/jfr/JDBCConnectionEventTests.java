package org.hibernate;

import java.util.List;

import org.hibernate.event.jfr.JDBCConnectionAcquisitionEvent;
import org.hibernate.event.jfr.JDBCConnectionReleaseEvent;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jdk.jfr.consumer.RecordedEvent;
import org.moditect.jfrunit.EnableEvent;
import org.moditect.jfrunit.JfrEventTest;
import org.moditect.jfrunit.JfrEvents;

import static org.assertj.core.api.Assertions.assertThat;

@JfrEventTest
@DomainModel
@SessionFactory
public class JDBCConnectionEventTests {
	public JfrEvents jfrEvents = new JfrEvents();

	@Test
	@EnableEvent(JDBCConnectionAcquisitionEvent.NAME)
	@EnableEvent(JDBCConnectionReleaseEvent.NAME)
	public void testJDBCConnectionAcquisition(SessionFactoryScope scope) {
		// starting a transaction should trigger the acquisition of the connection
		scope.inTransaction(
				session -> {
					final List<RecordedEvent> events = jfrEvents.events()
							.filter(
									recordedEvent ->
									{
										String eventName = recordedEvent.getEventType().getName();
										return eventName.equals( JDBCConnectionAcquisitionEvent.NAME )
												|| eventName.equals( JDBCConnectionReleaseEvent.NAME );
									}
							).toList();
					assertThat( events ).hasSize( 1 );
					assertThat( events.get( 0 )
										.getEventType()
										.getName() ).isEqualTo( JDBCConnectionAcquisitionEvent.NAME );
					jfrEvents.reset();
				}
		);
		final List<RecordedEvent> events = jfrEvents.events()
				.filter(
						recordedEvent ->
						{
							String eventName = recordedEvent.getEventType().getName();
							return eventName.equals( JDBCConnectionAcquisitionEvent.NAME )
									|| eventName.equals( JDBCConnectionReleaseEvent.NAME );
						}
				).toList();
		assertThat( events ).hasSize( 1 );
		assertThat( events.get( 0 ).getEventType().getName() ).isEqualTo( JDBCConnectionReleaseEvent.NAME );
		jfrEvents.reset();
	}

	@Test
	@EnableEvent(JDBCConnectionAcquisitionEvent.NAME)
	@EnableEvent(JDBCConnectionReleaseEvent.NAME)
	public void testJDBCConnectionAcquisitionNoFired(SessionFactoryScope scope) {
		// starting a session should not trigger the acquisition/release of the connection
		scope.inSession(
				session -> {
					final List<RecordedEvent> events = jfrEvents.events()
							.filter(
									recordedEvent ->
									{
										String eventName = recordedEvent.getEventType().getName();
										return eventName.equals( JDBCConnectionAcquisitionEvent.NAME )
												|| eventName.equals( JDBCConnectionReleaseEvent.NAME );
									}
							).toList();
					assertThat( events ).hasSize( 0 );
					jfrEvents.reset();
				}
		);
		final List<RecordedEvent> events = jfrEvents.events()
				.filter(
						recordedEvent ->
						{
							String eventName = recordedEvent.getEventType().getName();
							return eventName.equals( JDBCConnectionAcquisitionEvent.NAME )
									|| eventName.equals( JDBCConnectionReleaseEvent.NAME );
						}
				).toList();
		assertThat( events ).hasSize( 0 );
	}

}
