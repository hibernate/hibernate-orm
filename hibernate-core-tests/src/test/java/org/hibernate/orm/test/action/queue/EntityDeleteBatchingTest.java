package org.hibernate.orm.test.action.queue;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreDelete;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.queue.spi.decompose.entity.PreDeleteHandling;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.testing.jdbc.CollectingStatementObserver.Statement;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Reproduces HHH-20994 using the scenario from PR #13694.
///
/// @author Steve Ebersole
/// @author Thorben Janssen
@JiraKey("HHH-20994")
@DomainModel(annotatedClasses = {
		EntityDeleteBatchingTest.Author.class,
		EntityDeleteBatchingTest.VersionedAuthor.class,
		EntityDeleteBatchingTest.MultiTableAuthor.class,
		EntityDeleteBatchingTest.SoftAuthor.class,
		EntityDeleteBatchingTest.CallbackMultiAuthor.class,
		EntityDeleteBatchingTest.CallbackSoftAuthor.class
})
@ServiceRegistry(settings = @Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "5"))
@SessionFactory(useCollectingStatementObserver = true)
public class EntityDeleteBatchingTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getEventListenerRegistry().setListeners(
				EventType.PRE_DELETE, new org.hibernate.event.spi.PreDeleteEventListener[0] );
		scope.dropData();
	}

	@Test
	public void testBatching(SessionFactoryScope scope) {
		final var observer = scope.getCollectingStatementObserver();
		observer.clear();
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 11; id++ ) {
				session.persist( new Author( id ) );
			}
		} );
		assertThat( observer.getStatements().stream()
				.filter( statement -> statement.sql().startsWith( "insert into Author" ) )
				.filter( statement -> statement.batchPosition() == 1 ) ).hasSize( 3 );
		scope.inTransaction( session -> {
			final var authors = session.createSelectionQuery( "from Author", Author.class ).list();
			assertThat( authors ).hasSize( 11 );
			observer.clear();
			authors.forEach( session::remove );
		} );
		final var deletes = observer.getStatements().stream()
				.filter( statement -> statement.sql().startsWith( "delete from Author" ) ).toList();
		assertThat( deletes ).hasSize( 11 );
		assertThat( deletes ).extracting( Statement::batchPosition )
				.containsExactly( 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1 );
	}

	@ParameterizedTest
	@ValueSource(strings = { "VersionedAuthor", "MultiTableAuthor", "SoftAuthor" })
	void testOtherDeleteShapes(String entityName, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 11; id++ ) {
				session.persist( switch ( entityName ) {
					case "VersionedAuthor" -> new VersionedAuthor( id );
					case "MultiTableAuthor" -> new MultiTableAuthor( id );
					default -> new SoftAuthor( id );
				} );
			}
		} );
		final var observer = scope.getCollectingStatementObserver();
		scope.inTransaction( session -> {
			final var authors = session.createSelectionQuery( "from " + entityName, Object.class ).list();
			observer.clear();
			authors.forEach( session::remove );
		} );
		final var statements = observer.getStatements();
		assertThat( statements ).hasSize( entityName.equals( "MultiTableAuthor" ) ? 22 : 11 );
		for ( String sql : statements.stream().map( Statement::sql ).distinct().toList() ) {
			assertThat( statements.stream().filter( statement -> statement.sql().equals( sql ) ) )
					.extracting( Statement::batchPosition )
					.containsExactly( 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1 );
		}
		scope.inTransaction( session -> assertThat(
				session.createSelectionQuery( "from " + entityName, Object.class ).list() ).isEmpty() );
	}

	@ParameterizedTest
	@ValueSource(strings = { "CallbackMultiAuthor", "CallbackSoftAuthor" })
	void testCallbackVisibility(String entityName, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 3; id++ ) {
				session.persist( entityName.equals( "CallbackMultiAuthor" )
						? new CallbackMultiAuthor( id ) : new CallbackSoftAuthor( id ) );
			}
		} );
		// Multi-table deletes execute secondary-table operations before primary-table operations.
		final List<Long> observedCounts = new ArrayList<>();
		final var falseLiteral = scope.getSessionFactory().getJdbcServices().getDialect()
				.getLiteralSupport().toBooleanValueString( false );
		scope.inTransaction( session -> {
			final var authors = session.createSelectionQuery( "from " + entityName, Object.class ).list();
			// Lifecycle callbacks cannot call session methods, so capture the JDBC connection first.
			final var connection = session.doReturningWork( jdbcConnection -> jdbcConnection );
			for ( Object author : authors ) {
				final Runnable check = () -> observedCounts.add( countRows( connection,
						entityName.equals( "CallbackMultiAuthor" ) ? "CallbackAuthorDetails"
								: "CallbackSoftAuthor where deleted=" + falseLiteral ) );
				if ( author instanceof CallbackMultiAuthor multi ) {
					multi.check = check;
				}
				else {
					((CallbackSoftAuthor) author).check = check;
				}
				session.remove( author );
			}
		} );
		assertThat( observedCounts ).containsExactly( 3L, 2L, 1L );
	}

	@ParameterizedTest
	@ValueSource(strings = { "Author", "MultiTableAuthor" })
	void testListenerVisibilityAndVeto(String entityName, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 3; id++ ) {
				session.persist( entityName.equals( "Author" ) ? new Author( id ) : new MultiTableAuthor( id ) );
			}
		} );
		// Multi-table deletes execute secondary-table operations before primary-table operations.
		final List<Long> observedCounts = new ArrayList<>();
		final List<Long> secondListenerIds = new ArrayList<>();
		scope.getSessionFactory().getEventListenerRegistry().appendListeners( EventType.PRE_DELETE,
				event -> {
					observedCounts.add( event.getSession().doReturningWork( connection -> countRows( connection,
							entityName.equals( "Author" ) ? "Author" : "AuthorDetails" ) ) );
					return event.getId().equals( 2L );
				},
				event -> {
					secondListenerIds.add( (Long) event.getId() );
					return false;
				} );
		scope.inTransaction( session -> session.createSelectionQuery( "from " + entityName + " order by id", Object.class )
				.list().forEach( session::remove ) );
		assertThat( observedCounts ).containsExactly( 3L, 2L, 2L );
		assertThat( secondListenerIds ).containsExactly( 1L, 2L, 3L );
		scope.inTransaction( session -> assertThat(
				session.createSelectionQuery( "select id from " + entityName, Long.class ).list() ).containsExactly( 2L ) );
	}

	@Test
	void testBatchFailurePreventsFollowingListener(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new VersionedAuthor( 1L ) );
			session.persist( new VersionedAuthor( 2L ) );
		} );
		final List<Long> invoked = new ArrayList<>();
		scope.getSessionFactory().getEventListenerRegistry().appendListeners( EventType.PRE_DELETE, event -> {
			invoked.add( (Long) event.getId() );
			return false;
		} );
		assertThatThrownBy( () -> scope.inTransaction( session -> {
			final var authors = session.createSelectionQuery(
					"from VersionedAuthor order by id", VersionedAuthor.class ).list();
			session.doWork( connection -> {
				try ( var statement = connection.createStatement() ) {
					statement.executeUpdate( "update VersionedAuthor set version=version+1 where id=1" );
				}
			} );
			authors.forEach( session::remove );
			session.flush();
		} ) ).isInstanceOf( jakarta.persistence.OptimisticLockException.class );
		assertThat( invoked ).containsExactly( 1L );
	}

	@Test
	void testSharedHandlingAndUnloadedInstance(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var eventSource = session.unwrap( EventSource.class );
			final var persister = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Author.class );
			final var handling = new PreDeleteHandling( new EntityDeleteAction(
					1L, new Object[0], null, new Author( 1L ), persister, false, eventSource ) );
			assertThat( handling.requiresBatchFlush() ).isFalse();
			final List<Long> invoked = new ArrayList<>();
			scope.getSessionFactory().getEventListenerRegistry().appendListeners( EventType.PRE_DELETE, event -> {
				invoked.add( (Long) event.getId() );
				return true;
			} );
			assertThat( handling.requiresBatchFlush() ).isTrue();
			assertThat( handling.beforeExecution( eventSource ) ).isFalse();
			assertThat( handling.requiresBatchFlush() ).isFalse();
			assertThat( handling.beforeExecution( eventSource ) ).isFalse();
			assertThat( handling.isVeto() ).isTrue();
			assertThat( invoked ).containsExactly( 1L );

			final var unloaded = new PreDeleteHandling( new EntityDeleteAction( 2L, persister, eventSource ) );
			assertThat( unloaded.requiresBatchFlush() ).isFalse();
			assertThat( unloaded.beforeExecution( eventSource ) ).isTrue();
			assertThat( invoked ).containsExactly( 1L );
		} );
	}

	private static long countRows(Connection connection, String table) {
		try ( var statement = connection.createStatement();
				var result = statement.executeQuery( "select count(*) from " + table ) ) {
			result.next();
			return result.getLong( 1 );
		}
		catch (SQLException e) {
			throw new RuntimeException( e );
		}
	}

	@Entity(name = "VersionedAuthor")
	public static class VersionedAuthor {
		@Id
		Long id;
		@Version
		int version;

		public VersionedAuthor() {
		}

		VersionedAuthor(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "MultiTableAuthor")
	@SecondaryTable(name = "AuthorDetails")
	public static class MultiTableAuthor {
		@Id
		Long id;
		@Column(table = "AuthorDetails")
		String details = "details";

		public MultiTableAuthor() {
		}

		MultiTableAuthor(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "SoftAuthor")
	@SoftDelete
	public static class SoftAuthor {
		@Id
		Long id;

		public SoftAuthor() {
		}

		SoftAuthor(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "CallbackMultiAuthor")
	@SecondaryTable(name = "CallbackAuthorDetails")
	public static class CallbackMultiAuthor {
		@Id
		Long id;
		@Column(table = "CallbackAuthorDetails")
		String details = "details";
		@Transient
		Runnable check;

		public CallbackMultiAuthor() {
		}

		CallbackMultiAuthor(Long id) {
			this.id = id;
		}

		@PreDelete
		void beforeDelete() {
			if ( check != null ) {
				check.run();
			}
		}
	}

	@Entity(name = "CallbackSoftAuthor")
	@SoftDelete
	public static class CallbackSoftAuthor {
		@Id
		Long id;
		@Transient
		Runnable check;

		public CallbackSoftAuthor() {
		}

		CallbackSoftAuthor(Long id) {
			this.id = id;
		}

		@PreDelete
		void beforeDelete() {
			if ( check != null ) {
				check.run();
			}
		}
	}

	@Entity(name = "Author")
	public static class Author {
		@Id
		private Long id;

		public Author() {
		}

		public Author(Long id) {
			this.id = id;
		}
	}
}
