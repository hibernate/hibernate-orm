package org.hibernate.orm.test.mapping.javatime.redesign;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.type.internal.PostgreSQLOffsetDateTimeJdbcType;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Executable evidence gate, not a PostgreSQL production precision contribution.
/// No temporal entity is bootstrapped: this probe must run while its candidate
/// production plan remains unsupported. Independent prepared values exercise the
/// actual descriptor, reused statements and asymmetric session zones. Raw casts
/// characterize reduction separately and are never used as the expected oracle.
///
/// @author Steve Ebersole
@RequiresDialect(value = PostgreSQLDialect.class, matchSubTypes = false)
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "true"))
@DomainModel
@SessionFactory
class PostgreSQLPrecisionEvidenceTests {
	@Test
	void preparedOffsetDateTimesAndRawCasts(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.doWork( connection -> {
			final var options = (SharedSessionContractImplementor) session;
			final var jdbc = PostgreSQLOffsetDateTimeJdbcType.INSTANCE;
			System.out.println( "POSTGRES_PRECISION server=" + connection.getMetaData().getDatabaseProductVersion()
					+ " driver=" + connection.getMetaData().getDriverVersion() + " JVM=" + ZoneId.systemDefault()
					+ " JDBC=" + options.getJdbcTimeZone() + " descriptor=" + jdbc.getClass().getName() );
			for ( int p : new int[] { 0, 3, 6 } ) {
				try ( var statement = connection.createStatement() ) {
					statement.execute( "create temporary table precision_evidence (v timestamp(" + p + ") with time zone)" );
				}
				try ( var insert = connection.prepareStatement( "insert into precision_evidence values (?)" );
						var statement = connection.createStatement() ) {
					for ( String zone : List.of( "UTC", "America/Los_Angeles", "Asia/Kathmandu" ) ) {
						for ( String text : List.of( "1969-12-31T23:59:59.999999500+05:45", "2026-12-31T23:59:59.999499999-08:00",
								"2026-12-31T23:59:59.999500000+05:45", "2026-12-31T23:59:59.999500001+05:45",
								"2026-03-08T06:59:59.123456789Z", "2026-11-01T05:59:59.999999499Z",
								"0001-01-01T00:00:00Z", "9999-12-31T23:59:59.999999Z" ) ) {
							statement.execute( "set time zone '" + zone + "'" );
							final var source = OffsetDateTime.parse( text );
							int quantum = 1;
							for ( int i = p; i < 9; i++ ) { quantum *= 10; }
							final var prepared = source.withNano( source.getNano() / quantum * quantum );
							try ( var raw = statement.executeQuery( "select cast('" + text + "' as timestamp(" + p + ") with time zone)::text" ) ) {
								raw.next();
								System.out.println( "POSTGRES_RAW p=" + p + " zone=" + zone + " input=" + text + " server=" + raw.getString( 1 ) );
							}
							jdbc.getBinder( OffsetDateTimeJavaType.INSTANCE ).bind( insert, prepared, 1, options );
							insert.executeUpdate();
							statement.execute( "set time zone 'Pacific/Auckland'" );
							try ( var rows = statement.executeQuery( "select v, v::text, current_setting('TimeZone') from precision_evidence" ) ) {
								assertThat( rows.next() ).isTrue();
								final var actual = jdbc.getExtractor( OffsetDateTimeJavaType.INSTANCE ).extract( rows, 1, options );
								assertThat( actual.equals( prepared.withOffsetSameInstant( ZoneOffset.UTC ) ) ).isTrue();
								assertThat( rows.getString( 3 ) ).isEqualTo( "Pacific/Auckland" );
								System.out.println( "POSTGRES_PREPARED p=" + p + " input=" + prepared + " read=" + actual + " server=" + rows.getString( 2 ) );
							}
							statement.executeUpdate( "delete from precision_evidence" );
						}
					}
					jdbc.getBinder( OffsetDateTimeJavaType.INSTANCE ).bind( insert, null, 1, options );
					insert.executeUpdate();
					try ( var rows = statement.executeQuery( "select v from precision_evidence" ) ) {
						assertThat( rows.next() ).isTrue();
						assertThat( jdbc.getExtractor( OffsetDateTimeJavaType.INSTANCE ).extract( rows, 1, options ) ).isNull();
					}
				}
				finally {
					try ( var statement = connection.createStatement() ) { statement.execute( "drop table precision_evidence" ); }
				}
			}
		} ) );
	}
}
