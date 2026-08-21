/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.mixed;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@DomainModel(
		annotatedClasses = {
				MixedTimingEmbeddableGeneratorsTest.Event.class,
				MixedTimingEmbeddableGeneratorsTest.History.class,
		}
)
@SessionFactory
class MixedTimingEmbeddableGeneratorsTest {

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Event event = new Event();
			event.id = 1L;
			event.name = "conference";
			session.persist( event );
		} );

		LocalDateTime created = scope.fromTransaction( session -> {
			Event event = session.get( Event.class, 1L );
			return event.history.created;
		} );

		scope.inTransaction( session -> {
			Event event = session.get( Event.class, 1L );
			event.name = "concert";
		} );

		scope.inTransaction( session -> {
			Event event = session.get( Event.class, 1L );
			assertThat( event.history.created ).isEqualTo( created );
			assertThat( event.history.updated ).isNotEqualTo( created );
			assertThat( event.name ).isEqualTo( "concert" );
		} );
	}

	@Test
	void test2(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Event event = new Event();
			event.id = 1L;
			event.name = "conference";
			event.history = new History();
			event.history.notes = "inserted";
			session.persist( event );
		} );

		LocalDateTime created = scope.fromTransaction( session -> {
			Event event = session.get( Event.class, 1L );
			assertEquals( "inserted", event.history.notes );
			return event.history.created;
		} );

		scope.inTransaction( session -> {
			Event event = session.get( Event.class, 1L );
			event.name = "concert";
			event.history.notes = "updated";
		} );

		scope.inTransaction( session -> {
			Event event = session.get( Event.class, 1L );
			assertEquals( created, event.history.created );
			assertNotEquals( created, event.history.updated );
			assertEquals( "updated", event.history.notes );
			assertEquals( "concert", event.name );
		} );
	}


	@Entity(name = "Event")
	public static class Event {

		@Id
		public Long id;

		public String name;

		@Embedded
		public History history;

	}

	@Embeddable
	public static class History {
		@Column
		@CreationTimestamp
		public LocalDateTime created;

		@Column
		@UpdateTimestamp
		public LocalDateTime updated;

		public String notes;
	}
}
