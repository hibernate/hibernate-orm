package org.hibernate.orm.test.locking;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.util.ExceptionUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Version;

import static org.hibernate.cfg.JdbcSettings.CONNECTION_HANDLING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Refresh must retain the state obtained by a pessimistic load even when an earlier
/// consistent read established a snapshot preceding a concurrent insert or update.
/// Run with both the configured isolation and explicit repeatable read to expose
/// differences between database implementations without prescribing the reload SQL.
/// PostgreSQL repeatable-read locking reads retain the snapshot: a later insert is
/// invisible, and locking a row updated since the snapshot raises a serialization failure.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		PessimisticRefreshSnapshotTest.SnapshotMarker.class,
		PessimisticRefreshSnapshotTest.RefreshTarget.class
})
@SessionFactory
@ServiceRegistry(settings = @Setting(name = CONNECTION_HANDLING, value = "DELAYED_ACQUISITION_AND_HOLD"))
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
public class PessimisticRefreshSnapshotTest {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new SnapshotMarker() ) );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	static Stream<Arguments> refreshModes() {
		return Stream.of( false, true ).flatMap( repeatableRead ->
				Stream.of( LockMode.PESSIMISTIC_READ, LockMode.PESSIMISTIC_WRITE ).flatMap( loadMode ->
						Stream.of( loadMode, LockMode.READ, LockMode.NONE ).map( refreshMode ->
								Arguments.of( repeatableRead, loadMode, refreshMode ) ) ) );
	}

	@ParameterizedTest(name = "repeatableRead={0}, load={1}, refresh={2}")
	@MethodSource("refreshModes")
	void refreshAfterConcurrentInsert(
			boolean repeatableRead, LockMode loadMode, LockMode refreshMode, SessionFactoryScope scope) {
		withSnapshot( scope, repeatableRead, session -> {
			// Transaction B commits after A's snapshot, before A obtains any target-row lock.
			scope.inTransaction( other -> other.persist( new RefreshTarget( "inserted" ) ) );
			if ( isPostgreSQLRepeatableRead( scope, session ) ) {
				assertNull( session.find( RefreshTarget.class, 1, loadMode ),
						"PostgreSQL locking reads must not see rows inserted after the repeatable-read snapshot" );
				return;
			}
			assertRefreshRetainsState( session, loadMode, refreshMode, "inserted", 0 );
		} );
	}

	@ParameterizedTest(name = "repeatableRead={0}, load={1}, refresh={2}")
	@MethodSource("refreshModes")
	void refreshAfterConcurrentUpdate(
			boolean repeatableRead, LockMode loadMode, LockMode refreshMode, SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new RefreshTarget( "original" ) ) );
		withSnapshot( scope, repeatableRead, session -> {
			scope.inTransaction( other -> {
				final var target = other.find( RefreshTarget.class, 1 );
				target.data = "updated";
			} );
			if ( isPostgreSQLRepeatableRead( scope, session ) ) {
				final var failure = assertThrows( PersistenceException.class,
						() -> session.find( RefreshTarget.class, 1, loadMode ),
						"PostgreSQL must reject locking a row updated since the repeatable-read snapshot" );
				final var sqlException = assertInstanceOf( SQLException.class,
						ExceptionUtil.rootCause( failure ) );
				assertEquals( "40001", sqlException.getSQLState(), "Expected a serialization failure" );
				return;
			}
			assertRefreshRetainsState( session, loadMode, refreshMode, "updated", 1 );
		} );
	}

	private static boolean isPostgreSQLRepeatableRead(SessionFactoryScope scope, Session session) {
		return scope.getSessionFactory().getJdbcServices().getDialect() instanceof PostgreSQLDialect
				&& session.doReturningWork( Connection::getTransactionIsolation ) == Connection.TRANSACTION_REPEATABLE_READ;
	}

	private static void withSnapshot(
			SessionFactoryScope scope, boolean repeatableRead, Consumer<Session> work) {
		scope.inSession( session -> session.doWork( connection -> {
			// Hold this connection until its original isolation has been restored, including on failure.
			final int originalIsolation = connection.getTransactionIsolation();
			if ( repeatableRead ) {
				assumeTrue(
						connection.getMetaData().supportsTransactionIsolationLevel( Connection.TRANSACTION_REPEATABLE_READ ),
						"JDBC driver does not support REPEATABLE_READ" );
			}
			try {
				if ( repeatableRead ) {
					connection.setTransactionIsolation( Connection.TRANSACTION_REPEATABLE_READ );
				}
				session.beginTransaction();
				// A separate table establishes the snapshot without locking the row B will change.
				assertEquals( 1, session.createQuery(
						"select m.id from SnapshotMarker m where m.id = 1", Integer.class ).getSingleResult() );
				work.accept( session );
				// A only reads. Roll it back in finally, also covering expected serialization failures.
			}
			finally {
				try {
					if ( session.getTransaction().isActive() ) {
						session.getTransaction().rollback();
					}
				}
				finally {
					connection.setTransactionIsolation( originalIsolation );
				}
			}
		} ) );
	}

	private static void assertRefreshRetainsState(
			Session session, LockMode loadMode, LockMode refreshMode, String expectedData, int expectedVersion) {
		final var target = session.find( RefreshTarget.class, 1, loadMode );
		assertNotNull( target, "Pessimistic load must see the concurrently committed row" );
		assertEquals( expectedData, target.data, "State after pessimistic load" );
		assertEquals( expectedVersion, target.version, "Version after pessimistic load" );
		assertEquals( loadMode, session.getCurrentLockMode( target ) );

		for ( int refresh = 1; refresh <= 2; refresh++ ) {
			if ( refreshMode == LockMode.NONE ) {
				session.refresh( target );
			}
			else {
				session.refresh( target, refreshMode );
			}
			assertEquals( expectedData, target.data, "State after refresh " + refresh );
			assertEquals( expectedVersion, target.version, "Version after refresh " + refresh );
			assertEquals( loadMode, session.getCurrentLockMode( target ), "Lock mode after refresh " + refresh );
		}
	}

	@Entity(name = "SnapshotMarker")
	public static class SnapshotMarker {
		@Id
		int id = 1;
	}

	@Entity(name = "RefreshTarget")
	public static class RefreshTarget {
		@Id
		int id = 1;

		String data;

		@Version
		int version;

		public RefreshTarget() {
		}

		RefreshTarget(String data) {
			this.data = data;
		}
	}
}
