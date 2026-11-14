package org.hibernate.orm.test.mapping.javatime.redesign;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.hibernate.Session;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import static org.assertj.core.api.Assertions.assertThat;

/// Controls and verifies the independent environment axes of the temporal
/// regression matrices. These settings exercise JDBC handling; none requests
/// application adjustment or changes an assertion's expected domain value.
///
/// - `hibernate.test.temporal.jvm_zone` asserts the JVM default chosen before driver
///   initialization by Gradle's `temporalTestJvmTimeZone` option.
/// - `hibernate.jdbc.time_zone` asserts the factory's existing JDBC conversion zone,
///   independently of the JVM and database session zones.
/// - `hibernate.test.temporal.session_zone` asserts the session zone established by
///   connection initialization (`temporalTestSessionTimeZone`).
/// - `hibernate.test.temporal.writer_zone` and `reader_zone` select a session zone
///   on the current transaction's actual connection, overriding its initialization
///   zone for asymmetric write/read regression runs.
///
/// Unspecified axes are left alone, allowing ordinary IDE runs to expose their
/// actual defaults. This helper never changes the JVM timezone in-process, resets
/// pools, changes server-global settings or changes timezone-rule versions.
/// PostgreSQL session state is queried directly because pgJDBC's UTC OffsetDateTime
/// output does not reveal the server session timezone. H2/Oracle checks establish
/// the effective current offset, not the identity of a region's complete rules.
///
/// @author Steve Ebersole
final class TemporalMatrixEnvironment {
	private TemporalMatrixEnvironment() {
	}

	/// Set and verify the requested writer/reader zone on the connection used by
	/// this transaction. Without a phase-specific zone, verify only configured axes.
	static void prepareSession(Session session, boolean writer) {
		final String zone = System.getProperty( writer
				? "hibernate.test.temporal.writer_zone" : "hibernate.test.temporal.reader_zone" );
		if ( zone == null ) {
			assertSessionTimeZone( session );
			return;
		}
		// Validate before rendering the test-controlled zone into session SQL.
		final String id = ZoneId.of( zone ).getId();
		if ( !id.matches( "[A-Za-z0-9_+:/-]+" ) ) {
			throw new IllegalArgumentException( "Unsupported test zone: " + id );
		}
		final var dialect = ((SessionFactoryImplementor) session.getSessionFactory()).getJdbcServices().getDialect();
		session.doWork( connection -> {
			try ( var statement = connection.createStatement() ) {
				statement.execute( (dialect instanceof OracleDialect ? "alter session set time_zone = '" : "set time zone '") + id + "'" );
			}
		} );
		assertSessionTimeZone( session, zone );
	}

	/// Verify connection initialization and any requested JVM/JDBC zones without
	/// normalizing the environment or deriving an application adjustment target.
	static void assertSessionTimeZone(Session session) {
		assertSessionTimeZone( session, System.getProperty( "hibernate.test.temporal.session_zone" ) );
	}

	private static void assertSessionTimeZone(Session session, String expected) {
		final String jvmZone = System.getProperty( "hibernate.test.temporal.jvm_zone" );
		if ( jvmZone != null ) {
			assertThat( java.util.TimeZone.getDefault().getID() ).isEqualTo( jvmZone );
		}
		final String jdbcZone = System.getProperty( "hibernate.jdbc.time_zone" );
		if ( jdbcZone != null ) {
			assertThat( ((SessionFactoryImplementor) session.getSessionFactory()).getSessionFactoryOptions()
					.getJdbcTimeZone().getID() ).isEqualTo( java.util.TimeZone.getTimeZone( jdbcZone ).getID() );
		}
		if ( expected == null ) {
			return;
		}
		final var dialect = ((SessionFactoryImplementor) session.getSessionFactory()).getJdbcServices().getDialect();
		final boolean oracle = dialect instanceof OracleDialect;
		if ( dialect instanceof org.hibernate.dialect.PostgreSQLDialect ) {
			// pgJDBC returns OffsetDateTime in UTC even when the session is not UTC.
			// Read the server setting instead of mistaking extraction output for session state.
			session.doWork( connection -> {
				try ( var statement = connection.createStatement();
						var result = statement.executeQuery( "select current_setting('TimeZone')" ) ) {
					assertThat( result.next() ).isTrue();
					assertThat( ZoneId.of( result.getString( 1 ) ).normalized() ).isEqualTo( ZoneId.of( expected ).normalized() );
				}
			} );
			return;
		}
		session.doWork( connection -> {
			try ( var statement = connection.createStatement();
					var result = statement.executeQuery( oracle ? "select current_timestamp from dual" : "select current_timestamp" ) ) {
				assertThat( result.next() ).isTrue();
				final var actual = result.getObject( 1, OffsetDateTime.class );
				assertThat( actual.getOffset() ).as( "effective JDBC session timezone requested as %s", expected )
						.isEqualTo( ZoneId.of( expected ).getRules().getOffset( actual.toInstant() ) );
			}
		} );
	}
}
