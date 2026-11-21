package org.hibernate.temporal;

import org.hibernate.LockMode;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.VersionMatchMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Annotation-mapped variant of {@link AbstractTemporalEntityHistoryTest}.
 */
@SessionFactory
@DomainModel(annotatedClasses =
		{AbstractTemporalEntityHistoryTest.TemporalEntity4.class,
		AbstractTemporalEntityHistoryTest.TemporalChild4.class})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"))
@SkipForDialect(dialectClass = MariaDBDialect.class, majorVersion = 10, minorVersion = 11, versionMatchMode = VersionMatchMode.OLDER, reason = "See https://jira.mariadb.org/browse/MDEV-39230")
class TemporalEntityHistoryTest extends AbstractTemporalEntityHistoryTest {

	@Test
	void testPessimisticForceIncrementExecutesImmediately(SessionFactoryScope scope) {
		try {
			scope.inTransaction( session -> {
				final var entity = new TemporalEntity4();
				entity.id = 20417L;
				entity.text = "force increment";
				session.persist( entity );
			} );
			scope.inSession( session -> {
				final var transaction = session.beginTransaction();
				try {
					session.setJdbcBatchSize( 10 );
					final var entity = session.find( TemporalEntity4.class, 20417L );
					session.lock( entity, LockMode.PESSIMISTIC_FORCE_INCREMENT );
					assertEquals( 1, entity.version );
					final int databaseVersion = session.doReturningWork( connection -> {
						try ( var statement = connection.prepareStatement(
								"select version from TemporalEntity4 where id = ?" ) ) {
							statement.setLong( 1, entity.id );
							try ( var resultSet = statement.executeQuery() ) {
								assertTrue( resultSet.next() );
								return resultSet.getInt( 1 );
							}
						}
					} );
					assertEquals( 1, databaseVersion );
				}
				finally {
					// This test observes immediate execution, not committed history versioning.
					transaction.rollback();
				}
			} );
			scope.inTransaction( session ->
					assertEquals( 0, session.find( TemporalEntity4.class, 20417L ).version ) );
		}
		finally {
			scope.inTransaction( session -> {
				final var entity = session.find( TemporalEntity4.class, 20417L );
				if ( entity != null ) {
					session.remove( entity );
				}
			} );
		}
	}
}
