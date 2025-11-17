/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.jfr.flush;

import java.util.List;

import org.hibernate.event.jfr.internal.PrePartialFlushEvent;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
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
		PrePartialFlushTest.TestEntity.class,
})
@SessionFactory
public class PrePartialFlushTest {
	public JfrEvents jfrEvents = new JfrEvents();

	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete from TestEntity " ).executeUpdate();
				}
		);
	}

	@Test
	@EnableEvent(PrePartialFlushEvent.NAME)
	public void testPrePartialFlushEvent(SessionFactoryScope scope) {
		jfrEvents.reset();
		String sessionId = scope.fromTransaction(
				session -> {
					TestEntity entity = new TestEntity( 1, "name_1" );
					session.persist( entity );

					session.createQuery( "select t from TestEntity t" ).list();
					return session.getSessionIdentifier().toString();
				}
		);

		List<RecordedEvent> events = jfrEvents.events()
				.filter(
						recordedEvent ->
						{
							String eventName = recordedEvent.getEventType().getName();
							return eventName.equals( PrePartialFlushEvent.NAME );
						}
				).toList();
		assertThat( events ).hasSize( 1 );

		RecordedEvent event = events.get( 0 );
		assertThat( event.getEventType().getName() )
				.isEqualTo( PrePartialFlushEvent.NAME );
		assertThat( event.getDuration() ).isPositive();
		assertThat( event.getString( "sessionIdentifier" ) )
				.isEqualTo( sessionId );
	}

	@Test
	@EnableEvent(PrePartialFlushEvent.NAME)
	public void testPrePartialFlushEventNotTriggered(SessionFactoryScope scope) {
		jfrEvents.reset();
		String sessionId = scope.fromTransaction(
				session -> {
					TestEntity entity = new TestEntity( 1, "name_1" );
					session.persist( entity );
					return session.getSessionIdentifier().toString();
				}
		);

		List<RecordedEvent> events = jfrEvents.events()
				.filter(
						recordedEvent ->
						{
							String eventName = recordedEvent.getEventType().getName();
							return eventName.equals( PrePartialFlushEvent.NAME );
						}
				).toList();
		assertThat( events ).hasSize( 0 );
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
