package org.hibernate.orm.test.type.temporal.preservation;

import java.sql.Connection;
import java.time.*;
import java.util.TimeZone;
import org.hibernate.cfg.Environment;
import static org.assertj.core.api.Assertions.assertThat;

/// Named environment profiles keep JVM initialization, session state and JDBC
/// conversion settings independent. No DialectContext or boot connection is used.
///
/// @author Steve Ebersole
record PreservationEnvironment(String profile, String phase, String platform) {
	static PreservationEnvironment current() {
		final String dialect = Environment.getProperties().getProperty( "hibernate.dialect" );
		final String platform = dialect.contains( "PostgreSQL" )
				? "postgresql"
				: dialect.contains( "Oracle" )
						? "oracle"
						: dialect.contains( "H2" )
								? "h2"
								: "unavailable";
		org.junit.jupiter.api.Assumptions.assumeTrue( !platform.equals( "unavailable" ), "Live matrix supports H2/PostgreSQL/Oracle; SQL Server remains deferred" );
		return new PreservationEnvironment(
				System.getProperty( "hibernate.test.temporal.matrix_profile", "E0" ),
				System.getProperty( "hibernate.test.temporal.matrix_phase", "both" ),
				platform
		);
	}

	boolean pg() { return platform.equals( "postgresql" ); }
	boolean oracle() { return platform.equals( "oracle" ); }

	boolean full() { return profile.equals( "E0" ); }
	boolean cross() { return profile.equals( "E5" ) || profile.equals( "E6" ); }

	boolean writer() { return !phase.equals( "read" ); }
	boolean reader() { return !phase.equals( "write" ); }

	String writerJvm() { return switch ( profile ) { case "E0" -> "UTC"; case "E2", "E6" -> "Asia/Tokyo"; default -> "America/Denver"; }; }
	String readerJvm() { return profile.equals( "E5" ) ? "Asia/Tokyo" : profile.equals( "E6" ) ? "America/Denver" : writerJvm(); }
	String writerZone() { return profile.equals( "E2" ) || profile.equals( "E4" ) || profile.equals( "E6" ) ? "Asia/Tokyo" : "UTC"; }
	String readerZone() { return profile.equals( "E3" ) || profile.equals( "E5" ) ? "Asia/Tokyo" : profile.equals( "E2" ) ? "Asia/Tokyo" : "UTC"; }

	void assertJvm() {
		assertThat( TimeZone.getDefault().getID() ).isEqualTo( phase.equals( "read" ) ? readerJvm() : writerJvm() );
		assertThat( cross() ? phase.equals( "read" ) || phase.equals( "write" ) : phase.equals( "both" ) ).isTrue();
	}

	String identity(Connection connection) throws Exception {
		try ( var statement = connection.createStatement(); var result = statement.executeQuery(
				pg() ? "select pg_backend_pid()" : oracle() ? "select sys_context('USERENV','SID') from dual" : "select session_id()" ) ) {
			assertThat( result.next() ).isTrue(); return result.getString( 1 );
		}
	}

	void prepare(Connection connection, boolean writer) throws Exception {
		final String zone = writer ? writerZone() : readerZone();
		try ( var statement = connection.createStatement() ) {
			statement.execute( (oracle() ? "alter session set time_zone='" : "set time zone '") + zone + "'" );
			try ( var result = statement.executeQuery( pg() ? "select current_setting('TimeZone')"
					: oracle() ? "select current_timestamp from dual" : "select current_timestamp" ) ) {
				assertThat( result.next() ).isTrue();
				if ( pg() ) {
					assertThat( result.getString( 1 ) ).isEqualTo( zone );
				}
				else {
					assertThat( result.getObject( 1, OffsetDateTime.class ).getOffset() )
							.isEqualTo( ZoneId.of( zone ).getRules().getOffset( Instant.now() ) );
				}
			}
		}
	}
}
