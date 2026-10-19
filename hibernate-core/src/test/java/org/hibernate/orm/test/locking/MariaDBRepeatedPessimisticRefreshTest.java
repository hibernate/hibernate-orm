package org.hibernate.orm.test.locking;

import jakarta.persistence.LockModeType;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.exception.SnapshotIsolationException;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JiraKey("HHH-21010")
@RequiresDialect(value = MariaDBDialect.class, majorVersion = 11, minorVersion = 4, microVersion = 2,
		comment = "Requires the innodb_snapshot_isolation session variable")
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.ISOLATION, value = "4"),
		// Keep the same connection until its session variable has been restored, including after rollback.
		@Setting(name = AvailableSettings.CONNECTION_HANDLING, value = "DELAYED_ACQUISITION_AND_HOLD")
})
@DomainModel(annotatedClasses = MySQLRepeatedPessimisticRefreshTest.Item.class)
@SessionFactory
public class MariaDBRepeatedPessimisticRefreshTest {

	@AfterEach
	void dropData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest(name = "innodb_snapshot_isolation={0}")
	@ValueSource(booleans = { false, true })
	void repeatedPessimisticRefresh(boolean snapshotIsolation, SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new MySQLRepeatedPessimisticRefreshTest.Item( 1L, "initial" ) ) );

		scope.inSession( session -> {
			final boolean originalSnapshotIsolation = session.doReturningWork( connection -> {
				try (var statement = connection.createStatement();
					var result = statement.executeQuery( "select @@session.innodb_snapshot_isolation" )) {
					result.next();
					return result.getBoolean( 1 );
				}
			} );
			try {
				session.doWork( connection -> {
					try (var statement = connection.createStatement()) {
						statement.execute( "set session innodb_snapshot_isolation = " + snapshotIsolation );
					}
				} );
				session.beginTransaction();
				// Establish Transaction A's snapshot before Transaction B commits its update.
				final var item = session.find( MySQLRepeatedPessimisticRefreshTest.Item.class, 1L );
				assertEquals( "initial", item.description );
				scope.inTransaction( otherSession ->
						otherSession.find( MySQLRepeatedPessimisticRefreshTest.Item.class, 1L ).description = "updated" );

				if ( snapshotIsolation ) {
					// With snapshot isolation enabled, MariaDB rejects locking a row changed since the snapshot.
					final var exception = assertThrows( SnapshotIsolationException.class,
							() -> session.refresh( item, LockModeType.PESSIMISTIC_WRITE ) );
					assertEquals( 1020, exception.getErrorCode() );
				}
				else {
					// Without snapshot isolation, locking reads are current reads, just as on MySQL.
					session.refresh( item, LockModeType.PESSIMISTIC_WRITE );
					assertEquals( "updated", item.description );
					// Keeping the lock clause prevents the second refresh from restoring the old snapshot.
					session.refresh( item, LockModeType.PESSIMISTIC_WRITE );
					assertEquals( "updated", item.description );
					session.getTransaction().commit();
				}
			}
			finally {
				if ( session.getTransaction().isActive() ) {
					session.getTransaction().rollback();
				}
				// Restore the session setting before returning this connection to the pool.
				session.doWork( connection -> {
					try (var statement = connection.createStatement()) {
						statement.execute( "set session innodb_snapshot_isolation = " + originalSnapshotIsolation );
					}
				} );
			}
		} );
	}
}
