/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;

import java.time.LocalDateTime;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				CreationUpdatedTimestampInEmbeddableDbTest.Event.class,
				CreationUpdatedTimestampInEmbeddableDbTest.History.class,
		}
)
@SessionFactory
class CreationUpdatedTimestampInEmbeddableDbTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Event event = new Event();
			event.id = 1L;
			event.name = "conference";
			session.persist( event );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void test(SessionFactoryScope scope) {
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
			assertThat( event.history.updated ).isNotNull();
			assertThat( event.name ).isEqualTo( "concert" );
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
		@CreationTimestamp(source = SourceType.DB)
		public LocalDateTime created;

		@UpdateTimestamp(source = SourceType.DB)
		public LocalDateTime updated;
	}
}
