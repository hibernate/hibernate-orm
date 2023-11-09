package org.hibernate.event.jfr.flush;

import java.util.List;

import org.hibernate.event.jfr.internal.FlushEvent;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jdk.jfr.consumer.RecordedEvent;
import org.moditect.jfrunit.EnableEvent;
import org.moditect.jfrunit.JfrEventTest;
import org.moditect.jfrunit.JfrEvents;

import static org.assertj.core.api.Assertions.assertThat;

@JfrEventTest
@DomainModel(annotatedClasses = {
		ExplicitFlushTests.TestEntity.class,
})
@SessionFactory
public class ExplicitFlushTests {
	public JfrEvents jfrEvents = new JfrEvents();

	@Test
	@EnableEvent(FlushEvent.NAME)
	public void testFlushEvent(SessionFactoryScope scope) {
		jfrEvents.reset();
		scope.inTransaction(
				session -> {
					TestEntity entity = new TestEntity( 1, "name_1" );
					session.persist( entity );
					session.flush();
					List<RecordedEvent> events = jfrEvents.events()
							.filter(
									recordedEvent ->
									{
										String eventName = recordedEvent.getEventType().getName();
										return eventName.equals( FlushEvent.NAME );
									}
							).toList();
					assertThat( events ).hasSize( 1 );

					RecordedEvent event = events.get( 0 );
					assertThat( event.getEventType().getName() )
							.isEqualTo( FlushEvent.NAME );
					assertThat( event.getLong( "executionTime" ) ).isGreaterThan( 0 );
					assertThat( event.getString( "sessionIdentifier" ) )
							.isEqualTo( session.getSessionIdentifier().toString() );
					assertThat( event.getInt( "numberOfEntitiesProcessed" ) )
							.isEqualTo( 1 );
					assertThat( event.getInt( "numberOfCollectionsProcessed" ) ).isEqualTo( 0 );
					assertThat( event.getBoolean( "isAutoFlush" ) ).isFalse();
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Integer id;

		private String name;

		public TestEntity() {
		}

		public TestEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
