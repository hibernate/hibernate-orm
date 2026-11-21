package org.hibernate.temporal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Temporal;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.FindMultipleOption.OrderingMode.ORDERED;
import static org.hibernate.KeyType.NATURAL;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = TemporalNaturalIdTest.Book.class)
@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = "org.hibernate.temporal.TemporalNaturalIdTest$ChangesetIds")
})
class TemporalNaturalIdTest {
	@Test
	void orderedLookupAtChangeset(SessionFactoryScope scope) {
		final var changeset = scope.fromTransaction( session -> {
			session.persist( new Book( 1, "A", "First book" ) );
			session.persist( new Book( 2, "B", "Original title" ) );
			return session.getCurrentChangesetIdentifier();
		} );
		scope.inTransaction( session -> session.find( Book.class, 2 ).title = "Revised title" );

		try (var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession()) {
			final var first = session.find( Book.class, 1 );
			final var books = session.findMultiple( Book.class, List.of( "B", "missing", "A" ), NATURAL, ORDERED );
			assertThat( books ).containsExactly( session.find( Book.class, 2 ), null, first );
			assertEquals( "Original title", books.get( 0 ).title );
		}
	}

	@Entity(name = "Book")
	@Temporal
	static class Book {
		@Id int id;
		@NaturalId String isbn;
		String title;

		Book() {
		}

		Book(int id, String isbn, String title) {
			this.id = id;
			this.isbn = isbn;
			this.title = title;
		}
	}

	public static class ChangesetIds implements ChangesetIdentifierSupplier<Integer> {
		private int changeset;

		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++changeset;
		}
	}
}
