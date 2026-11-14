package org.hibernate.orm.test.type.temporal.preservation;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.sql.Connection;
import java.time.*;
import java.util.TimeZone;
import org.hibernate.orm.test.mapping.javatime.redesign.TemporalLiveProfile;
import org.hibernate.cfg.Environment;
import static org.assertj.core.api.Assertions.assertThat;

/// Named environment profiles keep JVM initialization, session state and JDBC
/// conversion settings independent. A separate provisioning connection discovers
/// the version before any guarded runtime bootstrap; no DialectContext is used.
///
/// @author Steve Ebersole
record PreservationEnvironment(String profile, String phase, String platform, String version, String providerDialect) {
	static PreservationEnvironment current() {
		final String platform = TemporalLiveProfile.requireSupported().name().toLowerCase( Locale.ROOT );
		final var settings = new HashMap<String, Object>();
		Environment.getProperties().forEach( (key, value) -> settings.put( key.toString(), value ) );
		final var provider = new GuardedConnectionProvider( false );
		provider.configure( settings );
		final String version;
		try ( var connection = provider.getConnection() ) {
			final var metadata = connection.getMetaData();
			version = checkedVersion( (String) settings.get( "jakarta.persistence.database-product-version" ),
					metadata.getDatabaseMajorVersion(), metadata.getDatabaseMinorVersion() );
		}
		catch (SQLException failure) {
			throw new IllegalStateException( "Could not discover matrix provisioning version", failure );
		}
		return new PreservationEnvironment(
				System.getProperty( "hibernate.test.temporal.matrix_profile", "E0" ),
				System.getProperty( "hibernate.test.temporal.matrix_phase", "both" ),
				platform, version, (String) settings.get( "hibernate.dialect" )
		);
	}

	static String checkedVersion(String explicit, int major, int minor) {
		final String actual = major + "." + minor;
		if ( explicit != null && !explicit.equals( actual ) && !explicit.startsWith( actual + "." ) ) {
			throw new IllegalStateException( "Configured test database version " + explicit + " disagrees with discovered version " + actual );
		}
		return explicit == null ? actual : explicit;
	}

	// SQL-profile sharing must not erase EDB identity in range evidence. Child
	// processes inherit the concrete dialect along with their connection settings.
	boolean edb() { return "org.hibernate.dialect.PostgresPlusDialect".equals( providerDialect ); }

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
