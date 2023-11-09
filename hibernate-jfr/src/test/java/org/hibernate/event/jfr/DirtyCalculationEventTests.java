package org.hibernate.event.jfr;

import java.util.List;

import org.hibernate.engine.spi.Status;
import org.hibernate.event.jfr.internal.DirtyCalculationEvent;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
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
		DirtyCalculationEventTests.TestEntity.class,
})
@SessionFactory
public class DirtyCalculationEventTests {
	public JfrEvents jfrEvents = new JfrEvents();

	@BeforeAll
	public void setUp(SessionFactoryScope scope){
			scope.inTransaction(
					session ->{
						TestEntity entity = new TestEntity( 1, "name_1" );
						session.persist( entity );
					}
			);
		}

	@Test
	@EnableEvent(DirtyCalculationEvent.NAME)
	public void testFlushEvent(SessionFactoryScope scope) {
		jfrEvents.reset();
		String sessionId = scope.fromTransaction(
				session -> {
					TestEntity testEntity = session.load( TestEntity.class, 1 );
					testEntity.setName( "new name" );
					return session.getSessionIdentifier().toString();
				}
		);
		List<RecordedEvent> events = jfrEvents.events()
				.filter(
						recordedEvent ->
						{
							String eventName = recordedEvent.getEventType().getName();
							return eventName.equals( DirtyCalculationEvent.NAME );
						}
				).toList();
		assertThat( events ).hasSize( 1 );

		RecordedEvent event = events.get( 0 );
		assertThat( event.getEventType().getName() )
				.isEqualTo( DirtyCalculationEvent.NAME );
		assertThat( event.getLong( "executionTime" ) ).isGreaterThan( 0 );
		assertThat( event.getString( "sessionIdentifier" ) )
				.isEqualTo( sessionId );
		assertThat( event.getString( "entityName" ) )
				.isEqualTo( TestEntity.class.getName() );
		assertThat( event.getString( "entityStatus" ) )
				.isEqualTo( Status.MANAGED.name() );
		assertThat( event.getBoolean( "dirty" ) )
				.isTrue();
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
