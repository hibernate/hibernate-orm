package org.hibernate.orm.test.annotations;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				CreationUpdatedTimestampInEmbeddableTest.Event.class,
				CreationUpdatedTimestampInEmbeddableTest.History.class,
		}
)
@SessionFactory
class CreationUpdatedTimestampInEmbeddableTest {

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
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete Event" ).executeUpdate();
		} );
	}

	@Test
	void test(SessionFactoryScope scope) {
		LocalDateTime created = scope.fromTransaction( session -> {
			Event fruit = session.get( Event.class, 1L );
			return fruit.history.created;
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
	}
}
