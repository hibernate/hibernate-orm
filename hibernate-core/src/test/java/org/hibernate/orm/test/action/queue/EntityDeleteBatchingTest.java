package org.hibernate.orm.test.action.queue;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.jdbc.CollectingStatementObserver;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that entity deletes get JDBC-batched like inserts.
 * <p>
 * With a batch size of 5, removing 11 entities in one flush requires 3 batches (5 + 5 + 1).
 *
 * @author Thorben Janssen
 */
@JiraKey("HHH-20994")
@DomainModel(annotatedClasses = EntityDeleteBatchingTest.Author.class)
@ServiceRegistry(settings = @Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "5"))
@SessionFactory(useCollectingStatementObserver = true)
public class EntityDeleteBatchingTest {

	private static final int ENTITY_COUNT = 11;
	private static final int EXPECTED_BATCHES = 3;

	@BeforeEach
	public void createAuthors(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 1; id <= ENTITY_COUNT; id++ ) {
				session.persist( new Author( id, "Author " + id ) );
			}
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testInsertsAreBatched(SessionFactoryScope scope) {
		scope.dropData();
		final CollectingStatementObserver observer = scope.getCollectingStatementObserver();
		observer.clear();

		scope.inTransaction( session -> {
			for ( long id = 1; id <= ENTITY_COUNT; id++ ) {
				session.persist( new Author( id, "Author " + id ) );
			}
		} );

		assertBatches( observer, "insert into Author" );
	}

	@Test
	public void testDeletesAreBatched(SessionFactoryScope scope) {
		final CollectingStatementObserver observer = scope.getCollectingStatementObserver();

		scope.inTransaction( session -> {
			final var authors = session.createSelectionQuery( "from Author", Author.class ).list();
			assertThat( authors ).hasSize( ENTITY_COUNT );
			observer.clear();

			for ( Author author : authors ) {
				session.remove( author );
			}
		} );

		assertBatches( observer, "delete from Author" );
	}

	private static void assertBatches(CollectingStatementObserver observer, String sqlPrefix) {
		final var statements = observer.getStatements().stream()
				.filter( statement -> statement.sql().startsWith( sqlPrefix ) )
				.toList();
		assertThat( statements ).hasSize( ENTITY_COUNT );
		// the first statement of every JDBC batch has batch position 1
		assertThat( statements.stream().filter( statement -> statement.batchPosition() == 1 ) )
				.as( "JDBC batches for '%s': %s", sqlPrefix, statements )
				.hasSize( EXPECTED_BATCHES );
	}

	@Entity(name = "Author")
	@Table(name = "Author")
	public static class Author {
		@Id
		private Long id;

		private String name;

		public Author() {
		}

		public Author(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
